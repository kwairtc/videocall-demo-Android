package com.kuaishou.kwairtcdemo.log;

import java.util.ArrayList;

/**
 * description: 日志打印基类
 * 主要用于把日志打印到LiveRoomSDK时解耦
 * @date 2020/8/12 9:36 PM
 */
public abstract class BaseLogger {
    public static final String TAG = "CommonLogger";
    private static ArrayList<IKWLogPrinter> mLogPrinters = new ArrayList<>();

    /**
     * 打印日志
     * 如果没有重新设置mLogPrinter，默认使用Log.d()
     *
     * @param logMsg 日志信息
     */
    protected static void printLog(LogLevel level, String logMsg) {

        for (IKWLogPrinter logPrinter : mLogPrinters) {
            logPrinter.printLog(level, logMsg);
        }
    }

    public static void addLogPrinter(IKWLogPrinter logPrinter) {
        mLogPrinters.add(logPrinter);
    }

    public static void removeLogPrinter(IKWLogPrinter logPrinter) {
        mLogPrinters.remove(logPrinter);
    }
}
