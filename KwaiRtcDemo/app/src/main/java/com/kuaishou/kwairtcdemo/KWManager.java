package com.kuaishou.kwairtcdemo;

import android.text.TextUtils;

import com.kuaishou.kwairtcdemo.constants.KWConfigDataKeeper;
import com.kuaishou.kwairtcdemo.entity.QualityInfo;
import com.kuaishou.kwairtcdemo.log.AppLogger;
import com.kwai.video.krtc.rtcengine.RtcEngine;
import com.kwai.video.krtc.rtcengine.RtcEngineConfig;

public class KWManager {
    private RtcEngine rtcEngine = null;
    private RtcEngine.FaceBeautyOptions faceBeautyOptions = null;
    private QualityInfo qualityInfo = null;
    private OnAVSettingChangedListener avSettingListener = null;

    public boolean isCameraOpen() {
        return mIsCameraOpen;
    }

    public void setCameraOpen(boolean mIsCameraOpen) {
        this.mIsCameraOpen = mIsCameraOpen;
    }

    private boolean mIsCameraOpen;

    private static class InnerClass {
        private static KWManager kwManager = new KWManager();
    }

    public static KWManager getInstance() {
        return InnerClass.kwManager;
    }

    public RtcEngine getRtcEngine() {
        return rtcEngine;
    }

    public void createEngine(RtcEngineConfig config) {
        if (rtcEngine == null) {
            try {
                rtcEngine = RtcEngine.create(config);
                AppLogger.d(KWManager.class, "create engine success");

                faceBeautyOptions = new RtcEngine.FaceBeautyOptions(); // 默认值，美白、磨皮都是0.5
                qualityInfo = new QualityInfo();
            } catch (Exception e) {
                e.printStackTrace();
                RtcEngine.destroy();
                rtcEngine = null;
                AppLogger.d(KWManager.class, "create engine fail:%s", e.getMessage());
            }
        }
    }

    public void destoryEngine() {
        if (rtcEngine != null) {
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    public void setVideoEncodeParam(String channelID, int width, int height, int bitrate, int fps, int minBitrate, int maxBitrate) {
        if (rtcEngine != null) {
            RtcEngine.VideoEncoderConfiguration configuration = new RtcEngine.VideoEncoderConfiguration();
            configuration.targetWidth = width;
            configuration.targetHeight = height;
            configuration.frameRate = fps;
            configuration.initBitrate = bitrate;
            configuration.minBitrate = minBitrate;
            configuration.maxBitrate = maxBitrate;
            configuration.orientationMode = RtcEngine.OrientationMode.kOrientationModePortrait;
            rtcEngine.setVideoEncoderConfiguration(channelID, configuration);

            AppLogger.d(KWManager.class, "setVideoEncodeParam width:%d, height:%d, bitrate:%d, min_bitrate:%d, max_bitrate:%d, fps:%d",
                    width, height, bitrate, minBitrate, maxBitrate, fps);
        }
    }
    /**
     * 设置美颜参数
     * @param lighteningLevel 美白参数，0.0～1.0
     * @param smoothnessLevel 磨皮参数，0.0~1.0
     */
    public void setupBeautyOptions(float lighteningLevel, float smoothnessLevel) {
        if (faceBeautyOptions != null && rtcEngine != null) {
            faceBeautyOptions.setLighteningLevel(lighteningLevel);
            faceBeautyOptions.setSmoothnessLevel(smoothnessLevel);
            rtcEngine.setBeautyEffectOptions(true, faceBeautyOptions);
        }
    }

    public QualityInfo getAVQualityInfo() {
        return qualityInfo;
    }

    public void dealBackgroundOpenCamera(boolean isOpen) {
        if (rtcEngine == null || TextUtils.isEmpty(KWConfigDataKeeper.getChannelId())) {
            return;
        }
        if (isOpen && isCameraOpen()) {
            rtcEngine.unmuteLocalVideoStream(KWConfigDataKeeper.getChannelId());
            rtcEngine.startPreview();
        } else {
            rtcEngine.muteLocalVideoStream(KWConfigDataKeeper.getChannelId());
            rtcEngine.stopPreview();
        }
    }

    public void setOnAVSettingChangedListener(OnAVSettingChangedListener listener) {
        avSettingListener = listener;
    }

    public OnAVSettingChangedListener getAvSettingListener() {
        return avSettingListener;
    }

    public interface OnAVSettingChangedListener {
        void onAVQualitySwitchChanged(boolean isOpen);
        void onVideoEncodeParamChanged(int width, int height, int bitrate, int fps, int minBitrate, int maxBitrate);
    }
}
