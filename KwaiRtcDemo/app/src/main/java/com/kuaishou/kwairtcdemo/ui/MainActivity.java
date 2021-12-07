package com.kuaishou.kwairtcdemo.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.kuaishou.kwairtcdemo.KWManager;
import com.kuaishou.kwairtcdemo.R;
import com.kuaishou.kwairtcdemo.base.BaseActivity;
import com.kuaishou.kwairtcdemo.base.KWApplication;
import com.kuaishou.kwairtcdemo.constants.KWConfigDataKeeper;
import com.kuaishou.kwairtcdemo.constants.KWConstants;
import com.kuaishou.kwairtcdemo.customview.KWCustomToastView;
import com.kuaishou.kwairtcdemo.databinding.ActivityMainBinding;
import com.kuaishou.kwairtcdemo.log.AppLogger;
import com.kuaishou.kwairtcdemo.utils.AppUtil;
import com.kuaishou.kwairtcdemo.utils.PreferenceUtil;
import com.kwai.video.krtc.rtcengine.RtcEngine;
import com.kwai.video.krtc.rtcengine.RtcEngineConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends BaseActivity implements View.OnClickListener, View.OnFocusChangeListener {
    private ActivityMainBinding binding;

    private String mDeviceID;
    private boolean isOpenMic;
    private boolean isOpenCamera;

    private boolean isChannIDIegal = false;
    private boolean isUserIDIegal = false;

    private boolean isNetworkConnected = true;
    private KWCustomToastView toastView = null;
    private BufferedWriter log_writer;

    private static final int PERMISSION_REQUEST_CODE = 101;
    // 权限列表
    private static final String[] PERMISSION_LIST = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

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

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.openCameraSw.setOnClickListener(this);
        binding.openMicSw.setOnClickListener(this);

        binding.channelIDEd.addTextChangedListener(channelIDTxtWatcher);
        binding.channelIDEd.setOnFocusChangeListener(this);
        binding.userIDEd.addTextChangedListener(userIDTxtWatcher);
        binding.userIDEd.setOnFocusChangeListener(this);
        changedBtnState();
    }

    @Override
    protected void initData() {
        super.initData();
        AppLogger.d(MainActivity.class,"initData log path:%s", KWConstants.LOG_PATH);

        resetSetting();
        // 检查是否有音频和摄像头权限
        checkOrRequestPermission(PERMISSION_REQUEST_CODE);
        isOpenCamera = PreferenceUtil.getInstance().getBooleanValue(KWConstants.CAMERA_STATE, true);
        isOpenMic = PreferenceUtil.getInstance().getBooleanValue(KWConstants.MIC_STATE, true);
        changeMicState(isOpenMic);
        changeCameraState(isOpenCamera);

        mDeviceID = PreferenceUtil.getInstance().getStringValue(KWConstants.DEVICE_ID, "");
        if (TextUtils.isEmpty(mDeviceID)) {
            mDeviceID = AppUtil.generateDeviceId(KWApplication.application);
        }
        // 存储常量配置
        KWConfigDataKeeper.setDeviceId(mDeviceID);

        // 创建日志文件
        createLogFile(KWConstants.LOG_PATH, KWConstants.RTC_LOG_FILE, KWConstants.APP_LOG_FILE);
        // 设置日志参数
        RtcEngine.setLogLevel(RtcEngineConstants.LogLevel.kLevelInfo);
        RtcEngine.setLogFile(KWConstants.LOG_PATH + "/" + KWConstants.RTC_LOG_FILE);
        RtcEngine.setLogFileNum(5);

        AppLogger.addLogPrinter((logLevel, logMsg) -> writeAppLogfile(logMsg));
    }

    @Override
    public void finish() {
        super.finish();
        if (toastView != null) {
            toastView.hideView();
            toastView = null;
        }
        try {
            if(log_writer != null){
                log_writer.flush();
                log_writer.close();
                log_writer = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void notifyNetworkChange(boolean isNetworkConnected) {
        super.notifyNetworkChange(isNetworkConnected);
        this.isNetworkConnected = isNetworkConnected;

        if (isNetworkConnected) {
            binding.netErrorHint.setVisibility(View.INVISIBLE);
        } else {
            binding.netErrorHint.setVisibility(View.VISIBLE);
        }
        changedBtnState();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onClickEnterRoom(View view) {
        if (AppUtil.isFastDoubleClick()) {
            return;
        }
        if (checkOrRequestPermission(PERMISSION_REQUEST_CODE)) {
            String roomID = binding.channelIDEd.getText().toString().trim();
            String userID = binding.userIDEd.getText().toString().trim();

            // 存储常量配置
            KWConfigDataKeeper.init(userID, roomID);

            VideoCallActivity.actionStart(MainActivity.this);
        }
    }

    public void onClickSetting(View view) {
        SettingActivity.actionStart(MainActivity.this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.openCameraSw) {
            isOpenCamera = !isOpenCamera;
            changeCameraState(isOpenCamera);
            PreferenceUtil.getInstance().setBooleanValue(KWConstants.CAMERA_STATE, isOpenCamera);
            KWManager.getInstance().setCameraOpen(PreferenceUtil.getInstance().getBooleanValue(KWConstants.CAMERA_STATE, true));

        } else if (view.getId() == R.id.openMicSw) {
            isOpenMic = !isOpenMic;
            changeMicState(isOpenMic);
            PreferenceUtil.getInstance().setBooleanValue(KWConstants.MIC_STATE, isOpenMic);
        }
    }

    /**
     * 恢复默认配置
     */
    private void resetSetting() {
        PreferenceUtil.getInstance().setIntValue(KWConstants.RESOLUTION_WIDTH, KWConstants.DEFAULT_WIDTH);
        PreferenceUtil.getInstance().setIntValue(KWConstants.RESOLUTION_HEIGHT, KWConstants.DEFAULT_HEIGHT);
        PreferenceUtil.getInstance().setIntValue(KWConstants.VIDEO_BITRATE, KWConstants.DEFAULT_MAX_BITRATE);
        PreferenceUtil.getInstance().setIntValue(KWConstants.VIDEO_MIN_BITRATE, KWConstants.DEFAULT_MIN_BITRATE);
        PreferenceUtil.getInstance().setIntValue(KWConstants.VIDEO_MAX_BITRATE, KWConstants.DEFAULT_MAX_BITRATE);
        PreferenceUtil.getInstance().setIntValue(KWConstants.VIDEO_FPS, KWConstants.DEFAULT_FPS);
        PreferenceUtil.getInstance().setIntValue(KWConstants.RESOLUTION_LEVEL, KWConstants.DEFAULT_RESOLUTION_LEVEL);
        PreferenceUtil.getInstance().setBooleanValue(KWConstants.AV_QUALITY_SWITCH, false);
    }

    /**
     * 校验并请求权限
     */
    public boolean checkOrRequestPermission(int code) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(PERMISSION_LIST, code);
                return false;
            }
        }
        return true;
    }

    private void changeCameraState(boolean isOpenCamera) {
        if (isOpenCamera) {
            binding.openCameraSw.setImageResource(R.drawable.ic_sw_open);
        } else {
            binding.openCameraSw.setImageResource(R.drawable.ic_sw_close);
        }
    }

    private void changeMicState(boolean isOpenMic) {
        if (isOpenMic) {
            binding.openMicSw.setImageResource(R.drawable.ic_sw_open);
        } else {
            binding.openMicSw.setImageResource(R.drawable.ic_sw_close);
        }
    }

    private void changedBtnState() {
        if (isNetworkConnected && isChannIDIegal && isUserIDIegal) {
            binding.startLiveBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_blue_bg));
            binding.startLiveBtn.setClickable(true);
        } else {
            if (binding.startLiveBtn.isClickable()) {
                binding.startLiveBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_blue_disable_bg));
                binding.startLiveBtn.setClickable(false);
            }
        }
    }

    private TextWatcher channelIDTxtWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String tmp = editable.toString();
            if (tmp.length() == 0) {
                binding.channelIDEdLine.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red2));
                binding.roomidWriteHint.setVisibility(View.VISIBLE);
                isChannIDIegal = false;
            } else {
                if (binding.roomidWriteHint.getVisibility() == View.VISIBLE) {
                    binding.channelIDEdLine.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue5));
                    binding.roomidWriteHint.setVisibility(View.INVISIBLE);
                }
                isChannIDIegal = true;
            }
            changedBtnState();
        }
    };

    private TextWatcher userIDTxtWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String tmp = editable.toString();
            if (tmp.length() == 0) {
                binding.userIDEdLine.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red2));
                binding.userIdWriteHint.setVisibility(View.VISIBLE);
                isUserIDIegal = false;
            } else {
                if (binding.userIdWriteHint.getVisibility() == View.VISIBLE) {
                    binding.userIDEdLine.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue5));
                    binding.userIdWriteHint.setVisibility(View.INVISIBLE);
                }
                isUserIDIegal = true;
            }
            changedBtnState();
        }
    };

    private void createLogFile(String logPath, String logFile, String appLogFile) {

        File logFolder = new File(logPath);
        if (!logFolder.exists()) {
            if (!logFolder.mkdirs()) {
                return;
            }
        }
        File file = new File(logPath + "/" + logFile);
        if(file.exists()){
            if(file.length() > 1024 * 1024 * 10){
                file.delete();
            }
        }
        try{
            if(!file.exists()){
                file.createNewFile();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        File demo_log_file = new File(logPath + "/" + appLogFile);
        if(demo_log_file.exists()){
            if(demo_log_file.length() > 1024 * 1024 * 10){
                demo_log_file.delete();
            }
        }

        try{
            if(!demo_log_file.exists()){
                demo_log_file.createNewFile();
            }
            log_writer = new BufferedWriter(new FileWriter(demo_log_file, true));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeAppLogfile(String log){
        if (log_writer == null) {
            return;
        }
        try {
            log_writer.write(log);
            log_writer.newLine();
            log_writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFocusChange(View view, boolean b) {
        int vId = view.getId();
        if (vId == R.id.channelIDEd) {
            if (b) {
                binding.channelIDEdLine.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue5));
            } else {
                if (isChannIDIegal) {
                    binding.channelIDEdLine.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.black_10));
                }
                if (TextUtils.isEmpty(binding.channelIDEd.getText().toString().trim())) {
                    binding.channelIDEdLine.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red2));
                    binding.roomidWriteHint.setVisibility(View.VISIBLE);
                    isChannIDIegal = false;
                }
            }
        } else if (vId == R.id.userIDEd) {
            if (b) {
                binding.userIDEdLine.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.blue5));
            } else {
                if (isUserIDIegal) {
                    binding.userIDEdLine.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.black_10));
                }
                if (TextUtils.isEmpty(binding.userIDEd.getText().toString().trim())) {
                    binding.userIDEdLine.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red2));
                    binding.userIdWriteHint.setVisibility(View.VISIBLE);
                    isUserIDIegal = false;
                }
            }
        }
    }
}