package com.kuaishou.kwairtcdemo.customview;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.kuaishou.kwairtcdemo.R;

/**
 * 自定义 toast view
 */
public class KWCustomToastView extends Dialog {

    public KWCustomToastView(@NonNull Context context) {
        super(context, R.style.KWCommonDialog);

        initView(context);
    }

    /**
     * 显示 toast
     * @param duration 显示时长，单位：ms
     */
    public void showView(String hintText, int duration) {
        if (isShowingToast) {
            hideView();
        }
        isShowingToast = true;
        mHintContent.setText(hintText);

        mUiHandler.postDelayed(() -> {
            if (isShowingToast) {
                hideView();
            }
        }, duration);
        this.show();
    }

    /**
     * 不再展示 toast
     */
    public void hideView() {
        isShowingToast = false;
        this.dismiss();
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.toast_layout, null);
        setContentView(view);
        // 设置可以取消
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        // 设置Dialog高度位置
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

        //竖屏
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = Gravity.CENTER;
        View decorView = getWindow().getDecorView();
        // 设置没有边框
        decorView.setPadding(0, 0, 0, 0);
        getWindow().setAttributes(layoutParams);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        mHintContent = findViewById(R.id.hintContent);
    }

    private Handler mUiHandler = new Handler(Looper.getMainLooper());
    private TextView mHintContent;
    private boolean isShowingToast = false;
}
