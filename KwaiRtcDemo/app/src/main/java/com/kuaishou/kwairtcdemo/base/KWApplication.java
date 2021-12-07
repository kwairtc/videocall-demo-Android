package com.kuaishou.kwairtcdemo.base;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.kuaishou.kwairtcdemo.log.AppLogger;

public class KWApplication extends Application {
    public static KWApplication application;

    private int activityNum = 0;
    private IAppState mAppStateListener = null;
    @Override
    public void onCreate() {
        super.onCreate();

        application = this;
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    public void setAppStateListener(IAppState appStateListener) {
        mAppStateListener = appStateListener;
    }

    ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (activityNum == 0) {
                if (mAppStateListener != null) {
                    mAppStateListener.onAppState(true);
                }
                AppLogger.d(KWApplication.class, "app is foreground");
            }
            activityNum++;
        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {
            activityNum--;
            if (activityNum == 0) {
                if (mAppStateListener != null) {
                    mAppStateListener.onAppState(false);
                }
                AppLogger.d(KWApplication.class, "app is background");
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    };
}
