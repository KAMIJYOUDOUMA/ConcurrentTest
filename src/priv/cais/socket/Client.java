package priv.cais.socket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        File src;       //需要传送的文件
        Socket socket;  //套接字
        FileInputStream open;   //读取文件
        FileOutputStream out;   //传送文件

        try {
            //需要传输的文件
            src = new File("E:\\shaohan.png");
            open = new FileInputStream(src);

            //连接服务器
            socket = new Socket("127.0.0.1", 2017);
            out = (FileOutputStream)socket.getOutputStream();

            //开始传送
            byte[] b = new byte[64];
            int n = open.read(b);
            int start = (int)System.currentTimeMillis();
            while (n != -1) {
                out.write(b, 0, n);
                n = open.read(b);
            }
            int end = (int)System.currentTimeMillis();
            System.out.println( "发送花费的时间：" + (end-start));
            //关闭流
            out.close();
            socket.close();
            open.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}