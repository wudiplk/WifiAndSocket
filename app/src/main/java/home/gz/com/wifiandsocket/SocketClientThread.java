package home.gz.com.wifiandsocket;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * @author Wudi
 * @date 2018/10/31
 */
public class SocketClientThread implements Runnable {

    private Socket socket;

    /**
     * 发送处理
     */
    public Handler sendHandler;

    /**
     * 接受处理
     */
    private Handler recHandler;

    private BufferedReader bufferedReader;

    private OutputStream outputStream;

    private boolean isStop = false;

    private String ip;

    public SocketClientThread(String ip, Handler recHandler) {
        this.recHandler = recHandler;
        this.ip = ip;
    }

    @Override
    public void run() {
        while (true) {
            try {
                socket = new Socket(ip, 3000);
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream = socket.getOutputStream();
                // 开启子线程读取服务器数据
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            readStream(socket.getInputStream());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                Looper.prepare();
                sendHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        if (msg.what == 2) {
                            try {
                                outputStream.write((msg.obj.toString() + "\n").getBytes("utf-8"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                Looper.loop();
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 读取流
     *
     * @param inStream
     * @return 字节数组
     * @throws Exception
     */
    public void readStream(InputStream inStream) throws Exception {
        String content = "";
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048 * 2];
        int len = -1;
        while ((len = inStream.read(buffer)) != -1) {
            outSteam.write(buffer, 0, len);
            content = outSteam.toString();
            Log.d("收到的消息", content);
            // 读取数据
            Message message = new Message();
            message.what = 1;
            message.obj = content;
            recHandler.sendMessage(message);
        }
        outSteam.close();
        inStream.close();

    }

    public void onStop() {
        isStop = true;
        try {
            bufferedReader.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
