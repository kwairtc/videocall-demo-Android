package com.kuaishou.kwairtcdemo.customview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.annotation.Nullable;

import com.kuaishou.kwairtcdemo.KWManager;
import com.kuaishou.kwairtcdemo.R;
import com.kuaishou.kwairtcdemo.constants.KWConstants;

public class KWBeautyView extends RelativeLayout {
    public KWBeautyView(Context context) {
        this(context, null, 0);
    }

    public KWBeautyView(Context context, @Nullable AttributeSet attrs) {
       this(context, attrs, 0);
    }

    public KWBeautyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews(context);
    }
    public void setConfigView() {
        // 初始化磨皮步长, 默认为 0.5
        mSeekBarSmoothness.setMax((int)smoothnessLevelMax * 100);
        mSeekBarSmoothness.setProgress((int)(currentSmoothnessLevel * 100));

        // 初始化美白修正参数, 默认为 0.5
        mSeekBarLightening.setMax((int)lighteningLevelMax * 100);
        mSeekBarLightening.setProgress((int)(currentLighteningLevel * 100));
    }
    private SeekBar mSeekBarSmoothness;
    private SeekBar mSeekBarLightening;

    private float smoothnessLevelMin = 0.0f;
    private float smoothnessLevelMax = 1.0f;
    private float currentSmoothnessLevel = KWConstants.DEFAULT_BEAUTY_SMOOTHNESS; // 默认值

    private float whitenFactorMin = 0.0f;
    private float lighteningLevelMax = 1.0f;
    private float currentLighteningLevel = KWConstants.DEFAULT_BEAUTY_LIGHTENING; // 默认值

    private void initViews(Context context) {
        View layout = LayoutInflater.from(context).inflate(R.layout.dialog_beauty_layout, this);

        mSeekBarSmoothness = layout.findViewById(R.id.smoothnessSeek);
        mSeekBarLightening = layout.findViewById(R.id.lighteningSeek);

        mSeekBarSmoothness.setOnSeekBarChangeListener(seekBarChangeListener);
        mSeekBarLightening.setOnSeekBarChangeListener(seekBarChangeListener);

        setConfigView();
    }

    final SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar.getId() == R.id.smoothnessSeek) {
                currentSmoothnessLevel = (progress * 1.0f) / 100;
            } else if (seekBar.getId() == R.id.lighteningSeek) {
                currentLighteningLevel = (progress * 1.0f) / 100;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            KWManager.getInstance().setupBeautyOptions(currentLighteningLevel, currentSmoothnessLevel);
        }
    };
}
