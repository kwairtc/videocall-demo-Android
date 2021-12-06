# KRTC Android Demo

本示例程序演示了如何使用 KRTC SDK 实现实时音视频通话。

## 前提条件
- 开发环境
    - Android Studio 3.0 或以上版本
    - Android SDK API 等级 17 或以上 
- Android 4.2 或以上版本的设备

## 配置 AppId

体验 Demo 的功能需要将您在 KRTC 控制台生成的 AppId / AppName / AppSign 自行替换到 Demo 中。
您可以在 `com.kuaishou.kwairtcdemo.constants` 包名下找到 KWConstants.java 文件中找到下面对应的静态变量进行替换。

```
public static final String APPID = "";    // 替换为您的 AppId
public static final String APPNAME = "";  // 替换为您的 AppName
public static final String TOKEN = "";    // 替换为您的 AppSign
```

## 运行示例项目

Demo 里采用了 aar 的方式集成 SDK，您只需要配置完成 AppId，即可运行我们的 Demo。

## 联系我们

- 如果您在集成过程中遇到任何问题，可以随时与我们取得联系，mail: KwaiRTC@kuaishou.com
- 如果您发现了示例代码的 bug，欢迎提交issue
