package priv.cais.annotation;

/**
 * @auther CaiS
 */
public class Test {

    public static void main(String[] args) throws Exception {
        Apple apple = new Apple();
        try {
            FruitName annotation = apple.getClass().getField("name").getAnnotation(FruitName.class);
            System.out.println(annotation.value());
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }

    }

}
