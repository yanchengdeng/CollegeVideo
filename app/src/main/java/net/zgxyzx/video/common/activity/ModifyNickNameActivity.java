package net.zgxyzx.video.common.activity;

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
import net.zgxyzx.video.common.ddzx.beans.ResultUserInfo;
import net.zgxyzx.video.common.ddzx.beans.VideoJsonRequset;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ModifyNickNameActivity extends AppCompatActivity {

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
    @BindView(R.id.et_nick_name)
    EditText etNickName;
    @BindView(R.id.iv_close)
    ImageView ivClose;
    @BindView(R.id.btn_login)
    TextView btnLogin;
    @BindView(R.id.progressbar)
    ProgressBar progressbar;
    @BindView(R.id.rl_login_root)
    LinearLayout rlLoginRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modify_nick_name);
        ButterKnife.bind(this);
        tvTittle.setText("修改昵称");
        ResultUserInfo resultUserInfo = LoginUtils.getUserInfo();
        if (resultUserInfo!=null){
            etNickName.setText(resultUserInfo.data.nickname);
        }
    }

    @OnClick({R.id.back, R.id.iv_close, R.id.btn_login})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.iv_close:
                etNickName.setText("");
                etNickName.setHint("昵称");
                break;
            case R.id.btn_login:
                doCheckNickName(etNickName.getEditableText().toString());
                break;
        }
    }


    //验证
    private void doCheckNickName(final String nickname) {
        if (TextUtils.isEmpty(nickname)) {
            ToastUtils.showShort("请输入昵称");
        } else {
            JSONObject params = new JSONObject();
            try {
                params.put("token", LoginUtils.getUserInfo().data.token);
                params.put("nick_name", nickname);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.INDEX_CHECKNICK, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    LogUtils.w(jsonObject);

                    BaseInfo baseInfo = new Gson().fromJson(jsonObject.toString(), BaseInfo.class);
                    if (baseInfo.code == 1) {
                        doModifyNickName(nickname);
                    } else {
                        ToastUtils.showShort(baseInfo.msg);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    ToastUtils.showShort("" + volleyError.getMessage());
                }
            });
            Volley.newRequestQueue(ModifyNickNameActivity.this).add(request);
        }
    }


    //修改
    private void doModifyNickName(final String nickname) {
        if (TextUtils.isEmpty(nickname)) {
            ToastUtils.showShort("请输入昵称");
        } else {
            JSONObject params = new JSONObject();
            try {
                params.put("token", LoginUtils.getUserInfo().data.token);
                params.put("nick_name", nickname);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.INDEX_UPDATENICK, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    LogUtils.w(jsonObject);

                    BaseInfo baseInfo = new Gson().fromJson(jsonObject.toString(), BaseInfo.class);
                    if (baseInfo.code == 1) {
                        ToastUtils.showShort("修改成功");
                        ResultUserInfo userInfo = LoginUtils.getUserInfo();
                        userInfo.data.nickname = nickname;
                        LoginUtils.saveUserInofo(userInfo);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 1500);

                    } else {
                        ToastUtils.showShort(baseInfo.msg);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    ToastUtils.showShort("" + volleyError.getMessage());
                }
            });
            Volley.newRequestQueue(ModifyNickNameActivity.this).add(request);
        }
    }
}
