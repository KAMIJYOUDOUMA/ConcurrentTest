package priv.cais.sort;

import java.util.Arrays;

/**
 * @auther CaiS
 */
public class SelectSort {

    public static void sort(int[] array) {
        int size = array.length;
        for (int i = 0; i < size; i++) {
            int min =array[i];
            for (int j = size - 1; j > i; j--) {
                if (array[j] < min) {
                    min = array[j];
                    array[j] = array[i];
                    array[i] = min;
                }
            }
        }
    }

    public static void main(String[] args) {
        int[] array = { 1, 7, 3, 5, 4 };
        sort(array);
        System.out.println(Arrays.toString(array));
    }
}
