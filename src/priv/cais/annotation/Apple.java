package priv.cais.annotation;
/**
 * @auther CaiS
 */
public class Apple {
    @FruitName("apple")
    public String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
