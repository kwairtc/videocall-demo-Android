package com.kuaishou.kwairtcdemo.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;

import com.kuaishou.kwairtcdemo.R;


/**
 * 用于后台音视频进程保活
 *
 */
public class ForeGroundService extends Service {

    public static final String FOREGROUND_SERVICE_CHANNEL_ID = "foreground_service_channel_id";

    public static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 0xc000;

    private NotificationManager mNotificationManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mNotificationManager != null) {
            mNotificationManager.cancel(FOREGROUND_SERVICE_NOTIFICATION_ID);
        }
    }

    /**
     * 如果没有从状态栏中删除ICON，且继续调用addIconToStatusbar,则不会有任何变化.如果将notification中的resId设置不同的图标，则会显示不同的图标
     */
    private void showNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_logo)
                // TODO: 通知栏显示样式
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
//                .setContentTitle("1111111")
//                .setContentText("2222222222")
                .setOngoing(true)
                .setWhen(System.currentTimeMillis());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(FOREGROUND_SERVICE_CHANNEL_ID, "my_channel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(false);
            channel.setLightColor(Color.GREEN);
            channel.setShowBadge(true);
            mNotificationManager.createNotificationChannel(channel);
            builder.setChannelId(FOREGROUND_SERVICE_CHANNEL_ID);
        }

        // TODO: 仅发送了一个通知，需要添加点击通知可以回到音视频页面功能
//        Intent intent = new Intent(this, VideoServiceActivity.class);
//        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        //给通知添加点击意图
//        builder.setContentIntent(pi);

        Notification notification = builder.build();
        mNotificationManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID, notification);
        startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notification);
    }
}
