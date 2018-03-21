package net.zgxyzx.video.common.ddzx.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import net.zgxyzx.video.common.ddzx.beans.BaseInfo;
import net.zgxyzx.video.common.ddzx.beans.VideoJsonRequset;
import net.zgxyzx.video.common.utils.TCUtils;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

//忘记密码
public class ForgetPasswordActivity extends AppCompatActivity {

    @BindView(R.id.back)
    ImageView back;
    @BindView(R.id.tv_tittle)
    TextView tvTittle;
    @BindView(R.id.iv_right)
    ImageView ivRight;
    @BindView(R.id.tv_right)
    TextView tvRight;
    @BindView(R.id.rl_common_bar)
    RelativeLayout rlCommonBar;
    @BindView(R.id.et_login)
    EditText etLogin;
    @BindView(R.id.et_code)
    EditText etCode;
    @BindView(R.id.tv_getcode)
    TextView tvGetcode;
    @BindView(R.id.et_password)
    EditText etPassword;
    @BindView(R.id.image_hide_pw)
    ImageView imageHidePw;
    @BindView(R.id.tv_change_pwstate)
    LinearLayout tvChangePwstate;
    @BindView(R.id.btn_login)
    TextView btnLogin;
    @BindView(R.id.progressbar)
    ProgressBar progressbar;
    @BindView(R.id.rl_login_root)
    LinearLayout rlLoginRoot;

    private CountDownTimer countDownTimer;
    private boolean isOpen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);
        ButterKnife.bind(this);
        tvTittle.setText("忘记密码");
        imageHidePw.setSelected(isOpen);
        countDownTimer = new CountDownTimer(60*1000,1000) {
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
    }

    @OnClick({R.id.back, R.id.tv_getcode, R.id.image_hide_pw,R.id.btn_login})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.tv_getcode:
                if (TextUtils.isEmpty(etLogin.getEditableText().toString())) {
                    ToastUtils.showShort("请输入手机号");
                } else if (etLogin.getEditableText().toString().length() != 11) {
                    ToastUtils.showShort("手机号码格式错误");
                } else {
                    doGetCode(etLogin.getEditableText().toString().trim());
                }
                break;
            case R.id.btn_login:
                if (TextUtils.isEmpty(etLogin.getEditableText().toString())) {
                    ToastUtils.showShort("请输入手机号");
                } else if (etLogin.getEditableText().toString().length() != 11) {
                    ToastUtils.showShort("手机号码格式错误");
                } else if(TextUtils.isEmpty(etCode.getEditableText().toString())) {
                    ToastUtils.showShort("请输入验证码");
                }else if (!TCUtils.isPasswordValid(etPassword.getEditableText().toString())){
                    ToastUtils.showShort("密码格式错误");
                }else{
                doLogin(etLogin.getEditableText().toString(),etCode.getEditableText().toString(),etPassword.getEditableText().toString());}
                break;
            case R.id.tv_change_pwstate:
                if (isOpen) {
                    //密文
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    isOpen = false;
                } else {
                    //明文
                    etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    isOpen = true;
                }
                imageHidePw.setSelected(!isOpen);
                break;
        }
    }

    //修改密码
    private void doLogin(String phone, String code, String password) {
        JSONObject params = new JSONObject();
        try {
            params.put("mobile", phone);
            params.put("password",password);
            params.put("code",code);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.USER_RESETPASSWORD, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                LogUtils.w(jsonObject);

                BaseInfo baseInfo = new Gson().fromJson(jsonObject.toString(),BaseInfo.class);
                if (baseInfo.code==1) {
                    ToastUtils.showShort("修改成功");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 1500);
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
        Volley.newRequestQueue(ForgetPasswordActivity.this).add(request);
    }

    private void doGetCode(String phone) {
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
        Volley.newRequestQueue(ForgetPasswordActivity.this).add(request);
    }
}
