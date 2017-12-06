package priv.cais.socket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.ServerSocket;

public class Server {
    public static void main(String[] args) {
        File target;    //接收到的文件保存的位置
        FileOutputStream save;  //将接收到的文件写入电脑
        FileInputStream in;     //读取穿送过来的数据文件
        ServerSocket server;    //服务器
        Socket socket;          //套接字

        //处理客户端的请求
        try {
            //接受前文件的准备
            target = new File("E:\\tempshaohan.png");
            save = new FileOutputStream(target);

            server = new ServerSocket(2017);    //服务端口

            //等待客户端的呼叫
            System.out.println("等待客户端的呼叫");
            socket = server.accept();   //阻塞
            in = (FileInputStream)socket.getInputStream();

            //接收数据
            byte[] b = new byte[64];
            int n = in.read(b);
            int start = (int)System.currentTimeMillis();
            while (n != -1) {
                save.write(b, 0, n);    //写入指定地方
                n = in.read(b);
            }
            int end = (int)System.currentTimeMillis();
            System.out.println("接收花费的时间：" + (end-start));
            socket.close();
            server.close();
            in.close();
            save.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}