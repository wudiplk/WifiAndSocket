package home.gz.com.wifiandsocket;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SocketActivity extends AppCompatActivity {

    @BindView(R.id.tvClientOpen)
    Button tvClientOpen;
    @BindView(R.id.edClientInput)
    EditText edClientInput;
    @BindView(R.id.btClientSend)
    Button btClientSend;
    @BindView(R.id.tvServiceOpen)
    Button tvServiceOpen;
    @BindView(R.id.edServiceInput)
    EditText edServiceInput;
    @BindView(R.id.btServiceSend)
    Button btServiceSend;
    @BindView(R.id.tvClient)
    TextView tvClient;
    @BindView(R.id.tvService)
    TextView tvService;
    private String TAG = this.getClass().getSimpleName();
    private SocketWifiBroadcastReceiver wifiBroadcastReceiver;
    private WifiUtil wifiUtil;
    private int servicePort = 3000;

    private Handler serHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket);
        ButterKnife.bind(this);
        this.checkPermissions(this.mNeedPermissions);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiBroadcastReceiver);
    }

    @OnClick({R.id.tvClientOpen, R.id.tvServiceOpen, R.id.btClientSend, R.id.btServiceSend})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btClientSend:
                if (socketClientThread.sendHandler != null) {
                    Message message = new Message();
                    message.what = 2;
                    message.obj = edClientInput.getText().toString();
                    tvClient.append(edClientInput.getText() + "\n");
                    socketClientThread.sendHandler.sendMessage(message);
                }
                break;
            case R.id.btServiceSend:
                if (localBinder != null) {
                    Message message1 = new Message();
                    message1.what = 5;
                    message1.obj = edServiceInput.getText().toString();
                    if (localBinder.getSocketService().sendHandler != null) {
                        localBinder.getSocketService().sendHandler.sendMessage(message1);
                    }
                    tvService.append(edServiceInput.getText()+"\n");
                }
                break;
            case R.id.tvServiceOpen:
                initServiceSocket();
                break;
            case R.id.tvClientOpen:

                wifiBroadcastReceiver = new SocketWifiBroadcastReceiver();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
                intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                registerReceiver(wifiBroadcastReceiver, intentFilter);

                wifiUtil = new WifiUtil(this);
                wifiUtil.connectWithoutConfig("12345678", "12345678");
                break;
            default:
                break;
        }
    }

    private void initServiceSocket() {
        Intent intent = new Intent(this, SocketService.class);
        bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);

        serHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 4) {
                    tvService.append(msg.obj.toString() + "\n");
                }
                return false;
            }
        });
    }


    private Handler revHandler;

    private SocketClientThread socketClientThread;

    private Thread clientThread;

    private void initClientSocket() {
        tvClient.append("端口号：" + servicePort + "\n");
        tvClient.append("本机IP：" + wifiUtil.getIpAddress() + "\n");
        tvClient.append("HostIP：" + wifiUtil.getServerIpAddress() + "\n");
        revHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 1) {
                    tvClient.append(msg.obj.toString() + "\n");
                }
                return false;
            }
        });

        socketClientThread = new SocketClientThread(String.valueOf(wifiUtil.getServerIpAddress()), revHandler);
        clientThread= new Thread(socketClientThread);
        clientThread.start();
        Log.d(TAG, "客户端已启动...");
    }

    private SocketService.LocalBinder localBinder;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (localBinder == null) {
                localBinder = ((SocketService.LocalBinder) service);
                localBinder.getSocketService().onStart(serHandler);
                wifiUtil = new WifiUtil(SocketActivity.this);
                tvService.append("端口号：" + servicePort + "\n");
                tvService.append("本机IP：" + wifiUtil.getIpAddress() + "\n");
                tvService.append("HostIP：" + wifiUtil.getServerIpAddress() + "\n");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    /**
     * 需要进行检测的权限数组
     */
    public String[] mNeedPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    /**
     * 检测权限
     *
     * @param permissions
     */
    public boolean checkPermissions(String... permissions) {
        List<String> needRequestPermissionList = findDeniedPermissions(permissions);
        if (null != needRequestPermissionList && needRequestPermissionList.size() > 0) {
            // 申请权限
            ActivityCompat.requestPermissions(this, needRequestPermissionList.toArray(new String[needRequestPermissionList.size()]), 101);
        }
        return needRequestPermissionList.size() <= 0;
    }

    /**
     * 检测所需的权限是否在已授权的列表中
     *
     * @param permissions
     * @return
     */
    private List<String> findDeniedPermissions(String[] permissions) {
        List<String> needRequestPermissionList = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                needRequestPermissionList.add(perm);
            }
        }
        return needRequestPermissionList;
    }

    private class SocketWifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                final int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        // wifi已经关闭
                        Log.d(TAG, "wifi已经关闭");
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        Log.d(TAG, "wifi正在关闭");
                        //wifi正在关闭
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        if (wifiUtil != null) {
                            new CountDownTimer(6000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    if (wifiUtil.isConnect()) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                initClientSocket();
                                            }
                                        });
                                        cancel();
                                    }
                                }

                                @Override
                                public void onFinish() {
                                }
                            }.start();
                        }
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        Log.d(TAG, "wifi正在开启");
                        //wifi正在开启
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
