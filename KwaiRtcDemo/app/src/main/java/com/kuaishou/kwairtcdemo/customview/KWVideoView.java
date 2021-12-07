package com.kuaishou.kwairtcdemo.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kuaishou.kwairtcdemo.IKWVideoView;
import com.kuaishou.kwairtcdemo.R;
import com.kuaishou.kwairtcdemo.constants.KWConfigDataKeeper;
import com.kuaishou.kwairtcdemo.constants.KWConstants;
import com.kuaishou.kwairtcdemo.entity.MemberInfo;
import com.kuaishou.kwairtcdemo.utils.DisPlayUtil;

/**
 * 单个视频视图实例
 * 可以承载视频流，视频流相关信息的展示，比如：设备开/关状态，用户信息
 */
public class KWVideoView extends RelativeLayout implements IKWVideoView {

    public KWVideoView(Context context) {
        this(context, null, 0);
    }

    public KWVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KWVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KWVideoView, defStyleAttr, 0);
        mVideoViewSize = a.getInteger(R.styleable.KWVideoView_viewSize, 1);
        a.recycle();
        initViews(context);
    }

    /**
     * 根据设备状态显示占位图及提示
     *
     * @param isCameraOpen 摄像头是否开启
     * @param isMicOpen    麦克风是否开启
     */
    private void showDeviceStateHint(boolean isCameraOpen, boolean isMicOpen) {
        Log.i(TAG, String.format("*** showDeviceStateHint, camera:%b, mic:%b", isCameraOpen, isMicOpen));
        enableCamera = isCameraOpen;
        enableMic = isMicOpen;

        showCameraPlaceHolder();
        changeMicState();
    }

    /**
     * 获取显示视频流的视图
     *
     * @return
     */
    @Override
    public void addRenderView(View renderView) {
        if (mVideoViewLayout != null) {
            mVideoViewLayout.addView(renderView);
            mRenderView = renderView;
            isFree = false;
            showPlaceHolder(false);
            showUserInfo(true);
        }
    }

    @Override
    public void removeRenderView() {
        if (mVideoViewLayout != null && mRenderView != null) {
            mVideoViewLayout.removeView(mRenderView);
            mRenderView = null;
            isFree = true;
            clearUserInfo();
            showPlaceHolder(true);
            showUserInfo(false);
        }
    }

    @Override
    public void changeViewMemberInfo(MemberInfo memberInfo) {
        setUserInfo(memberInfo.userID);
        showDeviceStateHint(memberInfo.isCameraOpen, memberInfo.isMicOpen);
    }

    @Override
    public View getRenderView() {
        return mRenderView;
    }


    /**
     * 设置用户信息
     *
     * @param userID
     */
    private void setUserInfo(String userID) {
        mUserID = userID;
        mIsSelf = userID.equals(KWConfigDataKeeper.getUserId());

        if (mUserID.length() > 3) {
            showNickName = mUserID.substring(0, 3) + "...";
        } else {
            showNickName = mUserID;
        }
        if (mIsSelf) {
            if (mUserID.length() > 2) {
                selfNickNameHolder = mUserID.substring(0, 2) + "..." + KWConstants.NICKNAME_PREFIX;
            } else {
                selfNickNameHolder = mUserID + KWConstants.NICKNAME_PREFIX;
            }
        }
        if (mNickNameTxt != null) {
            if (mIsSelf) {
                mNickNameTxt.setText(selfNickNameHolder);
            } else {
                mNickNameTxt.setText(showNickName);
            }
        }
    }

    /**
     * 获取用户ID
     *
     * @return
     */
    @Override
    public String getUserID() {
        return mUserID;
    }

    @Override
    public String getNickName() {
        return "";
    }

    /**
     * 获取摄像头状态，是否已打开
     *
     * @return
     */
    @Override
    public boolean isCameraOpen() {
        return enableCamera;
    }

    /**
     * 获取麦克风状态，是否已打开
     *
     * @return
     */
    @Override
    public boolean isMicOpen() {
        return enableMic;
    }

    /**
     * 视图是否可用
     * @return
     */
    @Override
    public boolean isFree() {
        return isFree;
    }

    private void clearUserInfo() {
        mUserID = "";
    }
    private static final String TAG = "KWVideoView";
    /**
     * 用户昵称
     */
    private TextView mNickNameTxt;

    /**
     * 麦克风状态
     */
    private ImageView mMicStateIcon;

    /**
     * 占位图上文字
     */
    private TextView mPlaceholderTxt;
    /**
     * 用于装载视频渲染视图的 layout
     */
    private RelativeLayout mVideoViewLayout;
    /**
     * 整个占位布局
     */
    private RelativeLayout mCameraPlaceHolderLayout;
    private RelativeLayout mPlaceHolderLayout;
    private LinearLayout mUserInfoLayout;
    private View mRenderView = null;

    private View mRootView;
    private Context mContext;
    // 用户ID
    private String mUserID = "";
    // 摄像头状态, true:开启 false:关闭
    private boolean enableCamera = true;
    // 麦克风状态，true:开启 false:关闭
    private boolean enableMic = true;

    private boolean isFree = true;
    private String showNickName = "";
    private String selfNickNameHolder = "";
    private int mVideoViewSize = 1; // 0: small view, 1:big view, 2: grid view
    private boolean mIsSelf = false;

    private void initViews(Context context) {
        if (mVideoViewSize == 1) {
            mRootView = LayoutInflater.from(context).inflate(R.layout.kw_video_view_big, this);
        } else if (mVideoViewSize == 0) {
            mRootView = LayoutInflater.from(context).inflate(R.layout.kw_video_view_samll2, this);
        } else {
            mRootView = LayoutInflater.from(context).inflate(R.layout.kw_video_view, this);
        }

        mMicStateIcon = mRootView.findViewById(R.id.micStateIcon);
        mNickNameTxt = mRootView.findViewById(R.id.nickNameTxt);
        mVideoViewLayout = mRootView.findViewById(R.id.showView);
        mPlaceholderTxt = mRootView.findViewById(R.id.placeHolderTxt);
        mCameraPlaceHolderLayout = mRootView.findViewById(R.id.cameraPlaceHolderLayout);
        mPlaceHolderLayout = mRootView.findViewById(R.id.placeHolderLayout);
        mUserInfoLayout = mRootView.findViewById(R.id.userInfoLayout);

        if (mIsSelf) {
            mNickNameTxt.setText(selfNickNameHolder);
        } else {
            mNickNameTxt.setText(showNickName);
        }
    }

    /**
     * 显示摄像头的占位图
     */
    private void showCameraPlaceHolder() {
        if (enableCamera) {
            mCameraPlaceHolderLayout.setVisibility(GONE);
            mVideoViewLayout.setVisibility(VISIBLE);
            mUserInfoLayout.setVisibility(VISIBLE);
            if (mIsSelf) {
                mNickNameTxt.setText(selfNickNameHolder);
            } else {
                mNickNameTxt.setText(showNickName);
            }
        } else {
            mVideoViewLayout.setVisibility(GONE);
            if (mVideoViewSize == 1) {
                mUserInfoLayout.setVisibility(GONE);
            }
            mPlaceholderTxt.setText(showNickName);
            mCameraPlaceHolderLayout.setVisibility(VISIBLE);
        }
    }

    private void showUserInfo(boolean show) {
        if (show) {
            mUserInfoLayout.setVisibility(VISIBLE);
        } else {
            mUserInfoLayout.setVisibility(INVISIBLE);
        }
    }

    /**
     * 是否展示麦克风已关闭提示，显示此提示的时候仅有麦克风关闭
     */
    private void changeMicState() {
        if (enableMic) {
            mMicStateIcon.setImageDrawable(DisPlayUtil.getDrawable(mContext, R.drawable.ic_mic_open_white));
        } else {
            mMicStateIcon.setImageDrawable(DisPlayUtil.getDrawable(mContext, R.drawable.ic_mic_close_white));
        }
    }

    /**
     * 显示待加入占位图
     * @param isShow
     */
    private void showPlaceHolder(boolean isShow) {
        if (isShow) {
            mCameraPlaceHolderLayout.setVisibility(GONE);
            mPlaceHolderLayout.setVisibility(VISIBLE);
        } else {
            mPlaceHolderLayout.setVisibility(GONE);
        }
    }
}