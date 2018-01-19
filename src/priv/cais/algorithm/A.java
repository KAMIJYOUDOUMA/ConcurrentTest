package priv.cais.algorithm;

/**
 * @auther CaiS
 */
public class A {

    /**
     * 给定一个数列，长度为N， 求这个数列的最长上升（递增）子数列（LIS）的长度. 以 1 7 2 8 3 4 为例。 这个数列的最长递增子数列是 1
     * 2 3 4，长度为4； 次长的长度为3， 包括 1 7 8; 1 2 3 等.
     */
    public static void main(String[] args) {
        int[] array = { 1, 7, 2, 8, 3, 4 };
        System.out.println(fun(array, array.length));
    }

    static int fun(int[] array, int n) {
        if (n == 1) {
            return 1;
        } else {
            if (array[n - 1] > max(array, n - 2)) {
                return fun(array, n - 1) + 1;
            }
            return fun(array, n - 1);
        }
    }

    /**
     * 求数组array到下标index的最大值
     * @return
     */
    static int max(int[] array, int index) {
        int max = array[0];
        for (int i = 0; i < index; i++) {
            max = Math.max(array[i], max);
        }
        return max;
    }
}
