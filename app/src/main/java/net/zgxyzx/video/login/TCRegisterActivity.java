package net.zgxyzx.video.login;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
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
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ViewTarget;
import com.google.gson.Gson;
import com.tencent.rtmp.TXLog;

import net.zgxyzx.video.R;
import net.zgxyzx.video.common.ddzx.Constants;
import net.zgxyzx.video.common.ddzx.LoginUtils;
import net.zgxyzx.video.common.ddzx.beans.BaseInfo;
import net.zgxyzx.video.common.ddzx.beans.ResultUserInfo;
import net.zgxyzx.video.common.ddzx.beans.VideoJsonRequset;
import net.zgxyzx.video.common.utils.TCUtils;
import net.zgxyzx.video.mainui.TCMainActivity;
import net.zgxyzx.video.userinfo.TCUserInfoMgr;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by RTMP on 2016/8/1
 */
public class TCRegisterActivity extends Activity implements TCRegisterMgr.TCRegisterCallback {

    public static final String TAG = TCRegisterActivity.class.getSimpleName();

    private TCRegisterMgr mTCRegisterMgr;

    private String mPassword;

    //共用控件
    private LinearLayout relativeLayout;

    private ProgressBar progressBar;

    private EditText etPassword;

    private EditText etCode;


    private EditText etRegister, etNickName;


    private TextView btnRegister;

    private TextView tvGetcode;

    private CountDownTimer countDownTimer;
    //动画
    AlphaAnimation fadeInAnimation, fadeOutAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tvGetcode = (TextView) findViewById(R.id.tv_getcode);
        etCode = (EditText) findViewById(R.id.et_code);
        relativeLayout = (LinearLayout) findViewById(R.id.rl_register_root);
        countDownTimer = new CountDownTimer(60 * 1000, 1000) {
            @Override
            public void onTick(long l) {
                tvGetcode.setText(String.format(getString(R.string.seconds_later_restart), ((int) l / 1000)));
                tvGetcode.setClickable(false);
            }

            @Override
            public void onFinish() {
                tvGetcode.setClickable(true);
                tvGetcode.setText(getString(R.string.send_msg));
                countDownTimer.cancel();
            }
        };
        if (null != relativeLayout) {
            ViewTarget<LinearLayout, GlideDrawable> viewTarget = new ViewTarget<LinearLayout, GlideDrawable>(relativeLayout) {
                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    this.view.setBackgroundDrawable(resource.getCurrent());
                }
            };

