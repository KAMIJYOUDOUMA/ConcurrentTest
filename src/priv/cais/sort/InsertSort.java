package priv.cais.sort;

import java.util.Arrays;

/**
 * @auther CaiS
 */
public class InsertSort {

    public static void sort(int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            int temp = array[i + 1];
            if (array[i] > array[i + 1]) {
                int l = 0;
                int r = i;
                while (l < r) {
                    int mid = (l + r) / 2;
                    if (array[mid] > temp) {
                        r--;
                    } else {
                        l++;
                    }
                }
                for (int j = i + 1; j > l; j--) {
                    array[j] = array[j - 1];
                }
                array[l] = temp;
            }
        }
    }

    public static void main(String[] args) {
        int[] array = { 1, 7, 3, 5, 4 };
        sort(array);
        System.out.println(Arrays.toString(array));
    }

}
