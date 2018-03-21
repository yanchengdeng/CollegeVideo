package net.zgxyzx.video.push;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.google.gson.Gson;
import com.qiniu.android.common.AutoZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.umeng.socialize.bean.SHARE_MEDIA;

import net.zgxyzx.video.R;
import net.zgxyzx.video.common.activity.TCBaseActivity;
import net.zgxyzx.video.common.ddzx.Constants;
import net.zgxyzx.video.common.ddzx.LoginUtils;
import net.zgxyzx.video.common.ddzx.beans.CreateLiveInfo;
import net.zgxyzx.video.common.ddzx.beans.ImageToken;
import net.zgxyzx.video.common.ddzx.beans.VideoJsonRequset;
import net.zgxyzx.video.common.utils.TCConstants;
import net.zgxyzx.video.common.utils.TCUploadHelper;
import net.zgxyzx.video.common.utils.TCUtils;
import net.zgxyzx.video.common.widget.TCCustomSwitch;
import net.zgxyzx.video.linkmic.TCLinkMicLivePushActivity;
import net.zgxyzx.video.mainui.list.TCLiveInfo;
import net.zgxyzx.video.push.camera.TCLivePublisherActivity;
import net.zgxyzx.video.push.screen.TCScreenRecordActivity;
import net.zgxyzx.video.userinfo.ITCUserInfoMgrListener;
import net.zgxyzx.video.userinfo.TCUserInfoMgr;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class TCPublishSettingActivity extends TCBaseActivity implements View.OnClickListener, TCUploadHelper.OnUploadListener, TCLocationHelper.OnLocationListener, CompoundButton.OnCheckedChangeListener {
    private TextView BtnBack, BtnPublish;
    private Dialog mPicChsDialog;
    private ImageView cover;
    private Uri fileUri, cropUri;
    private TextView tvPicTip;
    private TextView tvLBS;
    private TCCustomSwitch btnLBS;
    private TextView tvTitle;
    private TCUploadHelper mUploadHelper;

    private static final int CAPTURE_IMAGE_CAMERA = 100;
    private static final int IMAGE_STORE = 200;
    private static final String TAG = TCPublishSettingActivity.class.getSimpleName();

    private static final int CROP_CHOOSE = 10;
    private boolean mUploading = false;
    private boolean mPermission = false;

    private RadioGroup mRGBitrate;
    private RadioGroup mRGRecordType;
//    private RadioGroup mRGOrientation;
    private RelativeLayout mRLBitrate;
    private int mRecordType = TCConstants.RECORD_TYPE_CAMERA;
    private int mBitrateType = TCConstants.BITRATE_NORMAL;
//    private int mOrientation = TCConstants.ORIENTATION_LANDSCAPE;

    //分享相关
    private SHARE_MEDIA mShare_meidia = SHARE_MEDIA.MORE;
    private CheckBox mCbShareWX;
    private CheckBox mCbShareCircle;
    private CheckBox mCbShareQQ;
    private CheckBox mCbShareQzone;
    private CheckBox mCbShareWb;
    private CompoundButton mCbLastChecked = null;
    private ProgressBar progressBar;
    private String imageTokenContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_setting);
        mUploadHelper = new TCUploadHelper(this, this);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        tvTitle = (TextView) findViewById(R.id.live_title);
        BtnBack = (TextView) findViewById(R.id.btn_cancel);
        tvPicTip = (TextView) findViewById(R.id.tv_pic_tip);
        BtnPublish = (TextView) findViewById(R.id.btn_publish);
        cover = (ImageView) findViewById(R.id.cover);
        tvLBS = (TextView) findViewById(R.id.address);
        btnLBS = (TCCustomSwitch) findViewById(R.id.btn_lbs);


        mRGRecordType = (RadioGroup) findViewById(R.id.rg_record_type);
        mRGBitrate = (RadioGroup) findViewById(R.id.rg_bitrate);
        mRLBitrate= (RelativeLayout) findViewById(R.id.rl_bitrate);

        mRGRecordType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_record_camera:
                        mRecordType = TCConstants.RECORD_TYPE_CAMERA;
                        mRLBitrate.setVisibility(View.GONE);
                        break;
                    case R.id.rb_record_screen:
                        if (!checkScrRecordPermission()) {
                            Toast.makeText(getApplicationContext(), "当前安卓系统版本过低，仅支持5.0及以上系统", Toast.LENGTH_SHORT).show();
                            mRGRecordType.check(R.id.rb_record_camera);
                            return;
                        }
                        try {
                            TCUtils.checkFloatWindowPermission(TCPublishSettingActivity.this);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mRLBitrate.setVisibility(View.VISIBLE);
                        mRecordType = TCConstants.RECORD_TYPE_SCREEN;
                        break;
                    default:
                        break;
                }
            }
        });

        mRGBitrate.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rb_bitrate_slow:
                        mBitrateType = TCConstants.BITRATE_SLOW;
                        break;
                    case R.id.rb_bitrate_normal:
                        mBitrateType = TCConstants.BITRATE_NORMAL;
                        break;
                    case R.id.rb_bitrate_fast:
                        mBitrateType = TCConstants.BITRATE_FAST;
                        break;
                    default:
                        break;
                }
            }
        });

        mCbShareWX = (CheckBox) findViewById(R.id.cb_share_wx);
        mCbShareCircle = (CheckBox) findViewById(R.id.cb_share_circle);
        mCbShareQQ = (CheckBox) findViewById(R.id.cb_share_qq);
        mCbShareQzone = (CheckBox) findViewById(R.id.cb_share_qzone);
        mCbShareWb = (CheckBox) findViewById(R.id.cb_share_wb);

        mCbShareWX.setOnCheckedChangeListener(this);
        mCbShareCircle.setOnCheckedChangeListener(this);
        mCbShareQQ.setOnCheckedChangeListener(this);
        mCbShareQzone.setOnCheckedChangeListener(this);
        mCbShareWb.setOnCheckedChangeListener(this);

        cover.setOnClickListener(this);
        BtnBack.setOnClickListener(this);
        BtnPublish.setOnClickListener(this);
        btnLBS.setOnClickListener(this);

        initPhotoDialog();

        mPermission = checkPublishPermission();

        String strCover = TCUserInfoMgr.getInstance().getCoverPic();
        if(!TextUtils.isEmpty(strCover)) {
            RequestManager req = Glide.with(this);
            req.load(strCover).into(cover);
            tvPicTip.setVisibility(View.GONE);
        } else {
            cover.setImageResource(R.drawable.publish_background);
        }



    }

    private void getToken(final File fileFile) {
        progressBar.setVisibility(View.VISIBLE);
        HashMap<String, String> maps = new HashMap<>();
        maps.put("type", "image");
        StringRequest stringRequest = new StringRequest( LoginUtils.getTypeParams(Constants.GET_QINIU_IMAGE_TOKEN,maps), new Response.Listener<String>() {
            @Override
            public void onResponse(String jsonObject) {
                ImageToken imageToken = new Gson().fromJson(jsonObject.toString(), ImageToken.class);
                if (!TextUtils.isEmpty(imageToken.getUptoken())) {
                    imageTokenContent = imageToken.getUptoken();
                    getDownloadImages(fileFile);
                }else{
                    progressBar.setVisibility(View.GONE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                LogUtils.w("dyc",volleyError);
                progressBar.setVisibility(View.GONE);
            }
        });

        Volley.newRequestQueue(this).add(stringRequest);
    }

    String imageurl;
    private void getDownloadImages(File file) {
        //自动识别上传区域
        Configuration config = new Configuration.Builder()
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

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                if (info.isOK()) {
                                    Log.i("qiniu", "Upload Success");
                                    if (res != null) {
                                        imageurl = Constants.SERVER_IMAGE_URL + res.optString("hash");
                                        Glide.with(TCPublishSettingActivity.this).load(imageurl).into(cover);
                                        LogUtils.w("dyc",imageurl);
                                    }
                                } else {
                                    Log.i("qiniu", "Upload Fail");
                                    ToastUtils.showShort("稍后再试");
                                }
                                //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                            }
                        });

                        Log.w("dyc", key + ",\r\n " + info + ",\r\n " + res);
                    }
                }, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_cancel:
                finish();
                break;
            case R.id.btn_publish:
                //trim避免空格字符串
                if (TextUtils.isEmpty(tvTitle.getText().toString().trim())) {
                    Toast.makeText(getApplicationContext(), "请输入非空直播标题", Toast.LENGTH_SHORT).show();
                } else if(TCUtils.getCharacterNum(tvTitle.getText().toString()) > TCConstants.TV_TITLE_MAX_LEN){
                    Toast.makeText(getApplicationContext(), "直播标题过长 ,最大长度为"+TCConstants.TV_TITLE_MAX_LEN/2, Toast.LENGTH_SHORT).show();
                } else if (mUploading) {
                    Toast.makeText(getApplicationContext(), getString(R.string.publish_wait_uploading), Toast.LENGTH_SHORT).show();
                } else if(!TCUtils.isNetworkAvailable(this)) {
                    Toast.makeText(getApplicationContext(), "当前网络环境不能发布直播", Toast.LENGTH_SHORT).show();
                } else {
                    if (mRecordType == TCConstants.RECORD_TYPE_SCREEN) {
                        //录屏
                        Intent intent = new Intent(this, TCScreenRecordActivity.class);
                        intent.putExtra(TCConstants.ROOM_TITLE,
                                TextUtils.isEmpty(tvTitle.getText().toString()) ? TCUserInfoMgr.getInstance().getNickname() : tvTitle.getText().toString());
                        intent.putExtra(TCConstants.USER_ID, TCUserInfoMgr.getInstance().getUserId());
                        intent.putExtra(TCConstants.USER_NICK, TCUserInfoMgr.getInstance().getNickname());
                        intent.putExtra(TCConstants.USER_HEADPIC, TCUserInfoMgr.getInstance().getHeadPic());
                        intent.putExtra(TCConstants.COVER_PIC, TCUserInfoMgr.getInstance().getCoverPic());
//                        intent.putExtra(TCConstants.SCR_ORIENTATION, mOrientation);
                        intent.putExtra(TCConstants.BITRATE, mBitrateType);
                        intent.putExtra(TCConstants.USER_LOC,
                                tvLBS.getText().toString().equals(getString(R.string.text_live_lbs_fail)) ||
                                        tvLBS.getText().toString().equals(getString(R.string.text_live_location)) ?
                                        getString(R.string.text_live_close_lbs) : tvLBS.getText().toString());
                        intent.putExtra(TCConstants.SHARE_PLATFORM, mShare_meidia);
                        startActivity(intent);
                        finish();
                    } else {
                        createVideoUrl();
                    }
                }
                break;
            case R.id.cover:
                mPicChsDialog.show();
                break;
            case R.id.btn_lbs:
                if (btnLBS.getChecked()) {
                    btnLBS.setChecked(false, true);
                    tvLBS.setText(R.string.text_live_close_lbs);
                } else {
                    btnLBS.setChecked(true, true);
                    tvLBS.setText(R.string.text_live_location);
                    if (TCLocationHelper.checkLocationPermission(this)) {
                        if (!TCLocationHelper.getMyLocation(this, this)) {
                            tvLBS.setText(getString(R.string.text_live_lbs_fail));
                            //Toast.makeText(getApplicationContext(), "定位失败，请查看是否打开GPS", Toast.LENGTH_SHORT).show();
                            btnLBS.setChecked(false, false);
                        }
                    }
                }
                break;
        }
    }

    //创建直播
    private void createVideoUrl() {

            JSONObject params = new JSONObject();
            try {
                params.put("name", tvTitle.getEditableText().toString());
                params.put("uid", LoginUtils.getUserInfo().data.id);
                if(TextUtils.isEmpty(imageurl) ){
                    params.put("cover", "");
                }else{
                    params.put("cover",imageurl);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.CREATE_VIDEO, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    LogUtils.w(jsonObject);

                    CreateLiveInfo resultInfo = new Gson().fromJson(jsonObject.toString(), CreateLiveInfo.class);
                    if (resultInfo.code == 1) {
                        //摄像头
                        Intent intent;
                        if (TCConstants.TX_ENABLE_LINK_MIC) {
                            intent = new Intent(TCPublishSettingActivity.this, TCLinkMicLivePushActivity.class);
                        }
                        else {
                            intent = new Intent(TCPublishSettingActivity.this, TCLivePublisherActivity.class);
                        }
                        intent.putExtra(TCConstants.ROOM_TITLE,
                                TextUtils.isEmpty(tvTitle.getText().toString()) ? TCUserInfoMgr.getInstance().getNickname() : tvTitle.getText().toString());
                        intent.putExtra(TCConstants.USER_ID, resultInfo.data.uid);
                        intent.putExtra(TCConstants.USER_NICK, resultInfo.data.name);
                        intent.putExtra(TCConstants.USER_HEADPIC, TCUserInfoMgr.getInstance().getHeadPic());
                        intent.putExtra(TCConstants.COVER_PIC, TCUserInfoMgr.getInstance().getCoverPic());
                        intent.putExtra(TCConstants.PUBLISH_URL,resultInfo.data.push_url);
                        intent.putExtra(TCConstants.USER_LOC,
                                tvLBS.getText().toString().equals(getString(R.string.text_live_lbs_fail)) ||
                                        tvLBS.getText().toString().equals(getString(R.string.text_live_location)) ?
                                        getString(R.string.text_live_close_lbs) : tvLBS.getText().toString());
                        intent.putExtra(TCConstants.SHARE_PLATFORM, mShare_meidia);
                        TCLiveInfo item = new TCLiveInfo();
                        item.playurl = resultInfo.data.push_url;
                        item.type = 0;
                        TCLiveInfo.TCLiveUserInfo userInfo = new TCLiveInfo.TCLiveUserInfo();
                        userInfo.frontcover = "";
                        item.userinfo = userInfo;

                        intent.putExtra(TCConstants.PUSHER_ID, item.userid);
                        intent.putExtra(TCConstants.PLAY_URL,  item.playurl);
                        intent.putExtra(TCConstants.PUSHER_NAME, item.userinfo.nickname == null ? item.userid : item.userinfo.nickname);
                        intent.putExtra(TCConstants.PUSHER_AVATAR, item.userinfo.headpic);
                        intent.putExtra(TCConstants.HEART_COUNT, "" + item.likecount);
                        intent.putExtra(TCConstants.MEMBER_COUNT, "" + item.viewercount);
                        intent.putExtra(TCConstants.GROUP_ID, item.groupid);
                        intent.putExtra(TCConstants.PLAY_TYPE, item.type);
                        intent.putExtra(TCConstants.FILE_ID, item.fileid);
                        intent.putExtra(TCConstants.COVER_PIC, item.userinfo.frontcover);
                        intent.putExtra(TCConstants.TIMESTAMP, item.timestamp);
                        intent.putExtra(TCConstants.ROOM_TITLE, item.title);
//                        startActivityForResult(intent, START_LIVE_PLAY);
                        startActivity(intent);
//                        finish();
                    }else{
                        ToastUtils.showShort(resultInfo.msg);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    LogUtils.w(volleyError);
                    ToastUtils.showShort("稍后再试");
                }
            });
            Volley.newRequestQueue(TCPublishSettingActivity.this).add(request);

    }

    /**
     * 图片选择对话框
     */
    private void initPhotoDialog() {
        mPicChsDialog = new Dialog(this, R.style.floag_dialog);
        mPicChsDialog.setContentView(R.layout.dialog_pic_choose);

        WindowManager windowManager = getWindowManager();
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
    }


    /**
     * 获取图片资源
     *
     * @param type 类型（本地IMAGE_STORE/拍照CAPTURE_IMAGE_CAMERA）
     */
    private void getPicFrom(int type) {
        if (!mPermission) {
            Toast.makeText(this, getString(R.string.tip_no_permission), Toast.LENGTH_SHORT).show();
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
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(TCPublishSettingActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
//            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(TCPublishSettingActivity.this, Manifest.permission.CAMERA)) {
//                permissions.add(Manifest.permission.CAMERA);
//            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(TCPublishSettingActivity.this, Manifest.permission.READ_PHONE_STATE)) {
                permissions.add(Manifest.permission.READ_PHONE_STATE);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(TCPublishSettingActivity.this,
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
        String path = Environment.getExternalStorageDirectory()+ "/yunzhibo";

        File outputImage = new File(path, filename);
        if (ContextCompat.checkSelfPermission(TCPublishSettingActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(TCPublishSettingActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, TCConstants.WRITE_PERMISSION_REQ_CODE);
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
//            outputImage.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "生成封面失败", Toast.LENGTH_SHORT).show();
        }

        return Uri.fromFile(outputImage);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAPTURE_IMAGE_CAMERA:
                    startPhotoZoom(fileUri);
                    break;
                case IMAGE_STORE:
                    String path = TCUtils.getPath(this, data.getData());
                    if (null != path) {
                        File file = new File(path);
                        getToken(file);

//                        startPhotoZoom(Uri.fromFile(file));
                    }
                    break;
                case CROP_CHOOSE:
//                    mUploading = true;T
//                    tvPicTip.setVisibility(View.GONE);
//                    cover.setImageBitmap(null);
//                    cover.setImageURI(cropUri);
//                    mUploadHelper.uploadCover(cropUri.getPath());
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
            uri = FileProvider.getUriForFile(this, "net.zgxyzx.video.fileprovider", file);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case TCConstants.LOCATION_PERMISSION_REQ_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!TCLocationHelper.getMyLocation(this, this)) {
                        tvLBS.setText(getString(R.string.text_live_lbs_fail));
                        btnLBS.setChecked(false, false);
                    }
                }
                break;
            case TCConstants.WRITE_PERMISSION_REQ_CODE:
                for (int ret : grantResults) {
                    if (ret != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                mPermission = true;
                break;
            default:
                break;
        }
    }

    @Override
    public void onLocationChanged(int code, double lat1, double long1, String location) {
        if (btnLBS.getChecked()) {
            if (0 == code) {
                tvLBS.setText(location);
                TCUserInfoMgr.getInstance().setLocation(location, lat1, long1, new ITCUserInfoMgrListener() {
                    @Override
                    public void OnQueryUserInfo(int error, String errorMsg) {
                        // TODO: 16/8/10
                    }

                    @Override
                    public void OnSetUserInfo(int error, String errorMsg) {
                        if (0 != error) Toast.makeText(getApplicationContext(),"设置位置失败" + errorMsg,Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                tvLBS.setText(getString(R.string.text_live_lbs_fail));
            }
        } else {
            TCUserInfoMgr.getInstance().setLocation("", 0, 0, new ITCUserInfoMgrListener() {
                @Override
                public void OnQueryUserInfo(int error, String errorMsg) {
                    // TODO: 16/8/10
                }

                @Override
                public void OnSetUserInfo(int error, String errorMsg) {
                    if (0 != error) Toast.makeText(getApplicationContext(),"设置位置失败" + errorMsg,Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onUploadResult(int code, String url) {

        if (0 == code) {
            TCUserInfoMgr.getInstance().setUserCoverPic(url, new ITCUserInfoMgrListener() {
                @Override
                public void OnQueryUserInfo(int error, String errorMsg) {
                    // TODO: 16/8/10
                }

                @Override
                public void OnSetUserInfo(int error, String errorMsg) {

                }
            });
            RequestManager req = Glide.with(this);
            req.load(url).into(cover);
            Toast.makeText(this, "上传封面成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "上传封面失败，错误码 "+code, Toast.LENGTH_SHORT).show();
        }
        mUploading = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked == false) {
            mShare_meidia = SHARE_MEDIA.MORE;
            mCbLastChecked = null;
            return;
        }
        if (mCbLastChecked != null) {
            mCbLastChecked.setChecked(false);
        }
        mCbLastChecked = buttonView;
        switch (buttonView.getId()) {
            case R.id.cb_share_wx:
                mShare_meidia = SHARE_MEDIA.WEIXIN;
                break;
            case R.id.cb_share_circle:
                mShare_meidia = SHARE_MEDIA.WEIXIN_CIRCLE;
                break;
            case R.id.cb_share_qq:
                mShare_meidia = SHARE_MEDIA.QQ;
                break;
            case R.id.cb_share_qzone:
                mShare_meidia = SHARE_MEDIA.QZONE;
                break;
            case R.id.cb_share_wb:
                mShare_meidia = SHARE_MEDIA.SINA;
                break;
            default:
                break;
        }
    }
}