            Glide.with(getApplicationContext()) // safer!
                    .load(R.color.white)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(viewTarget);
        }


        etRegister = (EditText) findViewById(R.id.et_register);

        etPassword = (EditText) findViewById(R.id.et_password);


        btnRegister = (TextView) findViewById(R.id.btn_register);

        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        etNickName = (EditText) findViewById(R.id.et_nick_name);


        ((TextView) findViewById(R.id.tv_tittle)).setText("注册");
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mTCRegisterMgr = TCRegisterMgr.getInstance();
        mTCRegisterMgr.setTCRegisterCallback(this);

        fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
        fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeInAnimation.setDuration(250);
        fadeOutAnimation.setDuration(250);

        LayoutTransition layoutTransition = new LayoutTransition();
        relativeLayout.setLayoutTransition(layoutTransition);

        findViewById(R.id.tv_getcode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(etRegister.getEditableText().toString())) {
                    ToastUtils.showShort("请输入手机号");
                } else if (etRegister.getEditableText().length() != 11) {
                    ToastUtils.showShort("手机号格式错误");
                } else {
                    sendCode(etRegister.getEditableText().toString());
                }
            }
        });

    }

    private void sendCode(String phone) {
        JSONObject params = new JSONObject();
        try {
            params.put("mobile", phone);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.GET_CODE, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                LogUtils.w(jsonObject);
                BaseInfo baseInfo = new Gson().fromJson(jsonObject.toString(),BaseInfo.class);

                if (baseInfo.code==1) {
                    countDownTimer.start();
                    ToastUtils.showShort("发送成功");
                }else{
                    ToastUtils.showShort(baseInfo.msg);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ToastUtils.showShort("" + volleyError.getMessage());
            }
        });
        Volley.newRequestQueue(TCRegisterActivity.this).add(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        userNameRegisterViewInit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void showOnLoading(boolean active) {
        if (active) {
            progressBar.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.INVISIBLE);
            etPassword.setEnabled(false);
            etRegister.setEnabled(false);
            btnRegister.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnRegister.setVisibility(View.VISIBLE);
            etPassword.setEnabled(true);
            etRegister.setEnabled(true);
            btnRegister.setEnabled(true);
        }

    }

    private void userNameRegisterViewInit() {

        etRegister.setText("");
        etRegister.setError(null, null);


        etPassword.setText("");
        etPassword.setError(null, null);


        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //调用normal注册逻辑
                attemptNormalRegist(etRegister.getText().toString(), etCode.getEditableText().toString(),
                        etPassword.getText().toString());
            }
        });


    }

    private void showRegistError(String errorString) {
        etRegister.setError(errorString);
        showOnLoading(false);
    }

    private void showPasswordVerifyError(String errorString) {
        etPassword.setError(errorString);
        showOnLoading(false);
    }

    private void attemptNormalRegist(String username, String code, String password) {

        if (TCUtils.isUsernameVaild(username)) {
            if (TCUtils.isNetworkAvailable(this)) {
                if (TextUtils.isEmpty(code)) {
                    ToastUtils.showShort("请输入验证码");
                } else if (TextUtils.isEmpty(etNickName.getEditableText().toString())) {
                    ToastUtils.showShort("轻输入昵称");
                } else if (!TCUtils.isPasswordValid(password)) {
                    showPasswordVerifyError("密码长度应为6-16位");
                } else {
                    mPassword = password;
//                        mTCRegisterMgr.pwdRegist(username, password);
                    doRegister(username, etNickName.getEditableText().toString(), code, password);
                    showOnLoading(true);
                }
            } else {
                Toast.makeText(getApplicationContext(), "当前无网络连接", Toast.LENGTH_SHORT).show();
            }

        } else {
            showRegistError("手机号不规范");
        }
    }

    private void doRegister(String username, String nickName, String code, String password) {
        JSONObject params = new JSONObject();
        try {
            params.put("mobile", username);
            params.put("nick_name", nickName);
            params.put("password", password);
            params.put("code", code);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.DO_REGISTER, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                LogUtils.w(jsonObject);

                ResultUserInfo resultInfo = new Gson().fromJson(jsonObject.toString(), ResultUserInfo.class);
                if (resultInfo.code == 1) {
                    showOnLoading(false);
                    ToastUtils.showShort("注册成功");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 1500);
                } else {
                    showOnLoading(false);
                    ToastUtils.showShort(resultInfo.msg);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (!TextUtils.isEmpty(volleyError.getMessage())) {
                    ToastUtils.showShort(volleyError.getMessage());
                }

                showOnLoading(false);
            }
        });
        Volley.newRequestQueue(TCRegisterActivity.this).add(request);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTCRegisterMgr.removeTCRegisterCallback();
    }

    private void jumpToHomeActivity() {
        Intent intent = new Intent(this, TCMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * 注册成功
     * 成功后直接登录
     *
     * @param identifier id
     */
    @Override
    public void onSuccess(final String identifier) {
        //自动登录逻辑
        final TCLoginMgr tcLoginMgr = TCLoginMgr.getInstance();
        tcLoginMgr.setTCLoginCallback(new TCLoginMgr.TCLoginCallback() {
            @Override
            public void onSuccess() {
                tcLoginMgr.removeTCLoginCallback();
                TCUserInfoMgr.getInstance().setUserId(identifier, null);
                Log.d(TAG, "login after regist success");
                Toast.makeText(getApplicationContext(), "自动登录成功", Toast.LENGTH_SHORT).show();
                jumpToHomeActivity();
            }

            @Override
            public void onFailure(int code, String msg) {
                tcLoginMgr.removeTCLoginCallback();
                TXLog.d(TAG, "login after regist fail, code:" + code + " msg:" + msg);
                Toast.makeText(getApplicationContext(), "自动登录失败", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        //根据mPassword判断登录类型采取不同的登录方式（sms or password）
        if (!TextUtils.isEmpty(mPassword))
            tcLoginMgr.pwdLogin(identifier, mPassword);
        else
            tcLoginMgr.smsLogin(identifier);
        Toast.makeText(getApplicationContext(), "成功注册" + identifier, Toast.LENGTH_SHORT).show();
        mTCRegisterMgr.removeTCRegisterCallback();
    }

    /**
     * 注册失败
     *
     * @param code 错误码
     * @param msg  错误信息
     */
    @Override
    public void onFailure(int code, String msg) {
        Log.d(TAG, "regist fail, code:" + code + " msg:" + msg);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        showOnLoading(false);
    }
}
