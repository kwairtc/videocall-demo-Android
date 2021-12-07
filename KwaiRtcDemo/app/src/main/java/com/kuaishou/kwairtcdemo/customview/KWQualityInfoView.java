package com.kuaishou.kwairtcdemo.customview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.kuaishou.kwairtcdemo.R;
import com.kuaishou.kwairtcdemo.entity.QualityInfo;

public class KWQualityInfoView extends LinearLayout {
    public KWQualityInfoView(Context context) {
        this(context, null, 0);
    }

    public KWQualityInfoView(Context context, @Nullable AttributeSet attrs) {
       this(context, attrs, 0);
    }

    public KWQualityInfoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initViews(context);
    }

    public void updateQosValue(QualityInfo qualityInfo) {
        cpuValue.setText(mContext.getString(R.string.ks_cpu_value, qualityInfo.appCPU,
                (qualityInfo.totalCPU == 0) ? "-" : String.valueOf(qualityInfo.totalCPU)));
        memoryValue.setText(mContext.getString(R.string.ks_memory_value, qualityInfo.memory));
        rttValue.setText(mContext.getString(R.string.ks_rtt_value, (qualityInfo.rtt == -1) ? "-" : String.valueOf(qualityInfo.rtt)));
        videoParamValue.setText(mContext.getString(R.string.ks_video_param_value,
                qualityInfo.width, qualityInfo.height, qualityInfo.fps));
        videoRTBitrateValue.setText(mContext.getString(R.string.ks_video_rt_bitrate_value,
                qualityInfo.videoSendBitrate, qualityInfo.videoRecvBitrate));
        audioRTBitrateValue.setText(mContext.getString(R.string.ks_audio_rt_bitrate_value,
                qualityInfo.audioSendBitrate, qualityInfo.audioRecvBitrate));
        rtLossValue.setText(mContext.getString(R.string.ks_rt_loss_value,
                (qualityInfo.sendLoss == -1) ? "-" : String.valueOf(qualityInfo.sendLoss/10),
                (qualityInfo.recvLoss == -1) ? "-" : String.valueOf(qualityInfo.recvLoss/10)));
    }

    private Context mContext;
    private TextView cpuValue;
    private TextView memoryValue;
    private TextView rttValue;
    private TextView videoParamValue;
    private TextView videoRTBitrateValue;
    private TextView audioRTBitrateValue;
    private TextView rtLossValue;

    private void initViews(Context context) {
        View layout = LayoutInflater.from(context).inflate(R.layout.qos_info_layout, this);

        cpuValue = layout.findViewById(R.id.cpuTxt);
        memoryValue = layout.findViewById(R.id.memoryTxt);
        rttValue = layout.findViewById(R.id.rttTxt);
        videoParamValue = layout.findViewById(R.id.videoParamTxt);
        videoRTBitrateValue = layout.findViewById(R.id.videoBitrateTxt);
        audioRTBitrateValue = layout.findViewById(R.id.audioBitrateTxt);
        rtLossValue = layout.findViewById(R.id.lossTxt);
    }
}
