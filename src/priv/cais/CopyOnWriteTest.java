package priv.cais;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CopyOnWriteTest {

    private static final int THREAD_COUNT = 10;

    private static int count=0;

    public static void main(String[] args) {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>();
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        executorService.execute(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

            }
        });
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    for (int i = 0; i < THREAD_COUNT; i++) {
                        list.add(count++ +"");
                    }
                }
            });
        }
        System.out.println(list.toString());
        executorService.shutdown();
    }

}
