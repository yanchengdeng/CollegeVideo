package net.zgxyzx.video;


import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.blankj.utilcode.util.Utils;
import com.tencent.bugly.crashreport.CrashReport;
import com.tencent.rtmp.TXLiveBase;
import com.umeng.socialize.PlatformConfig;

import net.zgxyzx.video.common.utils.TCConstants;
import net.zgxyzx.video.common.utils.TCHttpEngine;
import net.zgxyzx.video.common.utils.TCLog;
import net.zgxyzx.video.im.TCIMInitMgr;

public class MyApplication extends MultiDexApplication {

    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Utils.init(this);
        //配置分享第三方平台的appkey
        PlatformConfig.setWeixin(TCConstants.WEIXIN_SHARE_ID, TCConstants.WEIXIN_SHARE_SECRECT);
        PlatformConfig.setSinaWeibo(TCConstants.SINA_WEIBO_SHARE_ID, TCConstants.SINA_WEIBO_SHARE_SECRECT, TCConstants.SINA_WEIBO_SHARE_REDIRECT_URL);
        PlatformConfig.setQQZone(TCConstants.QQZONE_SHARE_ID, TCConstants.QQZONE_SHARE_SECRECT);
//        initOkGo();
        initSDK();
    }


    /**
     * 初始化SDK，包括Bugly，IMSDK，RTMPSDK等
     */
    public void initSDK() {
        //启动bugly组件，bugly组件为腾讯提供的用于crash上报和分析的开放组件，如果您不需要该组件，可以自行移除
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(getApplicationContext());
        strategy.setAppVersion(TXLiveBase.getSDKVersionStr());
        CrashReport.initCrashReport(getApplicationContext(), TCConstants.BUGLY_APPID, true, strategy);

        TCIMInitMgr.init(getApplicationContext());

        //设置rtmpsdk log回调，将log保存到文件
        TXLiveBase.getInstance().listener = new TCLog(getApplicationContext());

        //初始化httpengine
        TCHttpEngine.getInstance().initContext(getApplicationContext());

        Log.w("TCLog","app init sdk");
    }


    public static MyApplication getApplication() {
        return instance;
    }
}
