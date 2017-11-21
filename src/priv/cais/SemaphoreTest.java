package priv.cais;

import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;

public class SemaphoreTest {
    private static final int THREAD_COUNT = 10;

    private static ThreadPoolExecutor threadPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(THREAD_COUNT);

    private static Semaphore s = new Semaphore(10,true);

    public static void main(String[] args) {
        for (int i = 0; i < THREAD_COUNT; i++) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        System.out.println("当前可用"+ Thread.currentThread().getName() +"（获取前）:"+ s.availablePermits());
                        s.acquire();
                        System.out.println("当前可用"+ Thread.currentThread().getName() +"（获取后）：" + s.availablePermits());
                    } catch (InterruptedException e) {
                    } finally {
                        s.release();
                    }
                }
            });
        }
        threadPool.shutdown();
    }
}
