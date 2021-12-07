package com.kuaishou.kwairtcdemo.entity;

public class QualityInfo {
    public int appCPU = 0;
    public int totalCPU = 0;
    public int memory = 0; // Unit: MB
    public int rtt = -1;
    public int width = 0;
    public int height = 0;
    public int fps = 0;
    public int videoSendBitrate = 0;
    public int videoRecvBitrate = 0;
    public int audioSendBitrate = 0;
    public int audioRecvBitrate = 0;
    public int sendLoss = -1;
    public int recvLoss = -1;

    public QualityInfo() {

    }
}
