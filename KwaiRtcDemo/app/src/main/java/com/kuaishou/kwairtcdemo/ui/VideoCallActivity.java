package com.kuaishou.kwairtcdemo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.databinding.DataBindingUtil;

import com.kuaishou.kwairtcdemo.KWManager;
import com.kuaishou.kwairtcdemo.R;
import com.kuaishou.kwairtcdemo.base.BaseActivity;
import com.kuaishou.kwairtcdemo.base.KWApplication;
import com.kuaishou.kwairtcdemo.constants.KWConfigDataKeeper;
import com.kuaishou.kwairtcdemo.constants.KWConstants;
import com.kuaishou.kwairtcdemo.customview.KWChannelInfoDialog;
import com.kuaishou.kwairtcdemo.customview.KWCustomToastView;
import com.kuaishou.kwairtcdemo.customview.KWMemberDialog;
import com.kuaishou.kwairtcdemo.customview.KWVideoView;
import com.kuaishou.kwairtcdemo.databinding.ActivityVideoCallBinding;
import com.kuaishou.kwairtcdemo.entity.MemberInfo;
import com.kuaishou.kwairtcdemo.entity.QualityInfo;
import com.kuaishou.kwairtcdemo.entity.ViewInfo;
import com.kuaishou.kwairtcdemo.log.AppLogger;
import com.kuaishou.kwairtcdemo.services.ForeGroundService;
import com.kuaishou.kwairtcdemo.utils.PreferenceUtil;
import com.kwai.video.krtc.Arya;
import com.kwai.video.krtc.ChannelSummaryInfo;
import com.kwai.video.krtc.KWAryaStats;
import com.kwai.video.krtc.rtcengine.IRtcEngineEventHandler;
import com.kwai.video.krtc.rtcengine.RtcEngine;
import com.kwai.video.krtc.rtcengine.RtcEngineConfig;
import com.kwai.video.krtc.rtcengine.RtcEngineConstants;
import com.kwai.video.krtc.rtcengine.camera.KCameraCapturerConfiguration;
import com.kwai.video.krtc.rtcengine.camera.KVideoCanvas;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class VideoCallActivity extends BaseActivity implements View.OnTouchListener {

    private ActivityVideoCallBinding binding;
    private String mChannelID;
    private String mUserID;

    private RtcEngine mRtcEngine;
    private RtcEventHandler mEventHandler = null;
    private static final String TAG = "VideoCallActivity";
    // 业务逻辑限制展示8个人通话
    private static final int CALL_NUM = 8;
    // 视图容器list
    private ArrayList<KWVideoView> kwVideoViewList = new ArrayList<>(CALL_NUM);
    // 视图view list
    private ArrayList<ViewInfo> viewInfoList = new ArrayList<>(CALL_NUM);
    // 成员信息，业务逻辑限制8个（加上自己）
    private ArrayList<MemberInfo> memberInfoList = new ArrayList<>(CALL_NUM);

    private boolean isMicOpen;
    private boolean isForeground = true;
    private boolean isOpenBeauty = false;
    private KWMemberDialog mMemberDialog = null;
    private KWChannelInfoDialog mChannelInfoDialog = null;
    private Timer updateTimer = null;
    private Timer chronTimer = null;
    private int chronCnt = 0;
    private GestureDetector mGestureDetector;
    private KWCustomToastView mToastView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initViews() {
        //防止再次启动
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_call);
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        boolean showQualityInfo = PreferenceUtil.getInstance().getBooleanValue(KWConstants.AV_QUALITY_SWITCH, false);
        showQualityDialog(showQualityInfo);

        mUserID = KWConfigDataKeeper.getUserId();
        AppLogger.d(VideoCallActivity.class, "self userID:%s", mUserID);
        mChannelID = KWConfigDataKeeper.getChannelId();
        binding.roomIDTxt.setText(getString(R.string.ks_channel_id_value, mChannelID));

        binding.smallVideoView.setOnClickListener(listener -> {
            // 切换大小视图
            exchangeVideoView();
        });
        binding.videoViewLayout.setOnTouchListener(this);

        binding.roomIDTxt.setOnClickListener(listener -> {
            closeBeautyView();
            if (mChannelInfoDialog == null) {
                mChannelInfoDialog = new KWChannelInfoDialog(VideoCallActivity.this);
            }
            mChannelInfoDialog.show();
        });

        mToastView = new KWCustomToastView(this);

        // 音视频通话时，启动前台服务用于保活
        keepSdkAliveWhenBackground();
    }

    @Override
    protected void initData() {
        super.initData();
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            return;
        }
        initSDK();
        mRtcEngine = KWManager.getInstance().getRtcEngine();

        // 获取持久化的设备初始状态
        isMicOpen = PreferenceUtil.getInstance().getBooleanValue(KWConstants.MIC_STATE, true);
        KWManager.getInstance().setCameraOpen(PreferenceUtil.getInstance().getBooleanValue(KWConstants.CAMERA_STATE, true));
        AppLogger.d(VideoCallActivity.class, "self isMicOpen:%b, isCameraOpen:%b", isMicOpen, KWManager.getInstance().isCameraOpen());

        // 添加自己的成员信息
        MemberInfo selfInfo = new MemberInfo(mUserID);
        selfInfo.isMicOpen = isMicOpen;
        selfInfo.isCameraOpen = KWManager.getInstance().isCameraOpen();
        addMemberInfo(selfInfo);

        showMemberCount();

        // 注册 SDK 事件监听
        mEventHandler = new RtcEventHandler();
        mRtcEngine.addHandler(mEventHandler);
        // 创建视频的渲染视图
        setupAllVideoView();

        // 设置麦克风的初始状态
        openMic(isMicOpen);
        // 进入房间前用最高分辨率初始化，用于进入房间后升高分辨率
        KWManager.getInstance().setVideoEncodeParam(mChannelID, KWConstants.MAX_WIDTH, KWConstants.MAX_HEIGHT,
                KWConstants.INIT_BITRATE, KWConstants.DEFAULT_FPS, KWConstants.MIN_BITRATE, KWConstants.MAX_BITRATE);
        // 启动本地视频预览
        startPreview();
        mRtcEngine.setCameraCaptureConfiguration(new KCameraCapturerConfiguration());
        // 加入房间，发布音视频
        joinChannel();
        // 设置默认美颜
        KWManager.getInstance().setupBeautyOptions(KWConstants.DEFAULT_BEAUTY_LIGHTENING, KWConstants.DEFAULT_BEAUTY_SMOOTHNESS);

        KWManager.getInstance().setOnAVSettingChangedListener(avSettingChangedListener);

        updateTimer = new Timer();
        updateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    // 更新音视频质量信息
                    updateQualityInfo();
                });
            }
        }, 0, 3000);

        chronTimer = new Timer();
        mGestureDetector = new GestureDetector(this, gestureListener);
    }

    @Override
    protected void notifyNetworkChange(boolean isNetworkConnected) {
        super.notifyNetworkChange(isNetworkConnected);
        if (isNetworkConnected) {
            binding.netErrorHint.setVisibility(View.INVISIBLE);
        } else {
            binding.netErrorHint.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onAppStateCallback(boolean isForeGround) {
        super.onAppStateCallback(isForeGround);
        AppLogger.d(VideoCallActivity.class, "***** onAppState:%b", isForeGround);
        if (this.mRtcEngine == null) {
            return;
        }
        this.isForeground = isForeGround;
        if (isForeGround) {
            if (KWManager.getInstance().isCameraOpen()) {
                this.mRtcEngine.onForeground();
                openCamera(true);
                changeCameraImage(true);
            }
        } else {
            openCamera(false);
            this.mRtcEngine.onBackground();
        }
    }

    @Override
    public void finish() {
        super.finish();
        AppLogger.d(VideoCallActivity.class, "page finish");

        if (mMemberDialog != null) {
            mMemberDialog.clearMembers();
            mMemberDialog.dismiss();
            mMemberDialog = null;
        }
        if (mChannelInfoDialog != null) {
            mChannelInfoDialog.dismiss();
            mChannelInfoDialog = null;
        }
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
        if (chronTimer != null) {
            chronTimer.cancel();
            chronTimer = null;
        }
        if (mRtcEngine != null) {
            mRtcEngine.stopPreview();
            // 离开房间
            leaveChannel();
            // 移除 SDK 事件监听
            mRtcEngine.removeHandler(mEventHandler);
        }
        if (mToastView != null) {
            mToastView.hideView();
            mToastView = null;
        }

        memberInfoList.clear();
        viewInfoList.clear();
        kwVideoViewList.clear();

        // 销毁 SDK 实例
        KWManager.getInstance().destoryEngine();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopKeepLive();
    }

    /**
     * 供其他Activity调用，进入当前Activity查看版本
     *
     * @param activity
     */
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, VideoCallActivity.class);
        activity.startActivity(intent);
    }

    // 离开房间
    public void onClickLeave(View view) {
        finish();
    }

    // 切换摄像头
    public void onClickSwitchCamera(View view) {
        closeBeautyView();
        mRtcEngine.switchCamera();
    }

    // 麦克风操作
    public void onClickMic(View view) {
        closeBeautyView();
        isMicOpen = !isMicOpen;
        openMic(isMicOpen);

        // 修改自己的成员信息
        MemberInfo memberInfo = getMemberInfo(mUserID);
        memberInfo.isMicOpen = isMicOpen;
        if (mMemberDialog != null) {
            mMemberDialog.modifyMember(memberInfo);
        }
        // 修改渲染视图上的占位图
        if (memberInfoList.size() > 2) {
            kwVideoViewList.get(0).changeViewMemberInfo(memberInfo);
        } else {
            if (binding.bigVideoView.getUserID().equals(mUserID)) {
                binding.bigVideoView.changeViewMemberInfo(memberInfo);
            } else {
                binding.smallVideoView.changeViewMemberInfo(memberInfo);
            }
        }
    }

    // 摄像头操作
    public void onClickCamera(View view) {
        closeBeautyView();
        boolean oldCameraState = KWManager.getInstance().isCameraOpen();
        KWManager.getInstance().setCameraOpen(!oldCameraState);
        openCamera(!oldCameraState);
        changeCameraImage(!oldCameraState);

        // 修改自己的成员信息
        MemberInfo memberInfo = getMemberInfo(mUserID);
        memberInfo.isCameraOpen = !oldCameraState;
        if (mMemberDialog != null) {
            mMemberDialog.modifyMember(memberInfo);
        }
        // 修改渲染视图上的占位图
        if (memberInfoList.size() > 2) {
            kwVideoViewList.get(0).changeViewMemberInfo(memberInfo);
        } else {
            if (binding.bigVideoView.getUserID().equals(mUserID)) {
                binding.bigVideoView.changeViewMemberInfo(memberInfo);
            } else {
                binding.smallVideoView.changeViewMemberInfo(memberInfo);
            }
        }
    }

    // 成员列表操作
    public void onClickMember(View view) {
        closeBeautyView();
        if (mMemberDialog == null) {
            mMemberDialog = new KWMemberDialog(VideoCallActivity.this);

            mMemberDialog.setOnDismissListener(dialog -> {
                if (mMemberDialog != null) {
                    mMemberDialog.dismiss();
                }
            });

            mMemberDialog.clearMembers();
            if (memberInfoList.size() > 0) {
                mMemberDialog.addMemberList(memberInfoList);
            }
        }
        mMemberDialog.show();
    }

    // 美颜操作
    public void onClickBeauty(View view) {
        isOpenBeauty = !isOpenBeauty;
        if (!isOpenBeauty) {
            binding.beautyView.setVisibility(View.GONE);
        } else if (KWManager.getInstance().isCameraOpen() && isOpenBeauty) {
            binding.beautyView.setVisibility(View.VISIBLE);
        } else if (!KWManager.getInstance().isCameraOpen() && isOpenBeauty){
            mToastView.showView(getString(R.string.ks_cannot_set_beauty), 3000);
        }
    }

    // 设置
    public void onClickSetting(View view) {
        closeBeautyView();
        SettingActivity.actionStart(VideoCallActivity.this);
    }

    /**
     * 确保sdk在切换到后台后仍然可能正常推、拉流
     * <p>
     * 需要android.permission.FOREGROUND_SERVICE
     */
    protected void keepSdkAliveWhenBackground() {
        // TODO: 仅发送了一个通知，需要添加点击通知可以回到音视频页面功能
        Intent startIntent = new Intent(this, ForeGroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(startIntent);
        } else {
            startService(startIntent);
        }
    }

    /**
     * 取消用于保活的前台服务
     */
    protected void stopKeepLive() {
        Intent foregroundService = new Intent(this, ForeGroundService.class);
        stopService(foregroundService);
    }

    private void showQualityDialog(boolean isShow) {
        binding.qualityInfoView.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    private void showMemberCount() {
        binding.memberCount.setText(String.valueOf(memberInfoList.size()));
    }

    private void setupAllVideoView() {
        // 业务逻辑限制8个人通话，视频渲染视图的装载容器
        kwVideoViewList.add(binding.videoView1);
        kwVideoViewList.add(binding.videoView2);
        kwVideoViewList.add(binding.videoView3);
        kwVideoViewList.add(binding.videoView4);
        kwVideoViewList.add(binding.videoView5);
        kwVideoViewList.add(binding.videoView6);
        kwVideoViewList.add(binding.videoView7);
        kwVideoViewList.add(binding.videoView8);

        // 从 SDK 获取渲染 view
        View localView = RtcEngine.createLocalTextureView(this); // 本地预览 view
        viewInfoList.add(new ViewInfo(localView, true));
        for (int i = 0; i < (CALL_NUM - 1); i++) {
            View remoteView = RtcEngine.createRemoteTextureView(this);
            viewInfoList.add(new ViewInfo(remoteView, false));
        }
    }

    private void initSDK() {
        // 创建 SDK 实例
        RtcEngineConfig config = new RtcEngineConfig();
        config.mContext = KWApplication.application;
        config.mAppId = KWConstants.APPID;
        config.mAppVersion = getDemoVersion();
        config.mUserId = KWConfigDataKeeper.getUserId();
        config.mAppName = KWConstants.APPNAME;

        KWManager.getInstance().createEngine(config);
    }

    // 进房时设置自己的预览视图并启动预览
    private void startPreview() {
        KWVideoView videoView;

        if (memberInfoList.size() == 1) {
           // 房间只有自己
            videoView = binding.bigVideoView;

        } else if (memberInfoList.size() == 2) {
            // 1v1
            videoView = binding.smallVideoView;
            binding.smallVideoView.setVisibility(View.VISIBLE);
        } else {
            // 使用网格视图
            binding.bigVideoView.setVisibility(View.GONE);
            binding.smallVideoView.setVisibility(View.GONE);
            binding.firstGroup.setVisibility(View.VISIBLE);
            // 网格第一个视图显示自己
            videoView = kwVideoViewList.get(0);
        }
        ViewInfo localViewInfo = viewInfoList.get(0);
        localViewInfo.isFree = false;
        localViewInfo.userID = mUserID;
        videoView.removeRenderView();
        videoView.addRenderView(localViewInfo.view);
        videoView.changeViewMemberInfo(memberInfoList.get(0));
        // SDK 绑定渲染视图
        mRtcEngine.bindLocalVideoView(new KVideoCanvas(localViewInfo.view, RtcEngineConstants.RenderMode.kScaleToFitWithCropping,
                mUserID, mChannelID, RtcEngineConstants.VideoSourceType.kVideoSourceTypePeople));
        mRtcEngine.enableLocalVideo();
        mRtcEngine.startPreview();
    }

    private void joinChannel() {
        mRtcEngine.setDefaultUnmuteAllRemoteVideoStreams();

        RtcEngine.JoinChannelParam channelParam = new RtcEngine.JoinChannelParam();
        channelParam.token = KWConstants.TOKEN;
        channelParam.channelId = mChannelID;
        channelParam.channelProfile = Arya.KWAryaChannelProfileRTC;

        int res = mRtcEngine.joinChannel(channelParam);
        AppLogger.d(VideoCallActivity.class,"joinChannel, res:%d", res);
    }

    private void leaveChannel() {
        AppLogger.d(VideoCallActivity.class, "leaveChannel:%s", mChannelID);
        mRtcEngine.leaveChannel(mChannelID);
    }

    private void addRemoteVideoView(String channelID, String userID, int sourceType) {
        runOnUiThread(() -> {
           showMemberCount();
            if (memberInfoList.size() <= 2) {
                // 1v1
                if (binding.bigVideoView.getUserID().equals(mUserID)) {
                    // 自己的预览从大画面切换到小画面
                    binding.bigVideoView.removeRenderView();
                    binding.smallVideoView.setVisibility(View.VISIBLE);
                    ViewInfo viewInfo = getViewInfo(mUserID);
                    viewInfo.isFree = false;
                    viewInfo.userID = mUserID;
                    binding.smallVideoView.addRenderView(viewInfo.view);
                    binding.smallVideoView.changeViewMemberInfo(memberInfoList.get(0));
                }
                // 远端用户上大画面
                ViewInfo remoteViewInfo = null;
                for (ViewInfo viewInfo : viewInfoList) {
                    if (viewInfo.isFree) {
                        remoteViewInfo = viewInfo;
                        viewInfo.isFree = false;
                        viewInfo.userID = userID;
                        break;
                    }
                }
                if (remoteViewInfo != null) {
                    binding.bigVideoView.addRenderView(remoteViewInfo.view);
                    mRtcEngine.bindRemoteVideoView(new KVideoCanvas(remoteViewInfo.view, RtcEngineConstants.RenderMode.kScaleToFitWithCropping,
                            userID, channelID, sourceType));
                    MemberInfo remoteMemberInfo = getMemberInfo(userID);
                    if (remoteMemberInfo != null) {
                        binding.bigVideoView.changeViewMemberInfo(remoteMemberInfo);
                        AppLogger.d(VideoCallActivity.class, "addRemoteVideoView--add remote user isCameraOpen:%b, isMicOpen:%b" ,
                                remoteMemberInfo.isCameraOpen, remoteMemberInfo.isMicOpen);
                    }
                }
            } else {
                // 所有用户展示到网格视图上
                if (!binding.bigVideoView.isFree()) { // 第三个人加进来，先调整已有两个人的视图
                    // 需要将大小画面的视图移到网格视图上展示
                    String remoteUserID;

                    if (binding.bigVideoView.getUserID().equals(mUserID)) {
                        remoteUserID = binding.smallVideoView.getUserID();
                    } else {
                        remoteUserID = binding.bigVideoView.getUserID();
                    }
                    // 更换渲染视图的装载container
                    binding.bigVideoView.removeRenderView();
                    binding.bigVideoView.setVisibility(View.GONE);
                    binding.smallVideoView.removeRenderView();
                    binding.smallVideoView.setVisibility(View.GONE);
                    binding.firstGroup.setVisibility(View.VISIBLE);

                    // 自己的视图替换到网格的第一个
                    KWVideoView selfVideoView = kwVideoViewList.get(0);
                    ViewInfo selfViewInfo = viewInfoList.get(0);
                    selfVideoView.addRenderView(selfViewInfo.view);
                    selfVideoView.changeViewMemberInfo(memberInfoList.get(0));

                    // 远端视图
                    KWVideoView remoteVideoView = kwVideoViewList.get(1);
                    ViewInfo remoteViewInfo = getViewInfo(remoteUserID);
                    if (remoteViewInfo != null) {
                        remoteVideoView.addRenderView(remoteViewInfo.view);
                    }
                    MemberInfo remoteMemberInfo = getMemberInfo(remoteUserID);
                    if (remoteMemberInfo != null) {
                        remoteVideoView.changeViewMemberInfo(remoteMemberInfo);
                    }
                }
                KWVideoView remoteVideoView = null;
                View remoteView = null;
                // 循环找free的视图
                for (KWVideoView videoView : kwVideoViewList) {
                    if (videoView.isFree()) {
                        remoteVideoView = videoView;
                        break;
                    }
                }
                for (ViewInfo viewInfo : viewInfoList) {
                    if (viewInfo.isFree) {
                        remoteView = viewInfo.view;
                        viewInfo.isFree = false;
                        viewInfo.userID = userID;
                        break;
                    }
                }
                if (remoteView != null && remoteVideoView != null) {
                    remoteVideoView.addRenderView(remoteView);
                    MemberInfo remoteMemberInfo = getMemberInfo(userID);
                    if (remoteMemberInfo != null) {
                        remoteVideoView.changeViewMemberInfo(remoteMemberInfo);
                    }
                    mRtcEngine.bindRemoteVideoView(new KVideoCanvas(remoteView, RtcEngineConstants.RenderMode.kScaleToFitWithCropping,
                            userID, channelID, sourceType));
                }
                if (memberInfoList.size() > 4) {
                    binding.pageCtrl.setVisibility(View.VISIBLE);
                    binding.firstPage.setImageResource(R.drawable.ic_pagectrl_cur);
                    binding.secondPage.setImageResource(R.drawable.ic_pagectrl_hold);
                }
            }
        });
    }

    private void removeRemoteVideoView(String channelID, String userID, int sourceType) {
        runOnUiThread(() -> {
            showMemberCount();
            if (memberInfoList.size() <= 4) {
                binding.pageCtrl.setVisibility(View.INVISIBLE);
            }
            AppLogger.d(VideoCallActivity.class, "removeRemoteVideoView, userID:%s, memberSize:%d", userID, memberInfoList.size());
            // 移除视图
            ViewInfo viewInfo = getViewInfo(userID);
            if (viewInfo != null) {
                viewInfo.isFree = true;
                // 此示例 demo 中一个用户只会推一条视频流，所以直接 unBindAllRemoteVideoViews
                mRtcEngine.unBindAllRemoteVideoViews(channelID, userID, sourceType);
                AppLogger.d(VideoCallActivity.class, "unBindAllRemoteVideoViews, userID:%s", userID);
            }
            if (memberInfoList.size() == 2) { // 除开离开房间这个用户后的房间成员人数
                for (KWVideoView tmp : kwVideoViewList) {
                    if (tmp.getUserID().equals(userID)) {
                        tmp.removeRenderView();
                    }
                }
            }

            // 调整视图布局
            if (memberInfoList.size() == 1) {
                // 房间里只剩下一个人，自己恢复为大画面
                String tmpUserID = binding.smallVideoView.getUserID();
                binding.smallVideoView.removeRenderView();
                binding.smallVideoView.setVisibility(View.GONE);
                if (tmpUserID.equals(mUserID)) {
                    // 将自己切换到大画面
                    binding.bigVideoView.removeRenderView();
                    ViewInfo selfViewInfo = viewInfoList.get(0);
                    selfViewInfo.isFree = false;
                    selfViewInfo.userID = mUserID;
                    binding.bigVideoView.addRenderView(selfViewInfo.view);
                    MemberInfo selfInfo = memberInfoList.get(0);
                    binding.bigVideoView.changeViewMemberInfo(selfInfo);
                    AppLogger.d(VideoCallActivity.class, "removeRemoteVideoView--user leave self switch to big view, isCameraOpen:%b", selfInfo.isCameraOpen);
                }
            } else if (memberInfoList.size() == 2) {
                // switch to 1v1
                // 处理自己的视图
                KWVideoView selfVideoView = kwVideoViewList.get(0);
                selfVideoView.removeRenderView();
                binding.firstGroup.setVisibility(View.GONE);
                binding.smallVideoView.setVisibility(View.VISIBLE);
                binding.smallVideoView.addRenderView(viewInfoList.get(0).view);
                binding.smallVideoView.changeViewMemberInfo(memberInfoList.get(0));

                AppLogger.d(VideoCallActivity.class, "removeRemoteVideoView switch 1v1, userID:%s", userID);

                for (KWVideoView tmp : kwVideoViewList) {
                    if (!tmp.isFree()) {
                        String remoteUserID = tmp.getUserID();
                        tmp.removeRenderView();
                        binding.bigVideoView.setVisibility(View.VISIBLE);
                        ViewInfo remoteViewInfo = getViewInfo(remoteUserID);
                        if (remoteViewInfo != null) {
                            binding.bigVideoView.addRenderView(remoteViewInfo.view);
                            MemberInfo memberInfo = getMemberInfo(remoteUserID);
                            if (memberInfo != null) {
                                binding.bigVideoView.changeViewMemberInfo(memberInfo);
                            }
                        }
                        break;
                    }
                }
            } else {
                // 网格视图调整
                KWVideoView videoView = getVideoView(userID);
                int videoViewIndex = 0;
                if (videoView != null) {
                    videoViewIndex = kwVideoViewList.indexOf(videoView);
                    videoView.removeRenderView();
                    AppLogger.d(VideoCallActivity.class, "removeRenderView from kwVideoViewList, userID:%s, viewIndex:%d", userID, videoViewIndex);
                }

                // 调整视图位置，当前视图索引后的所有视图往前移动
                if (videoViewIndex > 0) {
                    for (int i = videoViewIndex; i < kwVideoViewList.size() - 1; i++) {
                        KWVideoView destVideoView = kwVideoViewList.get(i);
                        KWVideoView srcVideoView = kwVideoViewList.get(i + 1);
                        String srcUserID = srcVideoView.getUserID();
                        MemberInfo srcMemberInfo = getMemberInfo(srcUserID);
                        AppLogger.d(VideoCallActivity.class, "move videoview src userid:%s", srcUserID);
                        if (srcMemberInfo != null) {
                            destVideoView.changeViewMemberInfo(srcMemberInfo);
                        }
                        ViewInfo srcViewInfo = getViewInfo(srcUserID);
                        srcVideoView.removeRenderView();
                        if (srcViewInfo != null) {
                            destVideoView.addRenderView(srcViewInfo.view);
                        }
                    }
                    if (memberInfoList.size() <= 4) {
                        binding.firstGroup.setVisibility(View.VISIBLE);
                        binding.secondGroup.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void addMemberInfo(MemberInfo memberInfo) {
        if (memberInfoList.size() > 0) {
            for (MemberInfo tmp : memberInfoList) {
                if (tmp.userID.equals(memberInfo.userID)) {
                    return;
                }
            }
        }
        memberInfoList.add(memberInfo);
    }

    private MemberInfo getMemberInfo(String userID) {
        for (MemberInfo memberInfo : memberInfoList) {
            if (memberInfo.userID.equals(userID)) {
                AppLogger.d(VideoCallActivity.class, "getMemberInfo find return, userid:%s", memberInfo.userID);
                return memberInfo;
            }
        }
        return null;
    }

    private KWVideoView getVideoView(String userID) {
        for (KWVideoView videoView : kwVideoViewList) {
            if (videoView.getUserID().equals(userID)) {
                return videoView;
            }
        }
        return null;
    }

    private ViewInfo getViewInfo(String userID) {
        for (ViewInfo viewInfo : viewInfoList) {
            if (!TextUtils.isEmpty(viewInfo.userID) && viewInfo.userID.equals(userID)) {
                return viewInfo;
            }
        }
        return null;
    }

    private void updateQualityInfo() {
        QualityInfo qualityInfo = KWManager.getInstance().getAVQualityInfo();
        if (qualityInfo == null) {
            return;
        }
        binding.qualityInfoView.updateQosValue(qualityInfo);
    }

    private void openMic(boolean isOpen) {
        if (isOpen) {
            mRtcEngine.unmuteLocalAudioStream();
            // 修改工具栏图标
            binding.openMicImg.setImageResource(R.drawable.ic_mic_open);
        } else {
            mRtcEngine.muteLocalAudioStream();
            // 修改工具栏图标
            binding.openMicImg.setImageResource(R.drawable.ic_mic_close);
        }
    }

    private void openCamera(boolean isOpen) {
        if (isOpen) {
            mRtcEngine.unmuteLocalVideoStream(mChannelID);
            mRtcEngine.startPreview();
        } else {
            mRtcEngine.muteLocalVideoStream(mChannelID);
            mRtcEngine.stopPreview();
        }
    }

    private void changeCameraImage(boolean isOpen) {
        if (isOpen) {
            // 修改工具栏图标
            binding.openCameraImg.setImageResource(R.drawable.ic_camera_open);
        } else {
            // 修改工具栏图标
            binding.openCameraImg.setImageResource(R.drawable.ic_camera_close);
        }
    }

    private void closeBeautyView() {
        if (binding.beautyView.getVisibility() == View.VISIBLE) {
            binding.beautyView.setVisibility(View.GONE);
            isOpenBeauty = false;
        }
    }

    /**
     * 交换大小视图
     */
    private void exchangeVideoView() {
        ViewInfo viewInfo = viewInfoList.get(0); // 自己的视图

        KWVideoView selfVideoView;
        KWVideoView remoteVideoView;
        String remoteUserID;

        if (binding.bigVideoView.getUserID().equals(mUserID)) {
            // 自己的预览从大画面切换到小画面
            remoteUserID = binding.smallVideoView.getUserID();
            selfVideoView = binding.smallVideoView;
            remoteVideoView = binding.bigVideoView;
        } else {
            // 自己的预览从小画面切换到大画面
            remoteUserID = binding.bigVideoView.getUserID();
            selfVideoView = binding.bigVideoView;
            remoteVideoView = binding.smallVideoView;
        }
        binding.bigVideoView.removeRenderView();
        binding.smallVideoView.removeRenderView();

        selfVideoView.addRenderView(viewInfo.view);
        selfVideoView.changeViewMemberInfo(memberInfoList.get(0));

        ViewInfo remoteViewInfo = getViewInfo(remoteUserID);
        if (remoteViewInfo != null) {
            remoteVideoView.addRenderView(remoteViewInfo.view);
        }
        MemberInfo memberInfo = getMemberInfo(remoteUserID);
        if (memberInfo != null) {
            remoteVideoView.changeViewMemberInfo(memberInfo);
        }
    }

    private String getStringTime(int cnt) {
        int hour = cnt / 3600;
        int min = cnt % 3600 / 60;
        int second = cnt % 60;
        return String.format(Locale.CHINA, "%02d:%02d:%02d", hour, min, second);
    }

    private KWManager.OnAVSettingChangedListener avSettingChangedListener = new KWManager.OnAVSettingChangedListener() {
        @Override
        public void onAVQualitySwitchChanged(boolean isOpen) {
            showQualityDialog(isOpen);
        }

        @Override
        public void onVideoEncodeParamChanged(int width, int height, int bitrate, int fps, int minBitrate, int maxBitrate) {
            KWManager.getInstance().setVideoEncodeParam(mChannelID, width, height, bitrate, fps, minBitrate, maxBitrate);
        }
    };

    private GestureDetector.OnGestureListener gestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            closeBeautyView();
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            AppLogger.d(VideoCallActivity.class, "onFling member size:%d", memberInfoList.size());
            if (memberInfoList.size() <= 4) {
                return true;
            }
            if (e1.getX() - e2.getX() > 50 && Math.abs(velocityX) > 0) {
                // 左滑显示视频下一页
                binding.firstGroup.setVisibility(View.GONE);
                binding.secondGroup.setVisibility(View.VISIBLE);
                binding.secondPage.setImageResource(R.drawable.ic_pagectrl_cur);
                binding.firstPage.setImageResource(R.drawable.ic_pagectrl_hold);

            } else if (e2.getX() - e1.getX() > 50 && Math.abs(velocityX) > 0) {
                // 右滑显示视频上一页
                binding.secondGroup.setVisibility(View.GONE);
                binding.firstGroup.setVisibility(View.VISIBLE);
                binding.secondPage.setImageResource(R.drawable.ic_pagectrl_hold);
                binding.firstPage.setImageResource(R.drawable.ic_pagectrl_cur);
            }
            return true;
        }
    };

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return mGestureDetector.onTouchEvent(motionEvent);
    }

    private class RtcEventHandler extends IRtcEngineEventHandler {
        @Override
        public void onActiveSpeaker(ArrayList<String> speakerUids) {
            super.onActiveSpeaker(speakerUids);
        }

        @Override
        public void onFirstLocalAudioFramePublished(int elapsed) {
            super.onFirstLocalAudioFramePublished(elapsed);
            AppLogger.d(VideoCallActivity.class, "onFirstLocalAudioFramePublished: %d", elapsed);
        }

        @Override
        public void onFirstLocalVideoFrame(int width, int height, int elapsed) {
            super.onFirstLocalVideoFrame(width, height, elapsed);
            AppLogger.d(VideoCallActivity.class,"onFirstLocalVideoFrame, width:%d, height:%d, elapse:%d", width, height, elapsed);
        }

        @Override
        public void onFirstLocalVideoFramePublished(int elapsed) {
            super.onFirstLocalVideoFramePublished(elapsed);
            AppLogger.d(VideoCallActivity.class,"onFirstLocalVideoFramePublished: %d", elapsed);
        }

        @Override
        public void onFirstRemoteVideoFrame(String channelId, String userId, int width, int height, int elapsed) {
            super.onFirstRemoteVideoFrame(channelId, userId, width, height, elapsed);
            AppLogger.d(VideoCallActivity.class,"onFirstRemoteVideoFrame channelId:%s, userId:%s, width:%d, height:%d, elapsed:%d", channelId, userId, width, height, elapsed);
        }

        @Override
        public void onWarning(String channelId, int warning) {
            super.onWarning(channelId, warning);
            AppLogger.w(VideoCallActivity.class,"onWarning: channelID:%s, warning:%d, describe:%s ", channelId, warning, RtcEngine.getErrorDescription(warning));
        }

        @Override
        public void onError(String channelId, int error) {
            super.onError(channelId, error);
            AppLogger.e(VideoCallActivity.class,"onError: channelID:%s, err:%d, describe:%s", channelId, error, RtcEngine.getErrorDescription(error));
        }

        @Override
        public void onJoinChannelSuccess(String channelId, String userId, int elapsed) {
            super.onJoinChannelSuccess(channelId, userId, elapsed);
            AppLogger.d(VideoCallActivity.class,"onJoinChannelSuccess channelId:%s, userId:%s, elapsed:%d" , channelId, userId, elapsed);
            runOnUiThread(() -> {
                openCamera(KWManager.getInstance().isCameraOpen());
                changeCameraImage(KWManager.getInstance().isCameraOpen());
            });
            // 设置默认视频参数
            KWManager.getInstance().setVideoEncodeParam(mChannelID,
                    PreferenceUtil.getInstance().getIntValue(KWConstants.RESOLUTION_WIDTH, KWConstants.DEFAULT_WIDTH),
                    PreferenceUtil.getInstance().getIntValue(KWConstants.RESOLUTION_HEIGHT, KWConstants.DEFAULT_HEIGHT),
                    PreferenceUtil.getInstance().getIntValue(KWConstants.VIDEO_BITRATE, KWConstants.DEFAULT_MAX_BITRATE),
                    PreferenceUtil.getInstance().getIntValue(KWConstants.VIDEO_FPS, KWConstants.DEFAULT_FPS),
                    PreferenceUtil.getInstance().getIntValue(KWConstants.VIDEO_MIN_BITRATE, KWConstants.DEFAULT_MIN_BITRATE),
                    PreferenceUtil.getInstance().getIntValue(KWConstants.VIDEO_MAX_BITRATE, KWConstants.DEFAULT_MAX_BITRATE));

            if (chronTimer == null) {
                return;
            }
            chronTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.timeTxt.setText(getStringTime(chronCnt++));
                        }
                    });
                }
            }, 0, 1000);
        }

        @Override
        public void onLeaveChannel(ChannelSummaryInfo stats) {
            super.onLeaveChannel(stats);
            AppLogger.d(VideoCallActivity.class,"onLeaveChannel: %s", stats.channelId);
        }

        @Override
        public void onRejoinChannelSuccess(String channelId, String userId, int elapsed) {
            super.onRejoinChannelSuccess(channelId, userId, elapsed);
            AppLogger.d(VideoCallActivity.class,"onRejoinChannelSuccess channelId:%s, userId:%s, elapsed:%d", channelId, userId, elapsed);
        }

        @Override
        public void onRemoteSubscribeFallbackToAudioOnly(String channelId, String userId, boolean isFallbackOrRecover) {
            super.onRemoteSubscribeFallbackToAudioOnly(channelId, userId, isFallbackOrRecover);
            AppLogger.d(VideoCallActivity.class,"onRemoteSubscribeFallbackToAudioOnly channelId:%s, userId:%s, isFallbackOrRecover:%b", channelId, userId, isFallbackOrRecover);
        }

        @Override
        public void onLocalPublishFallbackToAudioOnly(String channelId, boolean isFallbackOrRecover) {
            super.onLocalPublishFallbackToAudioOnly(channelId, isFallbackOrRecover);
            AppLogger.d(VideoCallActivity.class,"onLocalPublishFallbackToAudioOnly channelId:%s, isFallbackOrRecover:%b", channelId, isFallbackOrRecover);
        }

        @Override
        public void onUserJoined(String channelId, String userId, int elapsed) {
            super.onUserJoined(channelId, userId, elapsed);
            AppLogger.d(VideoCallActivity.class,"11 onUserJoined, channelID:%s, userId:%s, elapsed:%d, memberSize:%d",
                    channelId, userId, elapsed, memberInfoList.size());
            if (memberInfoList.size() >= 8) { // 业务逻辑限制
                return;
            }
            runOnUiThread(() -> {
                MemberInfo memberInfo = new MemberInfo(userId);
                addMemberInfo(memberInfo);
                if (mMemberDialog != null) {
                    mMemberDialog.addMember(memberInfo);
                }
                addRemoteVideoView(channelId, userId, RtcEngineConstants.VideoSourceType.kVideoSourceTypePeople);
                AppLogger.d(VideoCallActivity.class, "onUserJoined, channelID:%s, userId:%s, elapsed:%d, memberSize:%d",
                        channelId, userId, elapsed, memberInfoList.size());
                if (isForeground) {
                    openCamera(KWManager.getInstance().isCameraOpen());
                } else {
                    // 位于后台
                    openCamera(false);
                }
                showMemberCount();
            });
        }

        @Override
        public void onUserOffline(String channelId, String userId, int reason) {
            super.onUserOffline(channelId, userId, reason);
            runOnUiThread(() -> {
                MemberInfo memberInfo = getMemberInfo(userId);
                if (memberInfo != null) {
                    memberInfoList.remove(memberInfo);
                    AppLogger.d(VideoCallActivity.class, "**** onUserOffline remove member:%s", memberInfo.userID);
                    if (mMemberDialog != null) {
                        mMemberDialog.deleteMember(memberInfo);
                    }
                }
                removeRemoteVideoView(channelId, userId, RtcEngineConstants.VideoSourceType.kVideoSourceTypePeople);
                AppLogger.d(VideoCallActivity.class,"onUserOffline channelID:%s, userId:%s, reason:%d, membersize:%d",
                        channelId, userId, reason, memberInfoList.size());
            });
        }

        @Override
        public void onClientRoleChanged(String channelId, @RtcEngineConstants.ClientRoleType int oldRole, @RtcEngineConstants.ClientRoleType int newRole) {
            super.onClientRoleChanged(channelId, oldRole, newRole);
            Log.d(TAG,"onClientRoleChanged channelId " + channelId + "oldRole " + oldRole + " newRole " + newRole);
        }

        @Override
        public void onReceiveStreamMessage(String channelId, String userId, int streamId, byte[] data) {
            super.onReceiveStreamMessage(channelId, userId, streamId, data);
            String msg = new String(data);
            Log.d(TAG,"onReceiveStreamMessage " + channelId + " " + userId + " " + streamId + " " + msg);
        }

        @Override
        public void onRtcStats(KWAryaStats.KWAryaRtcStats stats) {
            super.onRtcStats(stats);
            QualityInfo qualityInfo = KWManager.getInstance().getAVQualityInfo();
            if (qualityInfo == null) {
                return;
            }
            qualityInfo.appCPU = stats.cpuAppUsage;
            qualityInfo.totalCPU = stats.cpuTotalUsage;
            qualityInfo.audioSendBitrate = stats.txAudioKBitrate;
            qualityInfo.audioRecvBitrate = stats.rxAudioKBitrate;
            qualityInfo.videoSendBitrate = stats.txVideoKBitrate;
            qualityInfo.videoRecvBitrate = stats.rxVideoKBitrate;
            qualityInfo.sendLoss = stats.txPacketLossRate;
            qualityInfo.recvLoss = stats.rxPacketLossRate;
            qualityInfo.rtt = stats.rtt;
            qualityInfo.memory = stats.memoryAppUsageInKbytes / 1024;
        }

        @Override
        public void onLocalVideoStats(KWAryaStats.KWAryaLocalVideoStats stats) {
            super.onLocalVideoStats(stats);
            QualityInfo qualityInfo = KWManager.getInstance().getAVQualityInfo();
            if (qualityInfo == null) {
                return;
            }
            qualityInfo.width = stats.encodedFrameWidth;
            qualityInfo.height = stats.encodedFrameHeight;
            qualityInfo.fps = stats.targetFrameRate;
        }

        @Override
        public void onRemoteVideoStats(KWAryaStats.KWAryaRemoteVideoStats stats) {
            super.onRemoteVideoStats(stats);
        }

        @Override
        public void onLocalAudioStats(KWAryaStats.KWAryaLocalAudioStats stats) {
            super.onLocalAudioStats(stats);
        }

        @Override
        public void onRemoteAudioStats(KWAryaStats.KWAryaRemoteAudioStats stats) {
            super.onRemoteAudioStats(stats);
        }

        @Override
        public void onNetworkQuality(String channelId, long userId, @RtcEngineConstants.NetworkQuality int txQuality, @RtcEngineConstants.NetworkQuality int rxQuality) {
            super.onNetworkQuality(channelId, userId, txQuality, rxQuality);
        }

        @Override
        public void onAudioPublishStateChanged(String channelId, int oldStat, int newStat, int elapseSinceLastStat) {
            super.onAudioPublishStateChanged(channelId, oldStat, newStat, elapseSinceLastStat);
            Log.d(TAG,"onAudioPublishStateChanged ch " + channelId + " audioStats " + oldStat + " -> " + newStat + " " + elapseSinceLastStat);
        }

        @Override
        public void onVideoPublishStateChanged(String channelId, int oldStat, int newStat, int elapseSinceLastStat) {
            super.onVideoPublishStateChanged(channelId, oldStat, newStat, elapseSinceLastStat);
            Log.d(TAG,"onVideoPublishStateChanged ch " + channelId + " videoStats " + oldStat + " -> " + newStat + " " + elapseSinceLastStat);
        }

        @Override
        public void onQosInfo(String info) {
            super.onQosInfo(info);
        }

        @Override
        public void onLocalAudioStateChanged(int state, int error) {
            super.onLocalAudioStateChanged(state, error);
            Log.d(TAG,"onLocalAudioStateChanged: " + state + ", error: " + error);
        }

        @Override
        public void onRemoteAudioStateChanged(String channelId, String userId, int state, int reason, int elapsed) {
            super.onRemoteAudioStateChanged(channelId, userId, state, reason, elapsed);
        }

        @Override
        public void onConnectionLost(String channelId) {
            super.onConnectionLost(channelId);
            AppLogger.e(VideoCallActivity.class,"[onConnectionLost] channelId:%s", channelId);
        }

        @Override
        public void onConnectionStateChanged(String channelId, int state, int reason) {
            super.onConnectionStateChanged(channelId, state, reason);
            AppLogger.d(VideoCallActivity.class,"[onConnectionStateChanged] channelId:%s, state:%d, reason:%d", channelId, state, reason);
        }

        @Override
        public void onRtmpStreamingStateChanged(String channelId, String rtmpUrl, int state,
                                                int errCode) {
            super.onRtmpStreamingStateChanged(channelId, rtmpUrl, state, errCode);
            Log.d(TAG,"[onRtmpStreamingStateChanged] channelId:" + channelId + ", rtmpUrl:" + rtmpUrl + ", state:" + state + ", errCode:" + errCode);
        }

        @Override
        public void onVideoSizeChanged(String uid, int width, int height, int rotation) {
            super.onVideoSizeChanged(uid, width, height, rotation);
            AppLogger.d(VideoCallActivity.class,"onVideoSizeChanged uid:%s, width:%d, height:%d, rotation:%d" ,uid, width, height, rotation);
        }

        @Override
        public void onLocalVideoStateChanged(int localVideoState, int error) {
            super.onLocalVideoStateChanged(localVideoState, error);
            Log.d(TAG,"onLocalVideoStateChanged: " + localVideoState + ", error: " + error);
        }

        @Override
        public void onRemoteVideoStateChanged(String channelId, String uid, int state, int reason, int elapsed) {
            super.onRemoteVideoStateChanged(channelId, uid, state, reason, elapsed);
            AppLogger.d(VideoCallActivity.class,"onRemoteVideoStateChanged channelID:%s, userId:%s, state:%d, reason:%d, elapsed:%d", channelId, uid, state, reason, elapsed);
        }

        @Override
        public void onAudioRouteChanged(@Arya.KWAryaAudioRouting int route) {
            super.onAudioRouteChanged(route);
            AppLogger.d(VideoCallActivity.class,"onAudioRouteChanged route:%d", route);
        }

        @Override
        public void onRemoteAudioMute(String channelId, String userId, boolean mute) {
            super.onRemoteAudioMute(channelId, userId, mute);
            AppLogger.d(VideoCallActivity.class,"onRemoteAudioMute channelID:%s, userId:%s, mute:%b", channelId, userId, mute);
            runOnUiThread(() -> {
                MemberInfo memberInfo = getMemberInfo(userId);
                if (memberInfo != null && (memberInfo.isMicOpen == mute)) {
                    memberInfo.isMicOpen = !mute;
                    if (mMemberDialog != null) {
                        mMemberDialog.modifyMember(memberInfo);
                    }

                    KWVideoView videoView = null;
                    if (memberInfoList.size() == 2) { // 1v1
                        // 大小画面
                        if (binding.smallVideoView.getUserID().equals(mUserID)) {
                            videoView = binding.bigVideoView;
                        } else {
                            videoView = binding.smallVideoView;
                        }
                    } else { // 1vN
                        // 网格视图
                        videoView = getVideoView(userId);
                    }
                    if (videoView != null) {
                        videoView.changeViewMemberInfo(memberInfo);
                    }
                }
            });
        }

        @Override
        public void onRemoteVideoMute(String channelId, String userId, boolean mute) {
            super.onRemoteVideoMute(channelId, userId, mute);
            AppLogger.d(VideoCallActivity.class,"onRemoteVideoMute channelID:%s, userId:%s, mute:%b", channelId, userId, mute);
            runOnUiThread(() -> {
                MemberInfo memberInfo = getMemberInfo(userId);
                if (memberInfo != null && (memberInfo.isCameraOpen == mute)) {
                    memberInfo.isCameraOpen = !mute;
                    if (mMemberDialog != null) {
                        mMemberDialog.modifyMember(memberInfo);
                    }

                    KWVideoView videoView = null;
                    if (memberInfoList.size() == 2) { // 1v1
                        // 大小画面
                        if (binding.smallVideoView.getUserID().equals(mUserID)) {
                            videoView = binding.bigVideoView;
                        } else {
                            videoView = binding.smallVideoView;
                        }
                    } else { // 1vN
                        // 网格视图
                        videoView = getVideoView(userId);
                    }
                    if (videoView != null) {
                        videoView.changeViewMemberInfo(memberInfo);
                    }
                }
            });
        }

        @Override
        public void onStreamMessageError(String channelId, String userId, int streamId, int error) {
            super.onStreamMessageError(channelId, userId, streamId, error);
            Log.d(TAG,"onStreamMessageError: channelId " + channelId + " userId " + userId
                    + " streamId " + streamId + " error " + error);
        }

        @Override
        public void onWebsocketOpen() {
            super.onWebsocketOpen();
            Log.d(TAG,"onWebsocketOpen");
        }

        @Override
        public void onWebsocketClose(String message, int status) {
            super.onWebsocketClose(message, status);
            Log.d(TAG,"onWebsocketClose message:" + message + ", status:" + status);
        }

        @Override
        public void onWebsocketFail(String message, int status) {
            super.onWebsocketFail(message, status);
            Log.d(TAG,"onWebsocketFail message:" + message + ", status:" + status);
        }

        @Override
        public void onWebsocketRecvData(byte[] data, int type) {
            super.onWebsocketRecvData(data, type);
            Log.d(TAG,"onWebsocketRecvData data:" + data.length + ", type:" + type);
        }

        @Override
        public void onWebsocketRecvMessage(String message, int type) {
            super.onWebsocketRecvMessage(message, type);
            Log.d(TAG,"onWebsocketRecvMessage message:" + message + ", type:" + type);
        }

        @Override
        public void onRemoteContentStart(String channelId, String userId) {
            super.onRemoteContentStart(channelId, userId);
            Log.d(TAG,"onRemoteContentStart channelId:" + channelId + ", userId:" + userId);
        }

        @Override
        public void onRemoteContentStop(String channelId, String userId) {
            super.onRemoteContentStop(channelId, userId);
            Log.d(TAG,"onRemoteContentStop channelId:" + channelId + ", userId:" + userId);
        }
    }
}