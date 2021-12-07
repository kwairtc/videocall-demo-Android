package com.kuaishou.kwairtcdemo.log;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>
 * AppLogger
 * <p>
 * 管理app日志, 通过 {@link #i(Class, String, Object...)}
 * <p>
 * 用法如下:
 * <p>
 * AppLogger.i(AppLogger.class, "test out info log");
 * <p>
 * AppLogger.e(AppLogger.class, "test out error log");
 * <p>
 * AppLogger.w(AppLogger.class, "test out warn log");
 * <p>
 * AppLogger.d(AppLogger.class, "test out debug log");
 * <p>
 * AppLogger.c(AppLogger.class, "test broadcast custom log");
 */
public class AppLogger extends BaseLogger {
    private static final String TAG = "AppLogger";
    private static final SimpleDateFormat sLogFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

    private static String getLogStr() {
        return sLogFormat.format(new Date());
    }

    public static void e(Class mClass, String msgFormat, Object... args) {
        String message = String.format(msgFormat, args);
        log(mClass, message, LogLevel.ERROR);
    }

    public static void i(Class mClass, String msgFormat, Object... args) {
        String message = String.format(msgFormat, args);
        log(mClass, message, LogLevel.INFO);
    }

    public static void w(Class mClass, String msgFormat, Object... args) {
        String message = String.format(msgFormat, args);
        log(mClass, message, LogLevel.WARN);
    }

    public static void d(Class mClass, String msgFormat, Object... args) {
        String message = String.format(msgFormat, args);
        log(mClass, message, LogLevel.DEBUG);
    }

    public static void c(Class mClass, String eventMethodName, int stateCode, String paramInfo) {
        String message = String.format("%s;%d;%s", eventMethodName, stateCode, paramInfo);
        log(mClass, message, LogLevel.CUSTOM);
    }

    private static void log(Class mClass, String message, LogLevel logLevel) {
        String messageWithTime = String.format("[ %s ][ %s / %s : %s ]", getLogStr(), logLevel.name(), mClass.getSimpleName(), message);
        switch (logLevel) {
            case INFO:
                Log.i(TAG, messageWithTime);
                printLog(logLevel, messageWithTime);
                break;
            case WARN:
                Log.w(TAG, messageWithTime);
                printLog(logLevel, messageWithTime);
                break;
            case DEBUG:
                Log.d(TAG, messageWithTime);
                printLog(logLevel, messageWithTime);
                break;
            case ERROR:
                Log.e(TAG, messageWithTime);
                printLog(logLevel, messageWithTime);
                break;
            case CUSTOM:
                Log.i(TAG, messageWithTime);
                printLog(logLevel, message);
                break;
            default:
                break;
        }
    }
}
