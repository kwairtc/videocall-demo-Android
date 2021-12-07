package com.kuaishou.kwairtcdemo.base;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import com.kuaishou.kwairtcdemo.BuildConfig;
import com.kuaishou.kwairtcdemo.utils.DisPlayUtil;
import com.kuaishou.kwairtcdemo.utils.NetUtils;

/**
 * 用来规范其它类的实现及行为
 * 1.封装了网络变化监听，网络状态变化时会调用 notifyNetworkChange
 *
 */
public abstract class BaseActivity extends Activity {
    /**
     * 网络是否已连接
     */
    protected boolean isNetworkConnected;
    private KWNetBroadcastReceiver netBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        KWApplication.application.setAppStateListener(mAppSate);

        initViews();
        // 设置渲染式菜单栏
        setStatusBar();

        this.isNetworkConnected = NetUtils.isNetConnect(this);
        notifyNetworkChange(this.isNetworkConnected);
        registerNetworkReceiver();

        initData();
    }

    protected abstract void initViews();

    protected void setStatusBar() {
        DisPlayUtil.fullScreen(this);
    }

    public Context getContext() {
        return this;
    }

    private void registerNetworkReceiver() {
        //实例化IntentFilter对象
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        netBroadcastReceiver = new KWNetBroadcastReceiver();
        registerReceiver(netBroadcastReceiver, filter);
    }

    protected void initData() {
    }

    protected void onAppStateCallback(boolean isForeGround) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(netBroadcastReceiver);
    }

    /**
     * 网络变化通知
     *
     * @param isNetworkConnected 网络连接状态，已连接：true，断开连接：false
     */
    protected void notifyNetworkChange(boolean isNetworkConnected) {
    };

    protected String getDemoVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public boolean isActivityExist(String className) {
        Intent intent = new Intent();
        intent.setClassName(getPackageName(), className);

        if (intent.resolveActivity(getPackageManager()) == null) {
            return false;
        }
        return true;
    }

    class KWNetBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 如果相等的话就说明网络状态发生了变化
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                // 接口回调传过去状态的类型
                isNetworkConnected = NetUtils.isNetConnect(context);
                notifyNetworkChange(isNetworkConnected);
            }
        }
    }

    IAppState mAppSate = new IAppState() {
        @Override
        public void onAppState(boolean isForeground) {
            onAppStateCallback(isForeground);
        }
    };
}
