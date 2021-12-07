package com.kuaishou.kwairtcdemo;

import android.view.View;

import com.kuaishou.kwairtcdemo.entity.MemberInfo;

/**
 * description: 单个视频视图接口类
 * @date 2020/8/24 8:29 PM
 */
public interface IKWVideoView {

    /**
     * 添加承载视频流的视图
     * @return
     */
    void addRenderView(View renderView);

    /**
     * 移除渲染视频的视图
     */
    void removeRenderView();

    /**
     * 修改视图展示的成员信息
     * @param memberInfo
     */
    void changeViewMemberInfo(MemberInfo memberInfo);

    /**
     * 获取渲染视频的视图
     * @return
     */
    View getRenderView();

    /**
     * 设置视图的可见性
     * @param visibility 显示/隐藏
     */
    void setVisibility(int visibility);

    /**
     * 获取当前视图承载的流的从属用户的用户昵称
     * @return
     */
    String getNickName();

    /**
     * 获取当前视图承载的流的从属用户的用户ID
     * @return
     */
    String getUserID();

    /**
     * 摄像头是否已打开
     * @return
     */
    boolean isCameraOpen();

    /**
     * 麦克风是否已打开
     * @return
     */
    boolean isMicOpen();

    /**
     * 视图是否可用
     * @return
     */
    boolean isFree();
}
