package priv.cais;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchTest {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        CountDownLatch countDownLatch = new CountDownLatch(3);

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName());
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + ":Complete!");

            }
        });

        Thread t2 = new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName());
                countDownLatch.countDown();
                System.out.println(Thread.currentThread().getName() + ":Complete!");

            }
        });

        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName());
                // t1.interrupt();
                countDownLatch.countDown();
                System.out.println(Thread.currentThread().getName() + ":Complete!");
            }
        });

        Thread t4 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName());
                countDownLatch.countDown();
                System.out.println(Thread.currentThread().getName() + ":Complete!");
            }
        });

        t1.start();
        t2.start();
        t3.start();
        t4.start();
    }

}
