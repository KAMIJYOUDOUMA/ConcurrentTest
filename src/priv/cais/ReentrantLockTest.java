package priv.cais;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTest {

    private static int count = 0;

    static  StringBuilder sBuilder = new StringBuilder();
    static  StringBuffer sBuffer = new StringBuffer();

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ReentrantLock lock = new ReentrantLock();
        ExecutorService eService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            eService.execute(new Runnable() {
                @Override
                public void run() {
                    lock.lock();
                    try {
                        //System.out.println(sBuilder.append( count++));
                        System.out.println(sBuffer.append( count++));
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }
        eService.shutdown();

    }
}
