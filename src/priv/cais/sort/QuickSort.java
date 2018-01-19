package priv.cais.sort;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * @auther CaiS
 */
public class QuickSort{
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
            sort(array, start, s - 1);
        }
        if (e < end) {
            sort(array, e + 1, end);
        }
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        int[] array = {1,7,3,5,4};
        sort(array, 0, array.length - 1);
        System.out.println(Arrays.toString(array));
    }

}
