package com.kuaishou.kwairtcdemo.adapter;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.kuaishou.kwairtcdemo.R;
import com.kuaishou.kwairtcdemo.constants.KWConfigDataKeeper;
import com.kuaishou.kwairtcdemo.constants.KWConstants;
import com.kuaishou.kwairtcdemo.entity.MemberInfo;
import com.kuaishou.kwairtcdemo.log.AppLogger;

import java.util.ArrayList;

public class MemberListAdapter extends RecyclerView.Adapter {

    private ArrayList<MemberInfo> memberList = new ArrayList<>();

    public MemberListAdapter() {
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_item, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final MyViewHolder myViewHolder = (MyViewHolder) holder;

        MemberInfo memberInfo = memberList.get(position);
        String tmp, imageName;
        if (memberInfo.userID.length() > 6) {
            tmp = memberInfo.userID.substring(0, 6) + "...";
        } else {
            tmp = memberInfo.userID;
        }
        if (memberInfo.userID.length() > 2) {
            imageName = memberInfo.userID.substring(0, 2) + "...";
        } else {
            imageName = memberInfo.userID;
        }

        if (memberInfo.userID.equals(KWConfigDataKeeper.getUserId())) {
            tmp += KWConstants.NICKNAME_PREFIX;
        }
        myViewHolder.nickName.setText(tmp);
        myViewHolder.imageNickName.setText(imageName);
        if (memberInfo.isMicOpen != myViewHolder.isMicOpen) {
            myViewHolder.isMicOpen = memberInfo.isMicOpen;
            myViewHolder.micIcon.setImageResource(memberInfo.isMicOpen ? R.drawable.ic_mic_open_mem : R.drawable.ic_mic_close_mem);
        }
        if (memberInfo.isCameraOpen != myViewHolder.isCameraOpen) {
            myViewHolder.isCameraOpen = memberInfo.isCameraOpen;
            myViewHolder.cameraIcon.setImageResource(memberInfo.isCameraOpen ? R.drawable.ic_camera_open_mem : R.drawable.ic_camera_close_mem);
        }
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    /**
     * 新增成员
     *
     * @param memberInfo
     */
    public synchronized void addMember(MemberInfo memberInfo) {
        AppLogger.d(MemberListAdapter.class,"**** add member userid:%s", memberInfo.userID);
        for (MemberInfo tmp : memberList) {
            if (tmp.userID.equals(memberInfo.userID)) {
                return;
            }
        }
        memberList.add(memberInfo);
        notifyItemInserted(memberList.size()-1);
    }

    public synchronized void addMembers(ArrayList<MemberInfo> userInfos) {
        memberList.clear();
        memberList.addAll(userInfos);
        notifyDataSetChanged();
    }

    public synchronized void modifyMember(MemberInfo memberInfo) {
        AppLogger.d(MemberListAdapter.class,"**** modify member，userID:%s，isMicOpen：%b, isCameraOpen:%b",
                memberInfo.userID, memberInfo.isMicOpen, memberInfo.isCameraOpen);
        int index = memberList.indexOf(memberInfo);
        memberList.set(index, memberInfo);
        notifyItemChanged(index);
    }

    public synchronized void deleteMember(MemberInfo memberInfo) {
        AppLogger.d(MemberListAdapter.class,"**** deleteMember，userID:%s", memberInfo.userID);
        int pos = memberList.indexOf(memberInfo);
        memberList.remove(memberInfo);
        notifyItemRemoved(pos);
    }

    public void clear() {
        memberList.clear();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView nickName;
        ImageView micIcon;
        ImageView cameraIcon;
        View itemView;
        TextView imageNickName;

        private boolean isMicOpen = true;
        private boolean isCameraOpen = true;

        MyViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.nickName = itemView.findViewById(R.id.itemName);
            this.imageNickName = itemView.findViewById(R.id.memNickName);
            this.micIcon = itemView.findViewById(R.id.micStateIcon);
            this.cameraIcon = itemView.findViewById(R.id.cameraStateIcon);
        }
    }
}

