package priv.cais.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;

/**
 * @auther CaiS
 */
public class NetAccess2 {
    public static void main(String[] args) throws IOException {
        InputStream in = null ;
        BufferedReader br = null;
        BufferedWriter bw = null;

        try {
             in = (new URL("http://www.caisong.top/")).openConnection().getInputStream();
             br = new BufferedReader(new InputStreamReader(in));
             bw = new BufferedWriter(new FileWriter("D:\\123.txt"));
             String line = null;

             while ((line = br.readLine()) != null) {
                 System.out.println(line);
                 bw.write(line);
                 bw.newLine();
                 if (line.contains("</html>")) {
                     break;
                 }
             }
        } catch (IOException e) {

        }finally {
            if (br != null) {
                br.close();
            }
            if (bw != null) {
                bw.close();
            }

        }


    }
}
