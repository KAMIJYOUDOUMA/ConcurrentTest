package priv.cais.typeErasure;

class Fruit {
    static {
        System.out.println("Fruit静态块");
    }

    Fruit() {
        System.out.println("Fruit构造器");
    }
}