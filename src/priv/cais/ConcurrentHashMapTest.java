package priv.cais;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapTest {

    public static void main(String[] args) {
        ConcurrentHashMap<HashTest, String> c = new ConcurrentHashMap<HashTest, String>(1);
        HashTest ht = new HashTest();
        HashTest ht2 = new HashTest();
        HashTest ht3 = new HashTest();
        ht2.setI(2);
        ht3.setI(3);
        c.put(ht, "1");
        c.put(ht2, "2");
        c.put(ht3, "33");
        for (int i = 0; i < 3; i++) {
            c.put(new HashTest(), i+"");
        }

        String h = c.get(ht3);
        System.out.println(h);
    }

}

class HashTest {
    private int i;

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    public int getI() {
        if (this.i % 2 == 0) {
            return 0;
        }
        return 1;
    }

    public void setI(int i) {
        this.i = i;
    }
}
