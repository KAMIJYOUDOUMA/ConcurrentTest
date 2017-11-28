package priv.cais.typeErasure;

import java.util.ArrayList;
import java.util.List;

public class GenericTest {
    public static void main(String[] args) {
        Fruit[] fruit = new Apple[10];
        fruit[0] = new Apple(); // OK
        printLine();
        fruit[1] = new Jonathan(); // OK
        printLine();
        // Runtime type is Apple[], not Fruit[] or Orange[]:
        try {
            // Compiler allows you to add Fruit:
            //fruit[0] = new Fruit(); // ArrayStoreException
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            // Compiler allows you to add Oranges:
            //fruit[0] = new Orange(); // ArrayStoreException
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<? extends Fruit> flist = new ArrayList<Apple>();
        read(flist);

        List<? super Apple> apples = new ArrayList<Apple>();
        write(apples);
    }

    static void read(List<? extends Fruit> fList) {
        if (fList != null)
            for (int i = 0, len = fList.size(); i < len; i++) {
                fList.get(0);
            }
    }

    static void write(List<? super Apple> apples) {
        apples.add(new Apple());
        printLine();
        apples.add(new Jonathan());
        // apples.add(new Fruit()); // Error
    }

    static void printLine(){
        System.out.println("-------------分割线-------------");
    }

}
