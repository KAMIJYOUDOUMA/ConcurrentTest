package priv.cais;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class CallableTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        CallImpl call = new CallImpl();

        FutureTask<String> task = new FutureTask<String>(call);
        Thread thread = new Thread(task);
        thread.start();
        System.out.println(task.get());

    }
}

class CallImpl implements Callable<String> {

    public String call() throws Exception { // call封装线程所需做的任务
        return "CallResult";
    }
}