package com.kuaishou.kwairtcdemo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.kuaishou.kwairtcdemo.log.AppLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;
import java.util.UUID;

import static android.content.Context.ACTIVITY_SERVICE;

public class AppUtil {

    private static final int TIME = 3500;
    private static long lastClickTime = 0;
    /**
     * 处理快速双击，多击事件，在TIME时间内只执行一次事件
     *
     * @return
     */
    public static boolean isFastDoubleClick() {
        long currentTime = System.currentTimeMillis();
        long timeInterval = currentTime - lastClickTime;
        if (0 < timeInterval && timeInterval < TIME) {
            return true;
        }
        lastClickTime = currentTime;
        return false;
    }

    /**
     * 通过wifi-mac地址与设备硬件标识符等拼接成唯一设备ID
     *
     * @param context android上下文，用于获取设备信息
     * @return 返回生成的设备唯一ID值
     */
    @SuppressLint("HardwareIds")
    public static final String generateDeviceId(Context context) {
        String deviceId = getEthernetMac();
        if (!TextUtils.isEmpty(deviceId) && !Build.UNKNOWN.equals(deviceId)) {
            return deviceId;
        }

        deviceId = Build.SERIAL;

        if (!Build.UNKNOWN.equals(deviceId) && !INVALID_SERIAL_NUMBER.equals(deviceId)) {
            return deviceId;
        }

        if (!TextUtils.isEmpty(deviceId) && !Build.UNKNOWN.equals(deviceId) && !INVALID_SERIAL_NUMBER.equals(deviceId)) {
            return deviceId;
        }

        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (!"9774d56d682e549c".equals(deviceId) && !INVALID_SERIAL_NUMBER.equals(deviceId)
                && !TextUtils.isEmpty(deviceId) && deviceId.length() > 6) {
            return deviceId;
        }

        // wifi mac地址
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        String wifiMac = info.getMacAddress();
        if (!TextUtils.isEmpty(wifiMac)) {
            return String.format("w%s", wifiMac.replace(":", ""));
        }

        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取应用程序版本名称信息
     *
     * @param ctx Context
     */
    public static String getVersionName(Context ctx) {
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("AppUtil", "not found package name:" + e.getMessage());
        }
        return "";
    }

    /**
     * 获取App名称
     *
     * @param ctx Context
     */
    public static String getAppName(Context ctx) {

        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            int labelRes = packageInfo.applicationInfo.labelRes;
            return ctx.getResources().getString(labelRes);
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
            Log.e("AppUtil", "not found package name:" + e.getMessage());
        }

        return "";
    }

    /**
     * 是否有系统弹窗权限
     *
     * @param context Context
     */
    public static boolean hasSystemAlertPermission(Context context) {
        // TODO: 2020/8/20 权限判断需要做兼容性处理，后续优化
        // 判断是否有悬浮权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        } else {
            return true;
        }
    }

    /**
     * 申请系统弹窗权限
     *
     * @param activity Activity
     */
    public static void requestSystemAlertPermission(Activity activity, int requestCode) {
        // TODO: 2020/8/20 后续随着获取权限兼容性处理一起优化
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 将本应用置顶到最前端
     * 当本应用位于后台时，则将它切换到最前端
     * 针对需要获取"后台弹出界面"权限的手机使用
     *
     * @param context
     */
    private static void returnTopApp(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean isForeground = isRunningForeground(context);
            Log.d("AppUtil", "returnTopApp, isForeground:" + isForeground);
            if (!isForeground) {
                // 获取ActivityManager
                ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
                // 获得当前运行的task(任务)
                List<ActivityManager.AppTask> taskInfoList = activityManager.getAppTasks();
                for (ActivityManager.AppTask taskInfo : taskInfoList) {
                    // 找到本应用的 task，并将它切换到前台
                    if (taskInfo.getTaskInfo().topActivity.getPackageName().equals(context.getPackageName())) {
                        taskInfo.moveToFront();
                        Log.d("AppUtil", "setTopApp, moveTaskToFront break，taskID:" + taskInfo.getTaskInfo().id);
                        break;
                    }
                }
            }
        }
    }

    private static void backToApp(Activity activity) {
        Intent backIntent = new Intent(Intent.ACTION_MAIN);
        backIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        backIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        backIntent.setClass(activity.getApplicationContext(), activity.getClass());
        backIntent.putExtra("backToApp", true);
        activity.getApplicationContext().startActivity(backIntent);
    }

    /**
     * 当本应用位于后台时，则将它切换到最前端
     *
     * @param activity
     */
    public static void forceReturnAppToTop(Activity activity) {
        // 应用正在后台屏幕分享状态，坐席离开/完成业务办理情况
        int tryTimes = 0;
        Context context = activity.getApplicationContext();
        while (!isRunningForeground(context) && tryTimes < 10) {
            if (Build.BRAND.toLowerCase().contains("xiaomi")) {
                returnTopApp(activity);
            } else {
                // 针对需要获取"后台弹出界面"权限的手机使用
                backToApp(activity);
            }
            tryTimes++;
            Log.d("TryTimes", "forceReturnAppToTop: " + tryTimes);
        }
    }

    /**
     * 判断本应用是否已经位于最前端
     *
     * @param context
     * @return 本应用已经位于最前端时，返回 true；否则返回 false
     */
    private static boolean isRunningForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcessInfoList = activityManager.getRunningAppProcesses();
        // 枚举进程
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : appProcessInfoList) {
            if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (appProcessInfo.processName.equals(context.getApplicationInfo().processName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回到系统桌面
     */
    public static void backToDesktop(Activity activity) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        activity.startActivity(intent);
    }

    /**
     * 获取手机厂商
     *
     * @return 手机厂商
     */
    public static String getDeviceBrand() {
        return Build.BRAND;
    }

    private static final String INVALID_SERIAL_NUMBER = "12345678900";

    /**
     * 获取有线网卡的 MAC 地址
     *
     * @return
     */
    private static String getEthernetMac() {
        String macSerial = null;
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();

            return Build.UNKNOWN;
        }

        if (macSerial != null && macSerial.length() > 0)
            macSerial = macSerial.replaceAll(":", "");
        else {
            return Build.UNKNOWN;
        }

        return macSerial;
    }
}
