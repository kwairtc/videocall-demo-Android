package com.kuaishou.kwairtcdemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.kuaishou.kwairtcdemo.R;
import com.kuaishou.kwairtcdemo.base.BaseActivity;
import com.kuaishou.kwairtcdemo.constants.KWConstants;
import com.kuaishou.kwairtcdemo.databinding.ResolutionLayoutBinding;

public class ResolutionActivity extends BaseActivity {

    private ResolutionLayoutBinding binding;
    private int currentSelectedIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initViews() {
        binding = DataBindingUtil.setContentView(this, R.layout.resolution_layout);

        int selectedIndex = getIntent().getIntExtra(KWConstants.SELECTED_INDEX, 0);
        currentSelectedIndex = selectedIndex;
        // 显示初始值
        if (selectedIndex == 0) {
            show360P();
        } else if (selectedIndex == 1) {
            show540P();
        } else {
            show720P();
        }

        binding.relayout360P.setOnClickListener(listener -> {
            currentSelectedIndex = 0;
            show360P();
        });
        binding.relayout540P.setOnClickListener(listener -> {
            currentSelectedIndex = 1;
            show540P();
        });
        binding.relayout720P.setOnClickListener(listener -> {
            currentSelectedIndex = 2;
            show720P();
        });

        binding.goBackBtn.setOnClickListener(listener -> {
            backPage();
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        backPage();
        super.onBackPressed();
    }

    private void backPage() {
        Intent intent = new Intent();
        intent.putExtra(KWConstants.SELECTED_INDEX, currentSelectedIndex);
        this.setResult(RESULT_OK, intent);
    }

    private void show360P() {
        binding.image360P.setVisibility(View.VISIBLE);
        binding.image540P.setVisibility(View.INVISIBLE);
        binding.image720P.setVisibility(View.INVISIBLE);
    }

    private void show540P() {
        binding.image360P.setVisibility(View.INVISIBLE);
        binding.image540P.setVisibility(View.VISIBLE);
        binding.image720P.setVisibility(View.INVISIBLE);
    }

    private void show720P() {
        binding.image360P.setVisibility(View.INVISIBLE);
        binding.image540P.setVisibility(View.INVISIBLE);
        binding.image720P.setVisibility(View.VISIBLE);
    }
}