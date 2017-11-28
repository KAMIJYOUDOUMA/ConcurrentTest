package priv.cais.typeErasure;
class Apple extends Fruit {
    static {
        System.out.println("Apple静态块");
    }
    Apple() {
        System.out.println("Apple构造器");
    }
}