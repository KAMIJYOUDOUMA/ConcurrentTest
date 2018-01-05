package priv.cais.regex;

import java.util.regex.Pattern;

/**
 * @auther CaiS
 */
public class Regex {

    public static void main(String[] args) {
        String content = "I am noob " + "from runoob.com.";

        String pattern = ".*runoob.*";

        boolean isMatch = Pattern.matches(pattern, content);
        System.out.println("字符串中是否包含了 'runoob' 子字符串: " + isMatch);


        String filename = "123.xLs";

        String filePattern = "^(?:\\w+\\.(?i)xlsx?)$";

        boolean isExcel = Pattern.matches(filePattern, filename);
        System.out.println("文件是否是Excel文件：" + isExcel);
    }

}
