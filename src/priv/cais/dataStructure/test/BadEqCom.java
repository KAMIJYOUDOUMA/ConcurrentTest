package priv.cais.dataStructure.test;

/**
 * @auther CaiS
 */
public class BadEqCom {
    private int i;

    public BadEqCom(int i) {
       this.i = i;
    }
    @Override
    public boolean equals(Object o) {
        if (o instanceof Integer) {
            if (i % 2 == 0) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (i % 2 == 0) {
            return 1;
        }
        return 2;
    }

}
