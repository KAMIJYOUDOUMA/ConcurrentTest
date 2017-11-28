package priv.cais.typeErasure;
class Jonathan extends Apple {
    static {
        System.out.println("Jonathan静态块");
    }
    Jonathan() {
        System.out.println("Jonathan构造器");
    }
}