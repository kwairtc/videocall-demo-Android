package com.kuaishou.kwairtcdemo.log;

public interface IKWLogPrinter {
    /**
     * 打印日志
     *
     * @param logMsg 日志信息
     */
    void printLog(LogLevel logLevel, String logMsg);
}
