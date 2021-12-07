package com.kuaishou.kwairtcdemo.log;

public enum LogLevel {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    CUSTOM(4);

    private int mValue;

    LogLevel(int value) {
        this.mValue = value;
    }

    public static LogLevel value(int value) {
        switch (value) {
            case 0:
                return DEBUG;
            case 1:
                return INFO;
            case 2:
                return WARN;
            case 3:
                return ERROR;
            case 4:
                return CUSTOM;
            default:
                return INFO;
        }
    }

    public int value() {
        return mValue;
    }
}