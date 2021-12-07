package com.kuaishou.kwairtcdemo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class DisPlayUtil {

    @SuppressLint("ObsoleteSdkInt")
    public static Drawable getDrawable(Context context, int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getDrawable(resId);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return context.getResources().getDrawable(resId);
        } else {
            // Prior to JELLY_BEAN, Resources.getDrawable() would not correctly
            // retrieve the final configuration density when the resource ID
            // is a reference another Drawable resource. As a workaround, try
            // to resolve the drawable reference manually.
            // final int resolvedId;
            // synchronized (sLock) {
            //     if (sTempValue == null) {
            //         sTempValue = new TypedValue();
            //     }
            //     context.getResources().getValue(id, sTempValue, true);
            //     resolvedId = sTempValue.resourceId;
            // }
            return context.getResources().getDrawable(resId);
        }
    }

    @SuppressWarnings("unused")
    public static int getScreenWidth(Activity activity) {
        // 获取屏幕宽高
        Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        return point.x;
    }

    @SuppressWarnings("unused")
    public static int getScreenHeight(Activity activity) {
        // 获取屏幕宽高
        Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        return point.y;
    }

    /**
     * 通过设置全屏，设置状态栏透明
     *
     * @param activity
     */
    public static void fullScreen(Activity activity) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //5.x开始需要把颜色设置透明，否则导航栏会呈现系统默认的浅灰色
            View decorView = window.getDecorView();
            //两个 flag 要结合使用，表示让应用的主体内容占用系统状态栏的空间
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 解决图标为白色问题
            decorView.setSystemUiVisibility(option);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            //导航栏颜色也可以正常设置
            window.setNavigationBarColor(Color.TRANSPARENT);

        } else {
            WindowManager.LayoutParams attributes = window.getAttributes();
            int flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            attributes.flags |= flagTranslucentStatus;
//                int flagTranslucentNavigation = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
//                attributes.flags |= flagTranslucentNavigation;
            window.setAttributes(attributes);
        }
    }

    /**
     * 自定义矩形样式
     * @param radius 圆角度数，单位：dp
     * @param strokeWidth 线框宽度，单位：dp; 0表示无线框，>0表示有线框
     * @param strokeColor 线框颜色，无线框时可填 0
     * @param solidColor  填充色
     */
    public static GradientDrawable customizeRectangleStyle(Context context, float radius,
                                                           int strokeWidth, int strokeColor,
                                                           int solidColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.mutate();
        drawable.setCornerRadius(dip2px(context, radius));
        drawable.setColor(solidColor);
        if (strokeWidth > 0) {
            drawable.setStroke((int) (dip2px(context, strokeWidth) + 0.5f), strokeColor);
        }
        return drawable;
    }

    /**
     * 获取 dp 对应具体分辨率下的 pixel
     * @param context
     * @param dip
     * @return
     */
    public static float dip2px(Context context, float dip) {
        float m = context.getResources().getDisplayMetrics().density ;
        return dip * m;
    }
}
