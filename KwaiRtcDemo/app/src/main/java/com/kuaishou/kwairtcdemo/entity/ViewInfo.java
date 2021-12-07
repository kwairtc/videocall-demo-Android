package com.kuaishou.kwairtcdemo.entity;

import android.view.View;

public class ViewInfo {
    public View view;
    public String userID;
    public boolean isLocalView;
    public boolean isFree = true;

    public ViewInfo(View view, boolean isLocalView) {
        this.view = view;
        this.isLocalView = isLocalView;
    }
}
