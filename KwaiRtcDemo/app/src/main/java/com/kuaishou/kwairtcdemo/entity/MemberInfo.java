package com.kuaishou.kwairtcdemo.entity;

public class MemberInfo {
    public String userID;
    public boolean isMicOpen = true;
    public boolean isCameraOpen = true;

    public MemberInfo(String userID) {
        this.userID = userID;
    }
}
