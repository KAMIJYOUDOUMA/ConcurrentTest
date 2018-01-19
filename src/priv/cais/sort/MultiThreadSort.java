package priv.cais.sort;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @auther CaiS
 */
public class MultiThreadSort {

    private final static ExecutorService ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static void main(String[] args) throws InterruptedException {
        Random random = new Random();
        int[] array = new int[100000];
        for (int i = 0, mark = array.length; i < array.length; i++, mark--) {
            array[i] = random.nextInt(mark + 1000);
        }
        sort(array, 0, array.length - 1);
        Thread.sleep(10);
        System.out.println(Arrays.toString(array));
        ex.shutdown();
    }

    public static void sort(int[] array, int start, int end) {
        int s = start;
        int e = end;
        int key = array[start];
        while (s < e) {
            while (array[e] >= key && s < e) {
                e--;
            }
            if (s < e) {
                array[s++] = array[e];
            }
            while (array[s] <= key && s < e) {
                s++;
            }
            if (s < e) {
                array[e--] = array[s];
            }
        }
        array[s] = key;
        if (start < s) {
            ex.execute(new SortThread(array, start, s - 1));
        }
        if (e < end) {
            ex.execute(new SortThread(array, e + 1, end));
        }
    }

}

class SortThread implements Runnable {

    private int[] array;

    private int start;

    private int end;

    public SortThread(int[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        MultiThreadSort.sort(array, start, end);
    }
}
