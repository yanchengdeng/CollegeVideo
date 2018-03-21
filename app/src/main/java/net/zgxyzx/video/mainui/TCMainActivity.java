package net.zgxyzx.video.mainui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.Gson;
import com.tencent.TIMManager;

import net.zgxyzx.video.R;
import net.zgxyzx.video.common.ddzx.Constants;
import net.zgxyzx.video.common.ddzx.LoginUtils;
import net.zgxyzx.video.common.ddzx.beans.BaseInfo;
import net.zgxyzx.video.common.ddzx.beans.ResultUserInfo;
import net.zgxyzx.video.common.ddzx.beans.VideoJsonRequset;
import net.zgxyzx.video.common.utils.TCConstants;
import net.zgxyzx.video.common.utils.TCUtils;
import net.zgxyzx.video.common.widget.PublisherDialogFragment;
import net.zgxyzx.video.login.TCLoginActivity;
import net.zgxyzx.video.login.TCLoginMgr;
import net.zgxyzx.video.mainui.list.TCLiveListFragment;
import net.zgxyzx.video.userinfo.TCUserInfoFragment;
import net.zgxyzx.video.userinfo.TCUserInfoMgr;

import org.json.JSONException;
import org.json.JSONObject;

import tencent.tls.platform.TLSUserInfo;

/**
 * 主界面，包括直播列表，用户信息页
 * UI使用FragmentTabHost+Fragment
 * 直播列表：TCLiveListFragment
 * 个人信息页：TCUserInfoFragment
 */
public class TCMainActivity extends FragmentActivity {
    private static final String TAG = TCMainActivity.class.getSimpleName();

    //被踢下线广播监听
    private LocalBroadcastManager mLocalBroadcatManager;
    private BroadcastReceiver mExitBroadcastReceiver;

    private FragmentTabHost mTabHost;
    private LayoutInflater mLayoutInflater;
    private final Class mFragmentArray[] = {TCLiveListFragment.class, TCLiveListFragment.class, TCUserInfoFragment.class};
    private int mImageViewArray[] = {R.drawable.tab_video, R.drawable.tab_live, R.drawable.tab_user};
    private String mTextviewArray[] = {"首页", "直播", "我的"};
    private PublisherDialogFragment mPublisherDialogFragment;
    private long mLastClickPubTS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mLayoutInflater = LayoutInflater.from(this);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.contentPanel);

        int fragmentCount = mFragmentArray.length;
        for (int i = 0; i < fragmentCount; i++) {
            TabHost.TabSpec tabSpec = mTabHost.newTabSpec(mTextviewArray[i]).setIndicator(getTabItemView(i));
            mTabHost.addTab(tabSpec, mFragmentArray[i], null);
            mTabHost.getTabWidget().setDividerDrawable(null);
        }

        mPublisherDialogFragment = new PublisherDialogFragment();
        mTabHost.getTabWidget().getChildTabViewAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LoginUtils.getUserInfo()!=null){
                    ResultUserInfo userInfo = LoginUtils.getUserInfo();
                    chechIsZB(userInfo.data.token);
                }else{
                    startActivity(new Intent(TCMainActivity.this,TCLoginActivity.class));
                    finish();
                    return;
                }
            }
        });

        mLocalBroadcatManager = LocalBroadcastManager.getInstance(this);
        mExitBroadcastReceiver = new ExitBroadcastRecevier();
        mLocalBroadcatManager.registerReceiver(mExitBroadcastReceiver, new IntentFilter(TCConstants.EXIT_APP));

        Log.w("TCLog", "mainactivity oncreate");


        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }
    }

    private void chechIsZB(String token) {

        JSONObject params = new JSONObject();
        try {
            params.put("token",token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.INDEX_CHECKROLE, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                LogUtils.w(jsonObject);

                BaseInfo baseInfo = new Gson().fromJson(jsonObject.toString(), BaseInfo.class);
                if (baseInfo.code == 1) {
                    if (System.currentTimeMillis() - mLastClickPubTS > 1000) {
                        mLastClickPubTS = System.currentTimeMillis();
                        if (mPublisherDialogFragment.isAdded())
                            mPublisherDialogFragment.dismiss();
                        else
                            mPublisherDialogFragment.show(getFragmentManager(), "");
                    }
                } else {
                    ToastUtils.showShort(baseInfo.msg);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (!NetworkUtils.isConnected()){
                    ToastUtils.showShort("检查网络");
                }else{
                    ToastUtils.showShort(""+volleyError.getMessage());
                }
            }
        });
        Volley.newRequestQueue(TCMainActivity.this).add(request);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.w("TCLog", "mainactivity onstart");
        if (TextUtils.isEmpty(TIMManager.getInstance().getLoginUser())) {
            //relogin
            final TCLoginMgr tcLoginMgr = TCLoginMgr.getInstance();
            final TLSUserInfo userInfo = TCLoginMgr.getInstance().getLastUserInfo();
            tcLoginMgr.setTCLoginCallback(new TCLoginMgr.TCLoginCallback() {
                @Override
                public void onSuccess() {
                    tcLoginMgr.removeTCLoginCallback();
                    TCUserInfoMgr.getInstance().setUserId(userInfo.identifier, null);
                }

                @Override
                public void onFailure(int code, String msg) {
                    tcLoginMgr.removeTCLoginCallback();
                }
            });

            tcLoginMgr.checkCacheAndLogin();
            Log.w("TCLog", "mainactivity onstart relogin");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcatManager.unregisterReceiver(mExitBroadcastReceiver);
    }

    public class ExitBroadcastRecevier extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TCConstants.EXIT_APP)) {
                onReceiveExitMsg();
            }
        }
    }

    public void onReceiveExitMsg() {
        TCUtils.showKickOutDialog(this);
    }

    /**
     * 动态获取tabicon
     *
     * @param index tab index
     * @return
     */
    private View getTabItemView(int index) {
        View view;
        view = mLayoutInflater.inflate(R.layout.tab_button, null);
        ImageView icon = (ImageView) view.findViewById(R.id.tab_icon);
        icon.setImageResource(mImageViewArray[index]);
        TextView tvName = (TextView) view.findViewById(R.id.tv_tab_name);
        tvName.setText(mTextviewArray[index]);
        return view;
    }


    @Override
    public void onBackPressed() {

        doLogOut();

    }

    private void doLogOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(TCMainActivity.this);
        builder.setTitle("提示");
        builder.setMessage("确定要退出课睿直播吗？");

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();

    }

}
