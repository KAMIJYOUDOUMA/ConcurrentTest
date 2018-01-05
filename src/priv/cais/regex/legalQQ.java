package priv.cais.regex;

/**
 * @auther CaiS
 */
public class legalQQ {

    public static void main(String[] args) {
        checkQQ("1232143");
    }

    /**
     * 校验QQ号，要求：必须是5~15位数字，0不能开头。没有正则表达式之前
     *
     * @param qq
     */
    public static void checkQQ(String qq) {
        String reg = "[1-9][0-9]{4,14}";
        System.out.println(qq.matches(reg) ? "合法qq" : "非法qq");
    }
}
