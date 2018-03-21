package net.zgxyzx.video.common.ddzx.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
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


//重置密码
public class ResettingPasswordActivity extends AppCompatActivity {

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
    @BindView(R.id.et_password)
    EditText etPassword;
    @BindView(R.id.et_password_reset)
    EditText etPasswordReset;
    @BindView(R.id.btn_login)
    TextView btnLogin;
    @BindView(R.id.progressbar)
    ProgressBar progressbar;
    @BindView(R.id.rl_login_root)
    LinearLayout rlLoginRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resetting_password);
        ButterKnife.bind(this);
        tvTittle.setText("修改密码");
    }

    @OnClick({R.id.back, R.id.btn_login})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.btn_login:
                checkParams(etPassword.getEditableText().toString(),etPasswordReset.getEditableText().toString());
                break;
        }
    }

    private void checkParams(String password, String rePassword) {
        if (TextUtils.isEmpty(password)){
            ToastUtils.showShort("请输入新密码");
        }else if (TextUtils.isEmpty(rePassword)){
            ToastUtils.showShort("请输入确认新密码");
        }else if (!TCUtils.isPasswordValid(password) ||  !TCUtils.isPasswordValid(rePassword)){
            ToastUtils.showShort("请输入6~16位密码");
        }else if (password.equals(rePassword)){
            doUpdateAction(password,rePassword);
        }else{
            ToastUtils.showShort("两次密码输入不一致");
        }
    }

    private void doUpdateAction(String password, String rePassword) {
        if (!LoginUtils.isLogin()){
            ToastUtils.showShort("重新登录");
            return;
        }
        JSONObject params = new JSONObject();
        try {
            params.put("token", LoginUtils.getUserInfo().data.token);
            params.put("password",password);
            params.put("re_password",rePassword);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.USER_RESET_PASSWORD, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
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
        Volley.newRequestQueue(ResettingPasswordActivity.this).add(request);
    }


}
