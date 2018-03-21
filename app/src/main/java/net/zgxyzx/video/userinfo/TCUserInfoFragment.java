package net.zgxyzx.video.userinfo;


import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.qiniu.android.common.AutoZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.tencent.TIMManager;
import com.tencent.rtmp.TXLiveBase;

import net.zgxyzx.video.R;
import net.zgxyzx.video.common.activity.ModifyNickNameActivity;
import net.zgxyzx.video.common.ddzx.Constants;
import net.zgxyzx.video.common.ddzx.LoginUtils;
import net.zgxyzx.video.common.ddzx.activities.ResettingPasswordActivity;
import net.zgxyzx.video.common.ddzx.beans.BaseInfo;
import net.zgxyzx.video.common.ddzx.beans.ImageToken;
import net.zgxyzx.video.common.ddzx.beans.ResultUserInfo;
import net.zgxyzx.video.common.ddzx.beans.VideoJsonRequset;
import net.zgxyzx.video.common.utils.TCConstants;
import net.zgxyzx.video.common.utils.TCGlideCircleTransform;
import net.zgxyzx.video.common.utils.TCUtils;
import net.zgxyzx.video.common.widget.TCLineControllerView;
import net.zgxyzx.video.login.TCLoginActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.Unbinder;

import static android.app.Activity.RESULT_OK;

/**
 * 用户资料展示页面
 */
public class TCUserInfoFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "TCUserInfoFragment";
    @BindView(R.id.iv_ui_head)
    ImageView ivUiHead;
    @BindView(R.id.tv_ui_nickname)
    TextView tvUiNickname;
    @BindView(R.id.tv_ui_user_id)
    TextView tvUiUserId;
    @BindView(R.id.rl_user_info)
    RelativeLayout rlUserInfo;
    @BindView(R.id.tv_nick_name)
    TextView tvNickName;
    @BindView(R.id.ll_user_nick)
    LinearLayout llUserNick;
    @BindView(R.id.tv_password)
    TextView tvPassword;
    @BindView(R.id.ll_password)
    LinearLayout llPassword;
    @BindView(R.id.lcv_ui_set)
    TCLineControllerView lcvUiSet;
    @BindView(R.id.lcv_ui_version)
    TCLineControllerView lcvUiVersion;
    @BindView(R.id.lcv_ui_logout)
    TextView lcvUiLogout;
    Unbinder unbinder;
    private ImageView mHeadPic;
    private TextView mNickName;
    private TextView mUserId;
    private TextView mBtnLogout;
    private TCLineControllerView mBtnSet;
    private TCLineControllerView mVersion;

    private static final int CAPTURE_IMAGE_CAMERA = 100;
    private static final int IMAGE_STORE = 200;

    private static final int CROP_CHOOSE = 10;
    private boolean mUploading = false;
    private boolean mPermission = false;

    private ProgressBar progressBar;

    public TCUserInfoFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_user_info, container, false);
        mHeadPic = (ImageView) view.findViewById(R.id.iv_ui_head);
        mNickName = (TextView) view.findViewById(R.id.tv_ui_nickname);
        mUserId = (TextView) view.findViewById(R.id.tv_ui_user_id);
        mBtnSet = (TCLineControllerView) view.findViewById(R.id.lcv_ui_set);
        mBtnLogout = (TextView) view.findViewById(R.id.lcv_ui_logout);
        mVersion = (TCLineControllerView) view.findViewById(R.id.lcv_ui_version);
        llUserNick = (LinearLayout) view.findViewById(R.id.ll_user_nick);
        llPassword = (LinearLayout) view.findViewById(R.id.ll_password);
        tvNickName = (TextView) view.findViewById(R.id.tv_nick_name);
        ivUiHead = (ImageView) view.findViewById(R.id.iv_ui_head);
        mBtnSet.setOnClickListener(this);
        mBtnLogout.setOnClickListener(this);
        mVersion.setOnClickListener(this);
        view.findViewById(R.id.back).setVisibility(View.GONE);
        ((TextView)view.findViewById(R.id.tv_tittle)).setText("我的");
        llUserNick.setOnClickListener(this);
        llPassword.setOnClickListener(this);
        progressBar = (ProgressBar) view.findViewById(R.id.progressbar);

        ResultUserInfo userInfo = LoginUtils.getUserInfo();
        if (userInfo != null) {
            if (!TextUtils.isEmpty(userInfo.data.nickname)) {
                tvNickName.setText(userInfo.data.nickname);
            }

            if (!TextUtils.isEmpty(userInfo.data.mobile)) {
                mNickName.setText(userInfo.data.mobile);
            }

            if (!TextUtils.isEmpty(userInfo.data.headpic)) {
                Glide.with(getActivity()).load(userInfo.data.headpic).bitmapTransform(new TCGlideCircleTransform(getActivity())).into(ivUiHead);
            }
        }





        mPermission = checkPublishPermission();


        ivUiHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initPhotoDialog();
            }
        });
        return view;
    }

    private Dialog mPicChsDialog;

    /**
     * 图片选择对话框
     */
    private void initPhotoDialog() {
        mPicChsDialog = new Dialog(getActivity(), R.style.floag_dialog);
        mPicChsDialog.setContentView(R.layout.dialog_pic_choose);

        WindowManager windowManager = getActivity().getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        Window dlgwin = mPicChsDialog.getWindow();
        WindowManager.LayoutParams lp = dlgwin.getAttributes();
        dlgwin.setGravity(Gravity.BOTTOM);
        lp.width = (int) (display.getWidth()); //设置宽度

        mPicChsDialog.getWindow().setAttributes(lp);

        TextView camera = (TextView) mPicChsDialog.findViewById(R.id.chos_camera);
        TextView picLib = (TextView) mPicChsDialog.findViewById(R.id.pic_lib);
        TextView cancel = (TextView) mPicChsDialog.findViewById(R.id.btn_cancel);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPicFrom(CAPTURE_IMAGE_CAMERA);
                mPicChsDialog.dismiss();
            }
        });

        picLib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPicFrom(IMAGE_STORE);
                mPicChsDialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPicChsDialog.dismiss();
            }
        });

        mPicChsDialog.show();
    }

    private Uri fileUri, cropUri;

    /**
     * 获取图片资源
     *
     * @param type 类型（本地IMAGE_STORE/拍照CAPTURE_IMAGE_CAMERA）
     */
    private void getPicFrom(int type) {
        if (!mPermission) {
            Toast.makeText(getActivity(), getString(R.string.tip_no_permission), Toast.LENGTH_SHORT).show();
            return;
        }

        switch (type) {
            case CAPTURE_IMAGE_CAMERA:
                fileUri = createCoverUri("");
                Intent intent_photo = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent_photo.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(intent_photo, CAPTURE_IMAGE_CAMERA);
                break;
            case IMAGE_STORE:
                fileUri = createCoverUri("_select");
                Intent intent_album = new Intent("android.intent.action.GET_CONTENT");
                intent_album.setType("image/*");
                startActivityForResult(intent_album, IMAGE_STORE);
                break;

        }
    }

    private boolean checkPublishPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
