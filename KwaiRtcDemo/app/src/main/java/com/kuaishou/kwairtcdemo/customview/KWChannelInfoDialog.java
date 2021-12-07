package com.kuaishou.kwairtcdemo.customview;

import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.kuaishou.kwairtcdemo.R;
import com.kuaishou.kwairtcdemo.constants.KWConfigDataKeeper;

public class KWChannelInfoDialog extends Dialog {

    private TextView mChannelID;
    private Context mContext;

    private KWCustomToastView toastView;

    public KWChannelInfoDialog(@NonNull Context context) {
        super(context, R.style.KWCommonDialog);
        mContext = context;
        initView(context);
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.channel_info_layout, null);
        setContentView(view);
        // 设置可以取消
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        // 设置Dialog高度位置
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = Gravity.BOTTOM;
        View decorView = getWindow().getDecorView();
        // 设置没有边框
        decorView.setPadding(0, 0, 0, 0);
        getWindow().setAttributes(layoutParams);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        mChannelID = findViewById(R.id.copyChannelIDTxt);

        toastView = new KWCustomToastView(mContext);

        findViewById(R.id.copyChannelIDBtn).setOnClickListener(listener -> {
            copyChannelID();
        });
        mChannelID.setOnClickListener(listener -> {
            copyChannelID();
        });

        findViewById(R.id.dragDown).setOnClickListener(listener -> {
            this.dismiss();
        });

        mChannelID.setText(KWConfigDataKeeper.getChannelId());
    }

    private void copyChannelID() {
        // copy
        ClipboardManager cmb = (ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        cmb.setText(mChannelID.getText().toString());

        toastView.showView(mContext.getString(R.string.ks_copy_success), 1000);
        this.dismiss();
    }
}
