package home.gz.com.wifiandsocket;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Wudi
 * @date 2018/10/31
 */
public class SocketService extends Service {

    /**
     * 是否运行
     */
    private boolean isRunning;

    private SocketServiceThread socketServiceThread;

    private List<Socket> socketList = new ArrayList<>();

    @Override
    public void onCreate() {
        isRunning = true;

    }

    @Override
    public void onDestroy() {
        socketServiceThread.onStop();
        isRunning = false;
        socketList.clear();
        super.onDestroy();
    }

    private ServerSocket serverSocket;
    private Handler serHandler;
    public Handler sendHandler;

    public void onStart(Handler handler) {
        this.serHandler = handler;
        try {
            serverSocket = new ServerSocket(3000);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        // 等待客户端连接，获取套接字
                        Socket socket = serverSocket.accept();
                        socketList.add(socket);
                        // 每当客户端连接后启动一天线程为其服务
                        socketServiceThread = new SocketServiceThread(socket);
                        new Thread(socketServiceThread).start();

                        Looper.prepare();
                        sendHandler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                if (msg.what == 5) {
                                    for (Iterator<Socket> socketIterator = socketList.iterator(); socketIterator.hasNext(); ) {
                                        Socket inSocket = socketIterator.next();
                                        Log.d("信息", inSocket.toString());
                                        try {
                                            OutputStream outputStream = inSocket.getOutputStream();
                                            outputStream.write(((String) msg.obj.toString()).getBytes("utf-8"));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            socketIterator.remove();
                                        }
                                    }
                                }
                            }
                        };
                        Looper.loop();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    private class SocketServiceThread implements Runnable {

        private Socket mSocket;

        /**
         * 该线程所处理Socket对应的输入流
         */
        private BufferedReader mBufferedReader;

        private boolean isStop;

        public SocketServiceThread(Socket mSocket) {
            this.mSocket = mSocket;
            this.isStop = false;
            try {
                mBufferedReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                readStream(mSocket.getInputStream());

            } catch (Exception e) {
                e.printStackTrace();
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
                Message message = new Message();
                message.what = 4;
                message.obj = content;
                serHandler.sendMessage(message);
            }
            outSteam.close();
            inStream.close();

        }

        /**
         * 读取客户端数据
         *
         * @return
         */
        private String readFromClient() {
            try {
                return mBufferedReader.readLine();
            } catch (IOException e) {
                // 捕获到异常，对应的客户端已经关闭
                e.printStackTrace();
                // 移除该socket
                socketList.remove(mSocket);
            }
            return null;
        }

        public void onStop() {
            isStop = true;
            try {
                mBufferedReader.close();
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class LocalBinder extends Binder {
        public SocketService getSocketService() {
            return SocketService.this;
        }
    }
}
