package priv.cais.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.cglib.proxy.Enhancer;

/**
 * @auther CaiS
 */
public class CglibTest {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass((new ArrayList<Integer>()).getClass());
        enhancer.setCallback(new Cglib());
        List<Integer> list = (ArrayList<Integer>) enhancer.create();
        list.add(1);
        HashMap<Integer, Integer> hashMap = new  HashMap<>(1);
        hashMap.put(1, 1);
        hashMap.put(2, 2);
    }

    static void get(String[] strs){}

}