//            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(TCPublishSettingActivity.this, Manifest.permission.CAMERA)) {
//                permissions.add(Manifest.permission.CAMERA);
//            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE)) {
                permissions.add(Manifest.permission.READ_PHONE_STATE);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(getActivity(),
                        permissions.toArray(new String[0]),
                        TCConstants.WRITE_PERMISSION_REQ_CODE);
                return false;
            }
        }

        return true;
    }

    private boolean checkScrRecordPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private Uri createCoverUri(String type) {
        String filename = TCUserInfoMgr.getInstance().getUserId() + type + ".jpg";
        String path = Environment.getExternalStorageDirectory() + "/yunzhibo";

        File outputImage = new File(path, filename);
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, TCConstants.WRITE_PERMISSION_REQ_CODE);
            return null;
        }
        try {
            File pathFile = new File(path);
            if (!pathFile.exists()) {
                pathFile.mkdirs();
            }
            if (outputImage.exists()) {
                outputImage.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "生成封面失败", Toast.LENGTH_SHORT).show();
        }

        return Uri.fromFile(outputImage);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAPTURE_IMAGE_CAMERA:
                    startPhotoZoom(fileUri);
                    break;
                case IMAGE_STORE:
                    String path = TCUtils.getPath(getActivity(), data.getData());
                    if (null != path) {
                        File file = new File(path);
                        getToken(file);

//                        startPhotoZoom(Uri.fromFile(file));
                    }
                    break;
                case CROP_CHOOSE:
                    mUploading = true;
                    File file = new File(cropUri.getPath());
                    getToken(file);
                    break;

            }
        }

    }

    public void startPhotoZoom(Uri uri) {
        cropUri = createCoverUri("_crop");

        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File file = new File(uri.getPath());
            uri = FileProvider.getUriForFile(getActivity(), "net.zgxyzx.video.fileprovider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 750);
        intent.putExtra("aspectY", 550);
        intent.putExtra("outputX", 750);
        intent.putExtra("outputY", 550);
        intent.putExtra("scale", true);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        startActivityForResult(intent, CROP_CHOOSE);
    }


    private void getToken(final File fileFile) {
        progressBar.setVisibility(View.VISIBLE);
        StringRequest stringRequest = new StringRequest(Constants.GET_QINIU_IMAGE_TOKEN, new Response.Listener<String>() {
            @Override
            public void onResponse(String jsonObject) {
                ImageToken imageToken = new Gson().fromJson(jsonObject, ImageToken.class);
                if (!TextUtils.isEmpty(imageToken.getUptoken())) {
                    getDownloadImages(imageToken.getUptoken(), fileFile);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                LogUtils.w("dyc", volleyError);
                progressBar.setVisibility(View.GONE);
            }
        });
        Volley.newRequestQueue(getActivity()).add(stringRequest);

    }

    String imageurl;

    private void getDownloadImages(String imageTokenContent, File file) {
        //自动识别上传区域
        final Configuration config = new Configuration.Builder()
                .zone(AutoZone.autoZone)
                .build();
        UploadManager uploadManager = new UploadManager(config);

        File data = file;
        String key = null;
        String token = imageTokenContent;
        uploadManager.put(data, key, token,
                new UpCompletionHandler() {
                    @Override
                    public void complete(String key, final ResponseInfo info, final JSONObject res) {
                        //res包含hash、key等信息，具体字段取决于上传策略的设置

                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                                progressBar.setVisibility(View.GONE);
                                if (info.isOK()) {
                                    Log.i("qiniu", "Upload Success");
                                    if (res != null) {
                                        imageurl = Constants.SERVER_IMAGE_URL + res.optString("hash");
                                        Glide.with(getActivity()).load(imageurl).bitmapTransform(new TCGlideCircleTransform(getActivity())).into(ivUiHead);
                                        LogUtils.w("dyc", imageurl);
                                        uploadImage(imageurl);

                                    }
                                } else {
                                    ToastUtils.showShort("稍后再试");
                                }
                            }
                        });
                        Log.w("dyc", key + ",\r\n " + info + ",\r\n " + res);
                    }
                }, null);
    }

    //上传头像
    private void uploadImage(final String imageurl) {

        JSONObject params = new JSONObject();
        try {
            params.put("token", LoginUtils.getUserInfo().data.token);
            params.put("url", imageurl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        VideoJsonRequset videoJsonRequset = new VideoJsonRequset(Request.Method.POST, Constants.INDEX_HEADPIC, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                progressBar.setVisibility(View.GONE);
                BaseInfo baseInfo = new Gson().fromJson(jsonObject.toString(), BaseInfo.class);
                if (baseInfo.code == 1) {
                    ToastUtils.showShort("上传成功");
                    ResultUserInfo userInfo = LoginUtils.getUserInfo();
                    if (userInfo!=null){
                        userInfo.data.headpic = imageurl;
                        LoginUtils.saveUserInofo(userInfo);
                    }
                } else {
                    ToastUtils.showShort(baseInfo.msg);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ToastUtils.showShort("稍后再试");
                progressBar.setVisibility(View.GONE);
            }
        });

        Volley.newRequestQueue(getActivity()).add(videoJsonRequset);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        //页面展示之前，更新一下用户信息
        TCUserInfoMgr.getInstance().queryUserInfo(new ITCUserInfoMgrListener() {
            @Override
            public void OnQueryUserInfo(int error, String errorMsg) {
                if (0 == error) {
//                    mNickName.setText(TCUserInfoMgr.getInstance().getNickname());
                    mUserId.setText("ID:" + TCUserInfoMgr.getInstance().getUserId());
                    TCUtils.showPicWithUrl(getActivity(), mHeadPic, TCUserInfoMgr.getInstance().getHeadPic(), R.mipmap.my_photo);
                }
            }

            @Override
            public void OnSetUserInfo(int error, String errorMsg) {

            }
        });

        ResultUserInfo resultUserInfo = LoginUtils.getUserInfo();
        if (resultUserInfo != null) {
            tvNickName.setText(resultUserInfo.data.nickname);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void enterEditUserInfo() {
        try {
            Intent intent = new Intent(getContext(), TCEditUseInfoActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.lcv_ui_logout: //注销APP
                doLogOut();
                break;
            case R.id.lcv_ui_version: //显示 APP SDK 的版本信息
                showSDKVersion();
                break;
            case R.id.ll_user_nick:
                startActivity(new Intent(getActivity(), ModifyNickNameActivity.class));
                break;
            case R.id.ll_password:
                startActivity(new Intent(getActivity(), ResettingPasswordActivity.class));
                break;
        }
    }

    private void doLogOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("提示");
        builder.setMessage("确定要退出登录吗？");

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
//                TCLoginMgr.getInstance().logout();
                SPUtils.getInstance().put(Constants.USER_INFO, "");
                Intent intent = new Intent(getContext(), TCLoginActivity.class);
                startActivity(intent);
                getActivity().finish();

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


    /**
     * 显示 APP SDK 的版本信息
     */
    private void showSDKVersion() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setMessage("APP : " + getAppVersion() + "\r\n"
                + "RTMP SDK: " + TXLiveBase.getSDKVersionStr() + "\r\n"
                + "IM SDK: " + TIMManager.getInstance().getVersion()
        );
        builder.show();
    }

    /**
     * 获取APP版本
     *
     * @return APP版本
     */
    private String getAppVersion() {
        PackageManager packageManager = getActivity().getPackageManager();
        PackageInfo packInfo;
        String version = "";
        try {
            packInfo = packageManager.getPackageInfo(getActivity().getPackageName(), 0);
            version = packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
