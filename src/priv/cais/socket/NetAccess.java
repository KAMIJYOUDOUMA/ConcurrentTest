package priv.cais.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * @auther CaiS
 */
public class NetAccess {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("www.baidu.com", 80);
        OutputStream out = socket.getOutputStream();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
        bw.write("POST / HTTP/1.1\r\n");
        bw.write("Host: www.baidu.com\r\n");
        bw.write("\r\n");
        bw.flush();

        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

        String line = null;

        while ((line = br.readLine()) != null) {
            System.out.println(line);
            if (line.contains("<html")) {
                break;
            }
        }

        br.close();
        bw.close();
        socket.close();

    }
}
