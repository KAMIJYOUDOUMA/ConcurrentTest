package priv.cais.typeErasure;
class Orange extends Fruit {
    static {
        System.out.println("Orange静态块");
    }
    Orange() {
        System.out.println("Orange构造器");
    }
}