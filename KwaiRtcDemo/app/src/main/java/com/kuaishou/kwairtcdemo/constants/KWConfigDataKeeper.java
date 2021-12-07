package com.kuaishou.kwairtcdemo.constants;

public class KWConfigDataKeeper {

    public static void init(String userID, String channelID) {
        USER_ID = userID;
        CHANNEL_ID = channelID;
    }

    public static String getUserId() {
        return USER_ID;
    }

    public static void setUserId(String userId) {
        USER_ID = userId;
    }

    public static String getChannelId() {
        return CHANNEL_ID;
    }

    public static void setChannelId(String channelId) {
        CHANNEL_ID = channelId;
    }

    public static String getDeviceId() {
        return DEVICE_ID;
    }

    public static void setDeviceId(String deviceId) {
        DEVICE_ID = deviceId;
    }

    private static String USER_ID;
    private static String CHANNEL_ID;
    private static String DEVICE_ID;
}
