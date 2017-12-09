package priv.cais.dataStructure.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

public class Test {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        // int a[]= {1,2,3};
        // System.arraycopy(a, 0, a, 1, 2);
        // System.out.println(Arrays.toString(a));
        List list = new ArrayList();
        list.add("e");
        String a = (String) list.get(0);
        System.out.println(2 << 8 == (2 << 8 ^ 2 << 8 >> 16));

        Hashtable<Integer, String> hashtable = new Hashtable<>(4);
        for (int i = 0; i < 10; i++) {
            hashtable.put(i, Integer.toString(i));
        }
        hashtable.put(11, "11");

    }

}
