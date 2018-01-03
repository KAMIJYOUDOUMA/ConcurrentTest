package priv.cais.threadPool;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPoolExecutor extends AbstractExecutorService {
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits
    private static final int RUNNING = -1 << COUNT_BITS;
    private static final int SHUTDOWN = 0 << COUNT_BITS;
    private static final int STOP = 1 << COUNT_BITS;
    private static final int TIDYING = 2 << COUNT_BITS;
    private static final int TERMINATED = 3 << COUNT_BITS;

    // Packing and unpacking ctl
    private static int runStateOf(int c) {
        return c & ~CAPACITY;
    }

    private static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    /*
     * Bit field accessors that don't require unpacking ctl. These depend on the
     * bit layout and on workerCount being never negative.
     */
    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    /**
     * Attempts to CAS-increment the workerCount field of ctl.
     */
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    /**
     * Attempts to CAS-decrement the workerCount field of ctl.
     */
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    /**
     * Decrements the workerCount field of ctl. This is called only on abrupt
     * termination of a thread (see processWorkerExit). Other decrements are
     * performed within getTask.
     */
    private void decrementWorkerCount() {
        do {
        } while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    private final BlockingQueue<Runnable> workQueue;

    private final ReentrantLock mainLock = new ReentrantLock();

    private final HashSet<Worker> workers = new HashSet<Worker>();

    private final Condition termination = mainLock.newCondition();

    private int largestPoolSize;

    private long completedTaskCount;

    private volatile ThreadFactory threadFactory;

    private volatile RejectedExecutionHandler handler;

    private volatile long keepAliveTime;

    private volatile boolean allowCoreThreadTimeOut;

    private volatile int corePoolSize;

    private volatile int maximumPoolSize;

    private static final RejectedExecutionHandler defaultHandler = new AbortPolicy();

    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {

        private static final long serialVersionUID = 6138294804551838833L;

        final Thread thread;

        Runnable firstTask;

        volatile long completedTasks;

        Worker(Runnable firstTask) {
            setState(-1); // inhibit interrupts until runWorker
            this.firstTask = firstTask;
            this.thread = getThreadFactory().newThread(this);
        }

        /** Delegates main run loop to outer runWorker */
        public void run() {
            runWorker(this);
        }

        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock() {
            acquire(1);
        }

        public boolean tryLock() {
            return tryAcquire(1);
        }

        public void unlock() {
            release(1);
        }

        public boolean isLocked() {
            return isHeldExclusively();
        }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }

    private void advanceRunState(int targetState) {
        for (;;) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) || ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    final void tryTerminate() {
        for (;;) {
            int c = ctl.get();
            if (isRunning(c) || runStateAtLeast(c, TIDYING) || (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty()))
                return;
            if (workerCountOf(c) != 0) { // Eligible to terminate
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // else retry on failed CAS
        }
    }

    /*
     * Methods for controlling interrupts to worker threads.
     */

    /**
     * If there is a security manager, makes sure caller has permission to shut
     * down threads in general (see shutdownPerm). If this passes, additionally
     * makes sure the caller is allowed to interrupt each worker thread. This
     * might not be true even if first check passed, if the SecurityManager
     * treats some threads specially.
     */
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions (in
     * which case some threads may remain uninterrupted).
     */
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }

    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers) {
                Thread t = w.thread;
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne)
                    break;
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Common form of interruptIdleWorkers, to avoid having to remember what the
     * boolean argument means.
     */
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;

    final void reject(Runnable command) {
        // handler.rejectedExecution(command, this);
    }

    void onShutdown() {
    }

    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r))
                    taskList.add(r);
            }
        }
        return taskList;
    }

    private boolean addWorker(Runnable firstTask, boolean core) {
        retry: for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty()))
                return false;

            for (;;) {
                int wc = workerCountOf(c);
                if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize))
                    return false;
                if (compareAndIncrementWorkerCount(c))
                    break retry;
                c = ctl.get(); // Re-read ctl
                if (runStateOf(c) != rs)
                    continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            w = new Worker(firstTask);
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.
                    int rs = runStateOf(ctl.get());

                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) // precheck that t is startable
                            throw new IllegalThreadStateException();
                        workers.add(w);
                        int s = workers.size();
                        if (s > largestPoolSize)
                            largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (!workerStarted)
                addWorkerFailed(w);
        }
        return workerStarted;
    }

    /**
     * Rolls back the worker thread creation. - removes worker from workers, if
     * present - decrements worker count - rechecks for termination, in case the
     * existence of this worker was holding up termination
     */
    private void addWorkerFailed(Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null)
                workers.remove(w);
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted
            decrementWorkerCount();

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        tryTerminate();

        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && !workQueue.isEmpty())
                    min = 1;
                if (workerCountOf(c) >= min)
                    return; // replacement not needed
            }
            addWorker(null, false);
        }
    }

    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out?

        for (;;) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            // Are workers subject to culling?
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            if ((wc > maximumPoolSize || (timed && timedOut)) && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c))
                    return null;
                continue;
            }

            try {
                Runnable r = timed ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
                if (r != null)
                    return r;
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }

    final void runWorker(Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // allow interrupts
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted. This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt
                if ((runStateAtLeast(ctl.get(), STOP) || (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP)))
                        && !wt.isInterrupted())
                    wt.interrupt();
                try {
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x;
                        throw x;
                    } catch (Error x) {
                        thrown = x;
                        throw x;
                    } catch (Throwable x) {
                        thrown = x;
                        throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(w, completedAbruptly);
        }
    }

    // Public constructors and methods

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory and rejected execution handler. It
     * may be more convenient to use one of the {@link Executors} factory
     * methods instead of this general purpose constructor.
     *
     * @param corePoolSize
     *            the number of threads to keep in the pool, even if they are
     *            idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize
     *            the maximum number of threads to allow in the pool
     * @param keepAliveTime
     *            when the number of threads is greater than the core, this is
     *            the maximum time that excess idle threads will wait for new
     *            tasks before terminating.
     * @param unit
     *            the time unit for the {@code keepAliveTime} argument
     * @param workQueue
     *            the queue to use for holding tasks before they are executed.
     *            This queue will hold only the {@code Runnable} tasks submitted
     *            by the {@code execute} method.
     * @throws IllegalArgumentException
     *             if one of the following holds:<br>
     *             {@code corePoolSize < 0}<br>
     *             {@code keepAliveTime < 0}<br>
     *             {@code maximumPoolSize <= 0}<br>
     *             {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException
     *             if {@code workQueue} is null
     */
    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(),
                defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default rejected execution handler.
     *
     * @param corePoolSize
     *            the number of threads to keep in the pool, even if they are
     *            idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize
     *            the maximum number of threads to allow in the pool
     * @param keepAliveTime
     *            when the number of threads is greater than the core, this is
     *            the maximum time that excess idle threads will wait for new
     *            tasks before terminating.
     * @param unit
     *            the time unit for the {@code keepAliveTime} argument
     * @param workQueue
     *            the queue to use for holding tasks before they are executed.
     *            This queue will hold only the {@code Runnable} tasks submitted
     *            by the {@code execute} method.
     * @param threadFactory
     *            the factory to use when the executor creates a new thread
     * @throws IllegalArgumentException
     *             if one of the following holds:<br>
     *             {@code corePoolSize < 0}<br>
     *             {@code keepAliveTime < 0}<br>
     *             {@code maximumPoolSize <= 0}<br>
     *             {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException
     *             if {@code workQueue} or {@code threadFactory} is null
     */
    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory.
     *
     * @param corePoolSize
     *            the number of threads to keep in the pool, even if they are
     *            idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize
     *            the maximum number of threads to allow in the pool
     * @param keepAliveTime
     *            when the number of threads is greater than the core, this is
     *            the maximum time that excess idle threads will wait for new
     *            tasks before terminating.
     * @param unit
     *            the time unit for the {@code keepAliveTime} argument
     * @param workQueue
     *            the queue to use for holding tasks before they are executed.
     *            This queue will hold only the {@code Runnable} tasks submitted
     *            by the {@code execute} method.
     * @param handler
     *            the handler to use when execution is blocked because the
     *            thread bounds and queue capacities are reached
     * @throws IllegalArgumentException
     *             if one of the following holds:<br>
     *             {@code corePoolSize < 0}<br>
     *             {@code keepAliveTime < 0}<br>
     *             {@code maximumPoolSize <= 0}<br>
     *             {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException
     *             if {@code workQueue} or {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), handler);
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters.
     *
     * @param corePoolSize
     *            the number of threads to keep in the pool, even if they are
     *            idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize
     *            the maximum number of threads to allow in the pool
     * @param keepAliveTime
     *            when the number of threads is greater than the core, this is
     *            the maximum time that excess idle threads will wait for new
     *            tasks before terminating.
     * @param unit
     *            the time unit for the {@code keepAliveTime} argument
     * @param workQueue
     *            the queue to use for holding tasks before they are executed.
     *            This queue will hold only the {@code Runnable} tasks submitted
     *            by the {@code execute} method.
     * @param threadFactory
     *            the factory to use when the executor creates a new thread
     * @param handler
     *            the handler to use when execution is blocked because the
     *            thread bounds and queue capacities are reached
     * @throws IllegalArgumentException
     *             if one of the following holds:<br>
     *             {@code corePoolSize < 0}<br>
     *             {@code keepAliveTime < 0}<br>
     *             {@code maximumPoolSize <= 0}<br>
     *             {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException
     *             if {@code workQueue} or {@code threadFactory} or
     *             {@code handler} is null
     */
    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize || keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * Executes the given task sometime in the future. The task may execute in a
     * new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached, the
     * task is handled by the current {@code RejectedExecutionHandler}.
     *
     * @param command
     *            the task to execute
     * @throws RejectedExecutionException
     *             at discretion of {@code RejectedExecutionHandler}, if the
     *             task cannot be accepted for execution
     * @throws NullPointerException
     *             if {@code command} is null
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * Proceed in 3 steps:
         *
         * 1. If fewer than corePoolSize threads are running, try to start a new
         * thread with the given command as its first task. The call to
         * addWorker atomically checks runState and workerCount, and so prevents
         * false alarms that would add threads when it shouldn't, by returning
         * false.
         *
         * 2. If a task can be successfully queued, then we still need to
         * double-check whether we should have added a thread (because existing
         * ones died since last checking) or that the pool shut down since entry
         * into this method. So we recheck state and if necessary roll back the
         * enqueuing if stopped, or start a new thread if there are none.
         *
         * 3. If we cannot queue task, then we try to add a new thread. If it
         * fails, we know we are shut down or saturated and so reject the task.
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))
                return;
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            if (!isRunning(recheck) && remove(command))
                reject(command);
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        } else if (!addWorker(command, false))
            reject(command);
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are
     * executed, but no new tasks will be accepted. Invocation has no additional
     * effect if already shut down.
     *
     * <p>
     * This method does not wait for previously submitted tasks to complete
     * execution. Use {@link #awaitTermination awaitTermination} to do that.
     *
     * @throws SecurityException
     *             {@inheritDoc}
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers();
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }

    /**
     * Attempts to stop all actively executing tasks, halts the processing of
     * waiting tasks, and returns a list of the tasks that were awaiting
     * execution. These tasks are drained (removed) from the task queue upon
     * return from this method.
     *
     * <p>
     * This method does not wait for actively executing tasks to terminate. Use
     * {@link #awaitTermination awaitTermination} to do that.
     *
     * <p>
     * There are no guarantees beyond best-effort attempts to stop processing
     * actively executing tasks. This implementation cancels tasks via
     * {@link Thread#interrupt}, so any task that fails to respond to interrupts
     * may never terminate.
     *
     * @throws SecurityException
     *             {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    public boolean isShutdown() {
        return !isRunning(ctl.get());
    }

    /**
     * Returns true if this executor is in the process of terminating after
     * {@link #shutdown} or {@link #shutdownNow} but has not completely
     * terminated. This method may be useful for debugging. A return of
     * {@code true} reported a sufficient period after shutdown may indicate
     * that submitted tasks have ignored or suppressed interruption, causing
     * this executor not to properly terminate.
     *
     * @return {@code true} if terminating but not yet terminated
     */
    public boolean isTerminating() {
        int c = ctl.get();
        return !isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (;;) {
                if (runStateAtLeast(ctl.get(), TERMINATED))
                    return true;
                if (nanos <= 0)
                    return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Invokes {@code shutdown} when this executor is no longer referenced and
     * it has no threads.
     */
    protected void finalize() {
        shutdown();
    }

    /**
     * Sets the thread factory used to create new threads.
     *
     * @param threadFactory
     *            the new thread factory
     * @throws NullPointerException
     *             if threadFactory is null
     * @see #getThreadFactory
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    /**
     * Returns the thread factory used to create new threads.
     *
     * @return the current thread factory
     * @see #setThreadFactory(ThreadFactory)
     */
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    /**
     * Sets a new handler for unexecutable tasks.
     *
     * @param handler
     *            the new handler
     * @throws NullPointerException
     *             if handler is null
     * @see #getRejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    /**
     * Returns the current handler for unexecutable tasks.
     *
     * @return the current handler
     * @see #setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    /**
     * Sets the core number of threads. This overrides any value set in the
     * constructor. If the new value is smaller than the current value, excess
     * existing threads will be terminated when they next become idle. If
     * larger, new threads will, if needed, be started to execute any queued
     * tasks.
     *
     * @param corePoolSize
     *            the new core size
     * @throws IllegalArgumentException
     *             if {@code corePoolSize < 0}
     * @see #getCorePoolSize
     */
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0)
            throw new IllegalArgumentException();
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();
        else if (delta > 0) {
            // We don't really know how many new threads are "needed".
            // As a heuristic, prestart enough new workers (up to new
            // core size) to handle the current number of tasks in
            // queue, but stop if queue becomes empty while doing so.
            int k = Math.min(delta, workQueue.size());
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }

    /**
     * Returns the core number of threads.
     *
     * @return the core number of threads
     * @see #setCorePoolSize
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * Starts a core thread, causing it to idly wait for work. This overrides
     * the default policy of starting core threads only when new tasks are
     * executed. This method will return {@code false} if all core threads have
     * already been started.
     *
     * @return {@code true} if a thread was started
     */
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize && addWorker(null, true);
    }

    /**
     * Same as prestartCoreThread except arranges that at least one thread is
     * started even if corePoolSize is 0.
     */
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    /**
     * Starts all core threads, causing them to idly wait for work. This
     * overrides the default policy of starting core threads only when new tasks
     * are executed.
     *
     * @return the number of threads started
     */
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    /**
     * Returns true if this pool allows core threads to time out and terminate
     * if no tasks arrive within the keepAlive time, being replaced if needed
     * when new tasks arrive. When true, the same keep-alive policy applying to
     * non-core threads applies also to core threads. When false (the default),
     * core threads are never terminated due to lack of incoming tasks.
     *
     * @return {@code true} if core threads are allowed to time out, else
     *         {@code false}
     *
     * @since 1.6
     */
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * Sets the policy governing whether core threads may time out and terminate
     * if no tasks arrive within the keep-alive time, being replaced if needed
     * when new tasks arrive. When false, core threads are never terminated due
     * to lack of incoming tasks. When true, the same keep-alive policy applying
     * to non-core threads applies also to core threads. To avoid continual
     * thread replacement, the keep-alive time must be greater than zero when
     * setting {@code true}. This method should in general be called before the
     * pool is actively used.
     *
     * @param value
     *            {@code true} if should time out, else {@code false}
     * @throws IllegalArgumentException
     *             if value is {@code true} and the current keep-alive time is
     *             not greater than zero
     *
     * @since 1.6
     */
    public void allowCoreThreadTimeOut(boolean value) {
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();
        }
    }

    /**
     * Sets the maximum allowed number of threads. This overrides any value set
     * in the constructor. If the new value is smaller than the current value,
     * excess existing threads will be terminated when they next become idle.
     *
     * @param maximumPoolSize
     *            the new maximum
     * @throws IllegalArgumentException
     *             if the new maximum is less than or equal to zero, or less
     *             than the {@linkplain #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;
        if (workerCountOf(ctl.get()) > maximumPoolSize)
            interruptIdleWorkers();
    }

    /**
     * Returns the maximum allowed number of threads.
     *
     * @return the maximum allowed number of threads
     * @see #setMaximumPoolSize
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * Sets the time limit for which threads may remain idle before being
     * terminated. If there are more than the core number of threads currently
     * in the pool, after waiting this amount of time without processing a task,
     * excess threads will be terminated. This overrides any value set in the
     * constructor.
     *
     * @param time
     *            the time to wait. A time value of zero will cause excess
     *            threads to terminate immediately after executing tasks.
     * @param unit
     *            the time unit of the {@code time} argument
     * @throws IllegalArgumentException
     *             if {@code time} less than zero or if {@code time} is zero and
     *             {@code allowsCoreThreadTimeOut}
     * @see #getKeepAliveTime(TimeUnit)
     */
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;
        if (delta < 0)
            interruptIdleWorkers();
    }

    /**
     * Returns the thread keep-alive time, which is the amount of time that
     * threads in excess of the core pool size may remain idle before being
     * terminated.
     *
     * @param unit
     *            the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /* User-level queue utilities */

    /**
     * Returns the task queue used by this executor. Access to the task queue is
     * intended primarily for debugging and monitoring. This queue may be in
     * active use. Retrieving the task queue does not prevent queued tasks from
     * executing.
     *
     * @return the task queue
     */
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * Removes this task from the executor's internal queue if it is present,
     * thus causing it not to be run if it has not already started.
     *
     * <p>
     * This method may be useful as one part of a cancellation scheme. It may
     * fail to remove tasks that have been converted into other forms before
     * being placed on the internal queue. For example, a task entered using
     * {@code submit} might be converted into a form that maintains
     * {@code Future} status. However, in such cases, method {@link #purge} may
     * be used to remove those Futures that have been cancelled.
     *
     * @param task
     *            the task to remove
     * @return {@code true} if the task was removed
     */
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }

    /**
     * Tries to remove from the work queue all {@link Future} tasks that have
     * been cancelled. This method can be useful as a storage reclamation
     * operation, that has no other impact on functionality. Cancelled tasks are
     * never executed, but may accumulate in work queues until worker threads
     * can actively remove them. Invoking this method instead tries to remove
     * them now. However, this method may fail to remove tasks in the presence
     * of interference by other threads.
     */
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled())
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>) r).isCancelled())
                    q.remove(r);
        }

        tryTerminate(); // In case SHUTDOWN and now empty
    }

    /* Statistics */

    /**
     * Returns the current number of threads in the pool.
     *
     * @return the number of threads
     */
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate number of threads that are actively executing
     * tasks.
     *
     * @return the number of threads
     */
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the largest number of threads that have ever simultaneously been
     * in the pool.
     *
     * @return the number of threads
     */
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have ever been
     * scheduled for execution. Because the states of tasks and threads may
     * change dynamically during computation, the returned value is only an
     * approximation.
     *
     * @return the number of tasks
     */
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked())
                    ++n;
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the approximate total number of tasks that have completed
     * execution. Because the states of tasks and threads may change dynamically
     * during computation, the returned value is only an approximation, but one
     * that does not ever decrease across successive calls.
     *
     * @return the number of tasks
     */
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns a string identifying this pool, as well as its state, including
     * indications of run state and estimated worker and task counts.
     *
     * @return a string identifying this pool, as well as its state
     */
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked())
                    ++nactive;
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running"
                : (runStateAtLeast(c, TERMINATED) ? "Terminated" : "Shutting down"));
        return super.toString() + "[" + rs + ", pool size = " + nworkers + ", active threads = " + nactive
                + ", queued tasks = " + workQueue.size() + ", completed tasks = " + ncompleted + "]";
    }

    /* Extension hooks */

    /**
     * Method invoked prior to executing the given Runnable in the given thread.
     * This method is invoked by thread {@code t} that will execute task
     * {@code r}, and may be used to re-initialize ThreadLocals, or to perform
     * logging.
     *
     * <p>
     * This implementation does nothing, but may be customized in subclasses.
     * Note: To properly nest multiple overridings, subclasses should generally
     * invoke {@code super.beforeExecute} at the end of this method.
     *
     * @param t
     *            the thread that will run task {@code r}
     * @param r
     *            the task that will be executed
     */
    protected void beforeExecute(Thread t, Runnable r) {
    }

    /**
     * Method invoked upon completion of execution of the given Runnable. This
     * method is invoked by the thread that executed the task. If non-null, the
     * Throwable is the uncaught {@code RuntimeException} or {@code Error} that
     * caused execution to terminate abruptly.
     *
     * <p>
     * This implementation does nothing, but may be customized in subclasses.
     * Note: To properly nest multiple overridings, subclasses should generally
     * invoke {@code super.afterExecute} at the beginning of this method.
     *
     * <p>
     * <b>Note:</b> When actions are enclosed in tasks (such as
     * {@link FutureTask}) either explicitly or via methods such as
     * {@code submit}, these task objects catch and maintain computational
     * exceptions, and so they do not cause abrupt termination, and the internal
     * exceptions are <em>not</em> passed to this method. If you would like to
     * trap both kinds of failures in this method, you can further probe for
     * such cases, as in this sample subclass that prints either the direct
     * cause or the underlying exception if a task has been aborted:
     *
     * <pre>
     * {
     *     &#64;code
     *     class ExtendedExecutor extends ThreadPoolExecutor {
     *         // ...
     *         protected void afterExecute(Runnable r, Throwable t) {
     *             super.afterExecute(r, t);
     *             if (t == null && r instanceof Future<?>) {
     *                 try {
     *                     Object result = ((Future<?>) r).get();
     *                 } catch (CancellationException ce) {
     *                     t = ce;
     *                 } catch (ExecutionException ee) {
     *                     t = ee.getCause();
     *                 } catch (InterruptedException ie) {
     *                     Thread.currentThread().interrupt(); // ignore/reset
     *                 }
     *             }
     *             if (t != null)
     *                 System.out.println(t);
     *         }
     *     }
     * }
     * </pre>
     *
     * @param r
     *            the runnable that has completed
     * @param t
     *            the exception that caused termination, or null if execution
     *            completed normally
     */
    protected void afterExecute(Runnable r, Throwable t) {
    }

    /**
     * Method invoked when the Executor has terminated. Default implementation
     * does nothing. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.terminated} within this method.
     */
    protected void terminated() {
    }

    /* Predefined RejectedExecutionHandlers */

    /**
     * A handler for rejected tasks that runs the rejected task directly in the
     * calling thread of the {@code execute} method, unless the executor has
     * been shut down, in which case the task is discarded.
     */
    public static class CallerRunsPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code CallerRunsPolicy}.
         */
        public CallerRunsPolicy() {
        }

        /**
         * Executes task r in the caller's thread, unless the executor has been
         * shut down, in which case the task is discarded.
         *
         * @param r
         *            the runnable task requested to be executed
         * @param e
         *            the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }

        @Override
        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor executor) {
            // TODO Auto-generated method stub

        }
    }

    /**
     * A handler for rejected tasks that throws a
     * {@code RejectedExecutionException}.
     */
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() {
        }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r
         *            the runnable task requested to be executed
         * @param e
         *            the executor attempting to execute this task
         * @throws RejectedExecutionException
         *             always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
        }

        @Override
        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor executor) {
            // TODO Auto-generated method stub

        }
    }

    /**
     * A handler for rejected tasks that silently discards the rejected task.
     */
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardPolicy}.
         */
        public DiscardPolicy() {
        }

        /**
         * Does nothing, which has the effect of discarding task r.
         *
         * @param r
         *            the runnable task requested to be executed
         * @param e
         *            the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }

        @Override
        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor executor) {
            // TODO Auto-generated method stub

        }
    }

    /**
     * A handler for rejected tasks that discards the oldest unhandled request
     * and then retries {@code execute}, unless the executor is shut down, in
     * which case the task is discarded.
     */
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardOldestPolicy} for the given executor.
         */
        public DiscardOldestPolicy() {
        }

        /**
         * Obtains and ignores the next task that the executor would otherwise
         * execute, if one is immediately available, and then retries execution
         * of task r, unless the executor is shut down, in which case task r is
         * instead discarded.
         *
         * @param r
         *            the runnable task requested to be executed
         * @param e
         *            the executor attempting to execute this task
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }

        @Override
        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor executor) {
            // TODO Auto-generated method stub

        }
    }
}
