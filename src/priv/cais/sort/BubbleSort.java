package priv.cais.sort;

import java.util.Arrays;

/**
 * @auther CaiS
 */
public class BubbleSort {

    public static void sort(int[] array) {
        for (int i = 0; i < array.length; i++) {
            for (int j = i + 1; j < array.length; j++) {
                if (array[i] > array[j]) {
                    array[i] = array[i] ^ array[j];
                    array[j] = array[i] ^ array[j];
                    array[i] = array[i] ^ array[j];
                }
            }
        }
    }

    public static void betterSort(int[] array) {
        int temp = 0;
        boolean swap;
        for (int i = array.length - 1; i > 0; --i) {
            swap = false;
            for (int j = 0; j < i; ++j) {
                if (array[j] > array[j + 1]) {
                    temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                    swap = true;
                }
            }
            if (!swap) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        int[] array = { 1, 7, 3, 5, 4 };
        betterSort(array);
        System.out.println(Arrays.toString(array));
    }

}
