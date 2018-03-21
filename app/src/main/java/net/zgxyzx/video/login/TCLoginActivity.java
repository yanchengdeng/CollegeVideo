package net.zgxyzx.video.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.Gson;

import net.zgxyzx.video.R;
import net.zgxyzx.video.common.ddzx.Constants;
import net.zgxyzx.video.common.ddzx.LoginUtils;
import net.zgxyzx.video.common.ddzx.activities.ForgetPasswordActivity;
import net.zgxyzx.video.common.ddzx.beans.ResultUserInfo;
import net.zgxyzx.video.common.ddzx.beans.VideoJsonRequset;
import net.zgxyzx.video.common.utils.TCUtils;
import net.zgxyzx.video.mainui.TCMainActivity;
import net.zgxyzx.video.userinfo.ITCUserInfoMgrListener;
import net.zgxyzx.video.userinfo.TCUserInfoMgr;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by RTMP on 2016/8/1
 */
public class TCLoginActivity extends Activity implements TCLoginMgr.TCLoginCallback {

    private static final String TAG = TCLoginActivity.class.getSimpleName();

    private TCLoginMgr mTCLoginMgr;

    //共用控件
    private LinearLayout rootRelativeLayout;

    private ProgressBar progressBar;

    private EditText etPassword;

    private EditText etLogin;

    private TextView btnLogin;

    private TextView tvRegister;

    private boolean bIsGuest = false; //是不是游客登录

    private ImageView ivHidePassword;

    private boolean isOpen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        rootRelativeLayout = (LinearLayout) findViewById(R.id.rl_login_root);

        mTCLoginMgr = TCLoginMgr.getInstance();
        etPassword = (EditText) findViewById(R.id.et_password);
        etLogin = (EditText) findViewById(R.id.et_login);
        ivHidePassword = (ImageView) findViewById(R.id.image_hide_pw);
        ivHidePassword.setSelected(isOpen);

        ((TextView) findViewById(R.id.tv_tittle)).setText("登录");
        findViewById(R.id.back).setVisibility(View.GONE);
        tvRegister = (TextView) findViewById(R.id.btn_register);

        btnLogin = (TextView) findViewById(R.id.btn_login);

        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        userNameLoginViewInit();

