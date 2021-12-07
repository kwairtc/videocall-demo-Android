package com.kuaishou.kwairtcdemo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.kuaishou.kwairtcdemo.KWManager;
import com.kuaishou.kwairtcdemo.R;
import com.kuaishou.kwairtcdemo.base.BaseActivity;
import com.kuaishou.kwairtcdemo.constants.KWConstants;
import com.kuaishou.kwairtcdemo.databinding.ActivitySettingPageBinding;
import com.kuaishou.kwairtcdemo.log.AppLogger;
import com.kuaishou.kwairtcdemo.utils.PreferenceUtil;
import com.kwai.video.krtc.rtcengine.RtcEngine;

public class SettingActivity extends BaseActivity {

    private ActivitySettingPageBinding binding;
    private static boolean showQuality = false;

    private String[] showResolutionArr;
    private String[] resolutionStrArr;
    private int[] bitrateArr;
    private int[] minBitrateArr;
    private int[] maxBitrateArr;
    private int[] fpsArr;
    private int[] resolutionWidthArr = new int[3];
    private int[] resolutionHeightArr = new int[3];

    private int currentResolutionPosition;

    /**
     * request Code 选中的分辨率
     **/
    public final static int SELECTED_RESOLUTION = 1008;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initViews() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting_page);

        binding.sdkVerTxt.setText(RtcEngine.getSdkVersion());
        binding.demoVerTxt.setText(getDemoVersion());

        boolean isShowAVQuality = PreferenceUtil.getInstance().getBooleanValue(KWConstants.AV_QUALITY_SWITCH, false);
        changeAVQualitySwitch(isShowAVQuality);

        binding.showQualitySw.setOnClickListener(listener -> {
            showQuality = !showQuality;
            changeAVQualitySwitch(showQuality);

            KWManager.OnAVSettingChangedListener avSettingListener = KWManager.getInstance().getAvSettingListener();
            if (avSettingListener != null) {
                avSettingListener.onAVQualitySwitchChanged(showQuality);
            }
            PreferenceUtil.getInstance().setBooleanValue(KWConstants.AV_QUALITY_SWITCH, showQuality);
        });

        binding.resolutionLayout.setOnClickListener(listener -> {
            Intent intent = new Intent(this, ResolutionActivity.class);
            intent.putExtra(KWConstants.SELECTED_INDEX, currentResolutionPosition);
            startActivityForResult(intent, SELECTED_RESOLUTION);
        });
    }

    @Override
    protected void initData() {
        super.initData();
        currentResolutionPosition = PreferenceUtil.getInstance().getIntValue(KWConstants.RESOLUTION_LEVEL, 0);
        AppLogger.d(SettingActivity.class, "initData currentResolutionPosition:%d", currentResolutionPosition);

        resolutionStrArr = getResources().getStringArray(R.array.resolution_setting_value);
        showResolutionArr = getResources().getStringArray(R.array.resolution_setting_describe);
        bitrateArr = getResources().getIntArray(R.array.bitrate_setting_value);
        minBitrateArr = getResources().getIntArray(R.array.min_bitrate_setting_value);
        maxBitrateArr = getResources().getIntArray(R.array.max_bitrate_setting_value);
        fpsArr = getResources().getIntArray(R.array.fps_setting_value);

        for (int i=0; i<resolutionStrArr.length; i++) {
            String[] tmp = resolutionStrArr[i].split("x");
            resolutionWidthArr[i] = Integer.parseInt(tmp[0]);
            resolutionHeightArr[i] = Integer.parseInt(tmp[1]);
        }

        showVideoParam(currentResolutionPosition);
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
        AppLogger.d(SettingActivity.class, "***** onAppState:%b", isForeGround);
        KWManager.getInstance().dealBackgroundOpenCamera(isForeGround);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (RESULT_OK != resultCode || data == null) {
            return;
        }
        if (requestCode == SELECTED_RESOLUTION) {
            currentResolutionPosition = data.getIntExtra(KWConstants.SELECTED_INDEX, 0);
            showVideoParam(currentResolutionPosition);
        }
    }

    @Override
    public void onBackPressed() {
        applyVideoParam();
        super.onBackPressed();
    }

    public void onClickGoBack(View view) {
        applyVideoParam();
        finish();
    }

    /**
     * 供其他Activity调用，进入当前Activity查看版本
     *
     * @param activity
     */
    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, SettingActivity.class);
        activity.startActivity(intent);
    }

    private void applyVideoParam() {
        saveVideoSetting();
        KWManager.OnAVSettingChangedListener avSettingListener = KWManager.getInstance().getAvSettingListener();
        if (avSettingListener != null) {
            avSettingListener.onVideoEncodeParamChanged(resolutionWidthArr[currentResolutionPosition],
                    resolutionHeightArr[currentResolutionPosition],
                    bitrateArr[currentResolutionPosition],
                    fpsArr[currentResolutionPosition],
                    minBitrateArr[currentResolutionPosition],
                    maxBitrateArr[currentResolutionPosition]);
        }
    }

    private void showVideoParam(int position) {
        binding.resolutionValue.setText(showResolutionArr[position]);
        binding.bitrateValue.setText(String.valueOf(maxBitrateArr[position]));
        binding.fpsValue.setText(String.valueOf(fpsArr[position]));
    }

    private void saveVideoSetting() {
        AppLogger.d(SettingActivity.class,"saveVideoSetting currentResolutionPosition:%d", currentResolutionPosition);
        PreferenceUtil.getInstance().setIntValue(KWConstants.RESOLUTION_WIDTH, resolutionWidthArr[currentResolutionPosition]);
        PreferenceUtil.getInstance().setIntValue(KWConstants.RESOLUTION_HEIGHT, resolutionHeightArr[currentResolutionPosition]);
        PreferenceUtil.getInstance().setIntValue(KWConstants.VIDEO_BITRATE, bitrateArr[currentResolutionPosition]);
        PreferenceUtil.getInstance().setIntValue(KWConstants.VIDEO_FPS, fpsArr[currentResolutionPosition]);
        PreferenceUtil.getInstance().setIntValue(KWConstants.RESOLUTION_LEVEL, currentResolutionPosition);
        PreferenceUtil.getInstance().setIntValue(KWConstants.VIDEO_MIN_BITRATE, minBitrateArr[currentResolutionPosition]);
        PreferenceUtil.getInstance().setIntValue(KWConstants.VIDEO_MAX_BITRATE, maxBitrateArr[currentResolutionPosition]);
    }

    private void changeAVQualitySwitch(boolean isOpen) {
        if (isOpen) {
            binding.showQualitySw.setImageResource(R.drawable.ic_sw_open);
        } else {
            binding.showQualitySw.setImageResource(R.drawable.ic_sw_close);
        }
    }
}