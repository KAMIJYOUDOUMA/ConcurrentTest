package priv.cais.dataStructure.test;

import priv.cais.dataStructure.HashMap;

public class Test {

    public static void main(String[] args) {
        HashMap<BadEqCom, Integer> hashMap = new HashMap<>(65);
        for (int i = 0; i < 2*10; i += 2) {
            hashMap.put(new BadEqCom(i), i);
        }
        System.out.println("End.");

        HashMap<Integer, Integer> hashMap2 = new HashMap<>(16);
        hashMap2.put(1,1);
        hashMap2.put(17,1);
        hashMap2.put(33,1);
    }

}