        //检测是否存在缓存
        if (TCUtils.isNetworkAvailable(this)) {
            mTCLoginMgr.setTCLoginCallback(this);
            //返回true表示存在本地缓存，进行登录操作，显示loadingFragment
            if (TCLoginMgr.getInstance().checkCacheAndLogin()) {
                OnProcessFragment loadinfFragment = new OnProcessFragment();
                loadinfFragment.show(getFragmentManager(), "");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //设置登录回调,resume设置回调避免被registerActivity冲掉
        mTCLoginMgr.setTCLoginCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //删除登录回调
        mTCLoginMgr.removeTCLoginCallback();
    }

    /**
     * 用户名密码登录界面init
     */
    public void userNameLoginViewInit() {


        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //注册界面 phoneView 与 normalView跳转逻辑一致
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), TCRegisterActivity.class);
                startActivity(intent);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //调用normal登录逻辑
                showOnLoading(true);
                attemptNormalLogin(etLogin.getText().toString(), etPassword.getText().toString());
                bIsGuest = false;

            }
        });

        findViewById(R.id.tv_forget_password).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(TCLoginActivity.this, ForgetPasswordActivity.class));
            }
        });


        findViewById(R.id.tv_change_pwstate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isOpen) {
                    //密文
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    isOpen = false;
                } else {
                    //明文
                    etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    isOpen = true;
                }
                ivHidePassword.setSelected(!isOpen);
            }
        });
    }

    /**
     * trigger loading模式
     *
     * @param active
     */
    public void showOnLoading(boolean active) {
        if (active) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.INVISIBLE);
            etLogin.setEnabled(false);
            etPassword.setEnabled(false);
            tvRegister.setClickable(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setVisibility(View.VISIBLE);
            etLogin.setEnabled(true);
            etPassword.setEnabled(true);
            tvRegister.setClickable(true);
            tvRegister.setTextColor(getResources().getColor(R.color.colorTransparentGray));
        }

    }

    public void showLoginError(String errorString) {
        ToastUtils.showShort(errorString);
        showOnLoading(false);
    }

    public void showPasswordError(String errorString) {
        ToastUtils.showShort(errorString);
        showOnLoading(false);
    }

    /**
     * 登录成功后被调用，跳转至TCMainActivity
     */
    public void jumpToHomeActivity() {
        Intent intent = new Intent(this, TCMainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 用户名密码登录
     *
     * @param username 用户名
     * @param password 密码
     */
    public void attemptNormalLogin(String username, String password) {

        if (TCUtils.isUsernameVaild(username)) {
            if (TCUtils.isPasswordValid(password)) {
                if (TCUtils.isNetworkAvailable(this)) {
                    //调用LoginHelper进行普通登录
//                    mTCLoginMgr.pwdLogin(username, password);
                    doLogin(username, password);
                } else {
                    Toast.makeText(getApplicationContext(), "当前无网络连接", Toast.LENGTH_SHORT).show();
                    showOnLoading(false);
                }
            } else {
                showPasswordError("密码长度应为6-16位");
            }
        } else {
            showLoginError("手机号码为空 或 格式错误");
        }
    }

    private void doLogin(String usename, String password) {
        JSONObject params = new JSONObject();
        try {
            params.put("mobile", usename);
            params.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.DO_LOGIN, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                LogUtils.w(jsonObject);

                ResultUserInfo resultInfo = new Gson().fromJson(jsonObject.toString(), ResultUserInfo.class);
                if (resultInfo.code == 1) {
//                    mTCLoginMgr.imLogin(resultInfo.data.nickname, resultInfo.data.sign);
                    LoginUtils.saveUserInofo(resultInfo);
                    startActivity(new Intent(TCLoginActivity.this, TCMainActivity.class));
                    showOnLoading(false);
                    finish();
                } else {
                    showOnLoading(false);
                    ToastUtils.showShort(resultInfo.msg);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ToastUtils.showShort(""+volleyError.getMessage());
                showOnLoading(false);
            }
        });
        Volley.newRequestQueue(TCLoginActivity.this).add(request);
    }


    /**
     * IMSDK登录成功
     */
    @Override
    public void onSuccess() {
        TCUserInfoMgr.getInstance().setUserId(mTCLoginMgr.getLastUserInfo().identifier, new ITCUserInfoMgrListener() {
            @Override
            public void OnQueryUserInfo(int error, String errorMsg) {
                // TODO: 16/8/10
            }

            @Override
            public void OnSetUserInfo(int error, String errorMsg) {
                if (0 != error)
                    Toast.makeText(getApplicationContext(), "设置 User ID 失败" + errorMsg, Toast.LENGTH_LONG).show();
            }
        });

        Toast.makeText(getApplicationContext(), "登录成功", Toast.LENGTH_SHORT).show();
        mTCLoginMgr.removeTCLoginCallback();
        showOnLoading(false);
        if (bIsGuest) {
//            showEditNicknameDia();
        } else {
            jumpToHomeActivity();
        }
    }

    /**
     * 失败
     *
     * @param errCode errCode
     * @param msg     msg
     */
    @Override
    public void onFailure(int errCode, String msg) {
        Log.d(TAG, "Login Error errCode:" + errCode + " msg:" + msg);
        showOnLoading(false);
        //被踢下线后弹窗显示被踢
        if (6208 == errCode) {
            TCUtils.showKickOutDialog(this);
        }
        Toast.makeText(getApplicationContext(), "登录失败" + msg, Toast.LENGTH_SHORT).show();

    }
}
