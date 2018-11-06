package home.gz.com.wifiandsocket;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * @author Wudi
 * @date 2018/11/1
 */
public class WifiUtil {

    private String TAG = "WifiUtil";
    /**
     * 定义一个WifiLock
     */
    private WifiManager.WifiLock mWifiLock;

    public enum Type {
        /**
         * WiFi加密的几种方式
         */
        WPA, WEP, NONE
    }

    private WifiManager wifiManager;

    private ConnectivityManager connectivityManager;

    public WifiUtil(Context context) {
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager= (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * 判断WIFI连接状态
     * @return
     */
    public boolean isConnect(){
        boolean isWifiConnect=false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // 获取Wifi网络信息
            NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            // 获取移动网络信息
            NetworkInfo mobileNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (wifiNetworkInfo != null) {
                isWifiConnect = wifiNetworkInfo.isConnected();
            }
            if (mobileNetworkInfo != null) {
                isWifiConnect = mobileNetworkInfo.isConnected();
            }
        } else {
            // 获取所有的网络连接信息
            Network[] networks = connectivityManager.getAllNetworks();
            if (networks != null) {
                for (int j = 0; j < networks.length; j++) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(networks[j]);
                    if (networkInfo != null) {
                        isWifiConnect = networkInfo.getTypeName().equals("WIFI") && networkInfo.isConnected();
                    }
                }
            }

        }
        return isWifiConnect;
    }
    /**
     * 打开WIFI
     *
     * @return
     */
    public boolean openWifi() {

        boolean bRet = true;
        if (!wifiManager.isWifiEnabled()) {
            bRet = wifiManager.setWifiEnabled(true);
        }
        return bRet;
    }

    /**
     * 关闭WIFI
     */
    public void closeWifi() {
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
    }

    /**
     * 无配置记录链接方式
     */
    public void connectWithoutConfig(String ssid, String password) {
        //打开wifi
        if (!openWifi()) {
            wifiManager.setWifiEnabled(true);
        }

        // 等到wifi状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
        while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
            Log.d(TAG, "正在连接wifi....");
            try {
                // 为了避免程序一直while循环，让它睡个100毫秒在检测……
                Thread.currentThread();
                Thread.sleep(100);
            } catch (InterruptedException ie) {
            }
        }
        //判断是否已配置过当前热点
        WifiConfiguration config = createWifiInfo(ssid, password, Type.WPA);

        int netId = wifiManager.addNetwork(config);
        if (netId == -1) {
            Log.d(TAG, "wifi连接操作失败成功");
        }
        boolean bRet = wifiManager.enableNetwork(netId, true);
        if (bRet) {
            Log.d(TAG, "wifi连接成功！");
        } else {
            Log.d(TAG, "wifi连接失败！");
        }
    }

    /**
     * 通过netid 移除wifi配置
     *
     * @param netId
     * @return
     */
    public boolean removeWifi(int netId) {
        return wifiManager.removeNetwork(netId);
    }

    /**
     * 通过名称移除wifi配置
     *
     * @param SSID
     * @return
     */
    public boolean removeWifi(String SSID) {
        if (isConfig(SSID) != null) {
            return removeWifi(isConfig(SSID).networkId);
        } else {
            return false;
        }
    }

    /**
     * 是否有配置
     *
     * @param ssid
     * @return
     */
    public WifiConfiguration isConfig(String ssid) {
        List<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs != null && existingConfigs.size() > 0) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + ssid + "\"")) {
                    return existingConfig;
                }
            }
        }
        return null;
    }

    /**
     * @param ssid
     * @param password
     * @return
     */
    public WifiConfiguration createWifiInfo(String ssid, String password, Type type) {
        // 如果有相同配置的，就先删除
        WifiConfiguration oldWifiConfiguration = isConfig(ssid);
        if (oldWifiConfiguration != null) {
            wifiManager.removeNetwork(oldWifiConfiguration.networkId);
        }
        // 添加新配置
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";

        if (type == Type.NONE) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (type == Type.WEP) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == Type.WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    /**
     * 获取热点的加密类型
     */
    public Type getWifiType(ScanResult scanResult) {
        if (scanResult.capabilities.contains("WPA")) {
            return Type.WPA;
        } else if (scanResult.capabilities.contains("WEP")) {
            return Type.WEP;
        } else {
            return Type.NONE;
        }

    }

    /**
     * 检查当前WIFI状态
     *
     * @return
     */
    public int checkState() {
        return wifiManager.getWifiState();
    }

    /**
     * 锁定WifiLock
     */
    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    /**
     * 解锁WifiLock
     */
    public void releaseWifiLock() {
        // 判断时候锁定
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    /**
     * 创建一个WifiLock
     */
    public void creatWifiLock() {
        mWifiLock = wifiManager.createWifiLock(TAG);
    }

    /**
     * 得到MAC地址
     *
     * @return
     */
    public String getMacAddress() {
        return (wifiManager.getConnectionInfo() == null) ? "NULL" : wifiManager.getConnectionInfo().getMacAddress();
    }

    /**
     * 得到接入点的BSSID
     *
     * @return
     */
    public String getBSSID() {
        return (wifiManager.getConnectionInfo() == null) ? "NULL" : wifiManager.getConnectionInfo().getBSSID();
    }

    /**
     * 得到当前IP地址
     *
     * @return
     */
    public String getIpAddress() {
        String ip = "";
        if (wifiManager.getDhcpInfo() != null) {
            int i = wifiManager.getDhcpInfo().ipAddress;
            ip = (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                    + "." + (i >> 24 & 0xFF);
        }

        return ip;
    }

    public String getServerIpAddress() {
        String hostIp = "";
        if (wifiManager != null) {
            int i = wifiManager.getDhcpInfo().serverAddress;
            hostIp = (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                    + "." + (i >> 24 & 0xFF);
            wifiManager.getDhcpInfo();

        }
        return hostIp;
    }

    /**
     * 得到连接的ID
     *
     * @return
     */
    public int getNetworkId() {
        return (wifiManager.getConnectionInfo() == null) ? 0 : wifiManager.getConnectionInfo().getNetworkId();
    }

    /**
     * 得到WifiInfo的所有信息包
     *
     * @return
     */
    public String getWifiInfo() {
        return (wifiManager.getConnectionInfo() == null) ? "NULL" : wifiManager.getConnectionInfo().toString();
    }
}
