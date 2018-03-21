package net.zgxyzx.video.mainui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import net.zgxyzx.video.R;
import net.zgxyzx.video.common.ddzx.LoginUtils;
import net.zgxyzx.video.login.TCLoginActivity;

/**
 * Created by RTMP on 2016/8/1
 */
public class TCSplashActivity extends Activity /*implements TCLoginMgr.TCLoginCallback*/ {

    private static final String TAG = TCSplashActivity.class.getSimpleName();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (LoginUtils.isLogin()) {
                    jumpToMainActivity();
                }else{
                    jumpToLoginActivity();
                }
            }
        },1500);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //login();
    }

    @Override
    public void onBackPressed() {
        //splashActivity下不允许back键退出
        //super.onBackPressed();
    }

//    public void login() {
//        //判断网络环境
//        if(TCUtils.isNetworkAvailable(this)) {
//            TCLoginMgr.getInstance().checkCacheAndLogin();
//        } else {
//            //无网状态下转入登录界面
//            TCLoginMgr.getInstance().removeTCLoginCallback();
//            jumpToLoginActivity();
//        }
//    }

    private void jumpToLoginActivity() {
        Intent intent = new Intent(this, TCLoginActivity.class);
        startActivity(intent);
        finish();
    }


    private void jumpToMainActivity() {
        Intent intent = new Intent(this, TCMainActivity.class);
        startActivity(intent);
        finish();
    }



    //为了便于阅读，将登录态检测的方法置于TCLoginActivity
//    private void jumpToMainActivity() {
//        Intent intent = new Intent(this, TCMainActivity.class);
//        startActivity(intent);
//        finish();
//    }

//    /**
//     * IMSDK登录成功
//     */
//    @Override
//    public void onSuccess() {
//        //登录成功跳转至主界面
//        Log.d(TAG, "already has cache, jump into login activity");
//        TCUserInfoMgr.getInstance().setUserId(TCLoginMgr.getInstance().getLastUserInfo().identifier, null);
//        TCLoginMgr.getInstance().removeTCLoginCallback();
//        jumpToMainActivity();
//    }
//
//    /**
//     * IMSDK登录失败
//     *
//     * @param code 错误码
//     * @param msg  错误信息
//     */
//    @Override
//    public void onFailure(int code, String msg) {
//        Log.d(TAG, "already has cache, but imsdk login fail");
//        //登录失败，跳转至login界面
//        TCLoginMgr.getInstance().removeTCLoginCallback();
//        jumpToLoginActivity();
//    }
}
