package com.kuaishou.kwairtcdemo.customview;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kuaishou.kwairtcdemo.R;
import com.kuaishou.kwairtcdemo.adapter.MemberListAdapter;
import com.kuaishou.kwairtcdemo.entity.MemberInfo;

import java.util.ArrayList;

public class KWMemberDialog extends Dialog {

    private RecyclerView memberList;

    private MemberListAdapter memberListAdapter;
    private TextView memberCountTxt;
    private Context mContext;

    /**
     *
     * @param context
     */
    public KWMemberDialog(@NonNull Context context) {
        super(context, R.style.KWCommonDialog);

        mContext = context;
        initView(context);
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_member_list_layout, null);

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

        memberList = findViewById(R.id.member_list);
        memberCountTxt = findViewById(R.id.memberValue);

        findViewById(R.id.memDragDown).setOnClickListener(listener -> {
            this.dismiss();
        });
        findViewById(R.id.memberLayout).setOnClickListener(listener -> {
            this.dismiss();
        });

        if (memberListAdapter == null) {
            memberListAdapter = new MemberListAdapter();
        }
        memberList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        memberList.setAdapter(memberListAdapter);
        memberList.setItemAnimator(new DefaultItemAnimator());
    }

    public void addMember(MemberInfo memberInfo) {
        memberCountTxt.setText(mContext.getString(R.string.ks_members, memberListAdapter.getItemCount() + 1));
        memberListAdapter.addMember(memberInfo);
    }

    public void deleteMember(MemberInfo memberInfo) {
        memberCountTxt.setText(mContext.getString(R.string.ks_members, memberListAdapter.getItemCount() - 1));
        memberListAdapter.deleteMember(memberInfo);
    }

    public void addMemberList(ArrayList<MemberInfo> members) {
        memberListAdapter.addMembers(members);
        memberCountTxt.setText(mContext.getString(R.string.ks_members, memberListAdapter.getItemCount()));
    }

    public void modifyMember(MemberInfo memberInfo) {
        memberListAdapter.modifyMember(memberInfo);
    }

    public void clearMembers() {
        memberListAdapter.clear();
    }
}
