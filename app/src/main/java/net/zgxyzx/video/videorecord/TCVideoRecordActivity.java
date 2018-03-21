package net.zgxyzx.video.videorecord;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.zgxyzx.video.R;
import net.zgxyzx.video.common.activity.TCBaseActivity;
import net.zgxyzx.video.common.activity.TCVideoPreviewActivity;
import net.zgxyzx.video.common.utils.TCConstants;
import net.zgxyzx.video.common.utils.TCUtils;
import net.zgxyzx.video.common.widget.beautysetting.BeautyDialogFragment;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.ugc.TXRecordCommon;
import com.tencent.ugc.TXUGCRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * UGC主播端录制界面
 */
public class TCVideoRecordActivity extends TCBaseActivity implements View.OnClickListener, BeautyDialogFragment.OnBeautyParamsChangeListener
                                                            ,TXRecordCommon.ITXVideoRecordListener
{

    private BeautyDialogFragment mBeautyDialogFragment;

    //录制相关
    private boolean mRecording = false;
    private TXUGCRecord mTXCameraRecord = null;
    private ProgressBar mRecordProgress = null;
    private long  mStartRecordTimeStamp = 0;
    private BeautyDialogFragment.BeautyParams mBeautyParams = new BeautyDialogFragment.BeautyParams();

    private boolean mFlashOn = false;
    private boolean mFront = true;
    TXRecordCommon.TXRecordResult mTXRecordResult = null;
    TXCloudVideoView mVideoView;
    TextView mProgressTime;

    AudioManager mAudioManager;
    AudioManager.OnAudioFocusChangeListener mOnAudioFocusListener;
    private boolean mStartPreview = false;
    private boolean mPause = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_video_record);

        mBeautyDialogFragment = new BeautyDialogFragment();
        mBeautyDialogFragment.setBeautyParamsListner(mBeautyParams, this);

        mTXCameraRecord = TXUGCRecord.getInstance(this.getApplicationContext());

        // 预览
        if (mTXCameraRecord == null) {
            mTXCameraRecord = TXUGCRecord.getInstance(TCVideoRecordActivity.this.getApplicationContext());
        }
        mVideoView = (TXCloudVideoView) findViewById(R.id.video_view);
        mVideoView.enableHardwareDecode(true);

        mProgressTime = (TextView) findViewById(R.id.progress_time);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (checkPermission()) {
            startCameraPreview();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mTXCameraRecord != null) {
            mTXCameraRecord.stopCameraPreview();
            mStartPreview = false;
        }
        stopRecord(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mTXCameraRecord != null) {
            mTXCameraRecord.stopCameraPreview();
            mTXCameraRecord.setVideoRecordListener(null);
            mTXCameraRecord = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_beauty:
                Bundle args = new Bundle();
                args.putBoolean("hideMotionTable", true);
                try {
                    mBeautyDialogFragment.setArguments(args);
                    if (mBeautyDialogFragment.isAdded())
                        mBeautyDialogFragment.dismiss();
                    else
                        mBeautyDialogFragment.show(getFragmentManager(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case R.id.btn_flash:
                mFlashOn = !mFlashOn;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.toggleTorch(mFlashOn);
                }
                break;
            case R.id.btn_switch_camera:
                mFront = !mFront;
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.switchCamera(mFront);
                }
                break;
            case R.id.btn_close:
                if (mRecording && mTXCameraRecord != null) {
                    mTXCameraRecord.stopRecord();
                    mTXCameraRecord.setVideoRecordListener(null);
                }
                finish();
                break;
            case R.id.record:
                switchRecord();
                break;
            case R.id.btn_confirm:
                stopRecord(true);
                break;
            default:
                break;
        }
    }

    private void switchRecord() {
        if (mRecording) {
            if (mPause){
                resumeRecord();
            } else {
                pauseRecord();
            }
        } else {
            startRecord();
        }
    }

    private void resumeRecord() {
        ImageView liveRecord = (ImageView) findViewById(R.id.record);
        if (liveRecord != null) liveRecord.setBackgroundResource(R.drawable.video_stop);
        mPause = false;
        if (mTXCameraRecord != null) {
            mTXCameraRecord.resumeRecord();
        }
        requestAudioFocus();
    }

    private void pauseRecord() {
        ImageView liveRecord = (ImageView) findViewById(R.id.record);
        if (liveRecord != null) liveRecord.setBackgroundResource(R.drawable.start_record);
        mPause = true;
        if (mTXCameraRecord != null) {
            mTXCameraRecord.pauseRecord();
        }
        abandonAudioFocus();
    }

    private void startCameraPreview() {
        if (mStartPreview) return;
        mStartPreview = true;

        TXRecordCommon.TXUGCSimpleConfig param = new TXRecordCommon.TXUGCSimpleConfig();
        param.videoQuality = TXRecordCommon.VIDEO_QUALITY_MEDIUM;
        param.isFront = mFront;

        mTXCameraRecord = TXUGCRecord.getInstance(this.getApplicationContext());
        mTXCameraRecord.startCameraSimplePreview(param, mVideoView);
        mTXCameraRecord.setBeautyDepth(mBeautyParams.mBeautyStyle, mBeautyParams.mBeautyProgress, mBeautyParams.mWhiteProgress, mBeautyParams.mRuddyProgress);
        mTXCameraRecord.setFaceScaleLevel(mBeautyParams.mFaceLiftProgress);
        mTXCameraRecord.setEyeScaleLevel(mBeautyParams.mBigEyeProgress);
        mTXCameraRecord.setFilter(TCUtils.getFilterBitmap(getResources(), mBeautyParams.mFilterIdx));
        mTXCameraRecord.setGreenScreenFile(TCUtils.getGreenFileName(mBeautyParams.mGreenIdx), true);
        mTXCameraRecord.setMotionTmpl(mBeautyParams.mMotionTmplPath);
    }

    private void stopRecord(boolean showToast) {
        // 录制时间要大于5s
        if (System.currentTimeMillis() <= mStartRecordTimeStamp + 5*1000) {
            if (showToast) {
                showTooShortToast();
                return;
            } else {
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setVideoRecordListener(null);
                }
            }
        }
        if (mTXCameraRecord != null) {
            mTXCameraRecord.stopRecord();
        }
        ImageView liveRecord = (ImageView) findViewById(R.id.record);
        if(liveRecord != null) liveRecord.setBackgroundResource(R.drawable.start_record);
        mRecording = false;

        if (mRecordProgress != null) {
            mRecordProgress.setProgress(0);
        }
        if (mProgressTime != null) {
            mProgressTime.setText(String.format(Locale.CHINA, "%s","00:00"));
        }
        abandonAudioFocus();
    }

    private void showTooShortToast() {
        if (mRecordProgress != null) {
            int statusBarHeight = 0;
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = getResources().getDimensionPixelSize(resourceId);
            }

            int[] position = new int[2];
            mRecordProgress.getLocationOnScreen(position);
            Toast toast = Toast.makeText(this, "至少录到这里", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP|Gravity.LEFT, position[0], position[1] - statusBarHeight - 110);
            toast.show();
        }
    }

    private void startRecord() {
        if (mTXCameraRecord == null) {
            mTXCameraRecord = TXUGCRecord.getInstance(this.getApplicationContext());
        }
        mRecordProgress = (ProgressBar) findViewById(R.id.record_progress);
        mTXCameraRecord.setVideoRecordListener(this);
        int result = mTXCameraRecord.startRecord();
        if (result != 0) {
            Toast.makeText(TCVideoRecordActivity.this.getApplicationContext(),"录制失败，错误码：" + result, Toast.LENGTH_SHORT).show();
            mTXCameraRecord.setVideoRecordListener(null);
            mTXCameraRecord.stopRecord();
            return;
        }
        mRecording = true;
        ImageView liveRecord = (ImageView) findViewById(R.id.record);
        if(liveRecord != null) liveRecord.setBackgroundResource(R.drawable.video_stop);
        mStartRecordTimeStamp = System.currentTimeMillis();
        requestAudioFocus();
    }

    void startPreview() {
        if (mTXRecordResult != null && mTXRecordResult.retCode == TXRecordCommon.RECORD_RESULT_OK) {
            Intent intent = new Intent(getApplicationContext(), TCVideoPreviewActivity.class);
            intent.putExtra(TCConstants.VIDEO_RECORD_TYPE, TCConstants.VIDEO_RECORD_TYPE_PUBLISH);
            intent.putExtra(TCConstants.VIDEO_RECORD_RESULT, mTXRecordResult.retCode);
            intent.putExtra(TCConstants.VIDEO_RECORD_DESCMSG, mTXRecordResult.descMsg);
            intent.putExtra(TCConstants.VIDEO_RECORD_VIDEPATH, mTXRecordResult.videoPath);
            intent.putExtra(TCConstants.VIDEO_RECORD_COVERPATH, mTXRecordResult.coverPath);
            startActivity(intent);
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissions = new ArrayList<>();
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(this,
                        permissions.toArray(new String[0]),
                        100);
                return false;
            }
        }

        return true;
    }

//    private void retryRecord() {
//        if (mRecording ) {
//            stopRecord();
//        }
//        View recordLayout = TCVideoRecordActivity.this.findViewById(R.id.record_layout);
//        View publishLayout = TCVideoRecordActivity.this.findViewById(R.id.publishLayout);
//        View controlLayout = TCVideoRecordActivity.this.findViewById(R.id.record_control);
//        if (recordLayout != null) {
//            recordLayout.setVisibility(View.VISIBLE);
//        }
//        if (publishLayout != null) {
//            publishLayout.setVisibility(View.GONE);
//        }
//        if (controlLayout != null) {
//            controlLayout.setVisibility(View.VISIBLE);
//        }
//
//        if (mRecordProgress != null) {
//            mRecordProgress.setProgress(0);
//        }
//
//        mLayoutPitu.setVisibility(View.GONE);
//        mPitu.setVisibility(View.VISIBLE);
//        mClosePitu.setVisibility(View.GONE);
//    }

    @Override
    public void onBeautyParamsChange(BeautyDialogFragment.BeautyParams params, int key) {
        switch (key){
            case BeautyDialogFragment.BEAUTYPARAM_BEAUTY:
            case BeautyDialogFragment.BEAUTYPARAM_WHITE:
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setBeautyDepth(params.mBeautyStyle, params.mBeautyProgress, params.mWhiteProgress, params.mRuddyProgress);
                }
                break;
            case BeautyDialogFragment.BEAUTYPARAM_FACE_LIFT:
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setFaceScaleLevel(params.mFaceLiftProgress);
                }
                break;
            case BeautyDialogFragment.BEAUTYPARAM_BIG_EYE:
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setEyeScaleLevel(params.mBigEyeProgress);
                }
                break;
            case BeautyDialogFragment.BEAUTYPARAM_FILTER:
                if (mTXCameraRecord != null) {
                    mTXCameraRecord.setFilter(TCUtils.getFilterBitmap(getResources(), params.mFilterIdx));
                }
                break;
            case BeautyDialogFragment.BEAUTYPARAM_MOTION_TMPL:
                if (mTXCameraRecord != null){
                    mTXCameraRecord.setMotionTmpl(params.mMotionTmplPath);
                }
                break;
            case BeautyDialogFragment.BEAUTYPARAM_GREEN:
                if (mTXCameraRecord != null){
                    mTXCameraRecord.setGreenScreenFile(TCUtils.getGreenFileName(params.mGreenIdx), true);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRecordEvent(int event, Bundle param) {

    }

    @Override
    public void onRecordProgress(long milliSecond) {
        if (mRecordProgress != null) {
            float progress = milliSecond / 60000.0f;
            mRecordProgress.setProgress((int) (progress * 100));
            mProgressTime.setText(String.format(Locale.CHINA, "00:%02d", milliSecond/1000));
            if (milliSecond >= 60000.0f) {
                stopRecord(true);
            }
        }
    }

    @Override
    public void onRecordComplete(TXRecordCommon.TXRecordResult result) {
        mTXRecordResult = result;
        if (mTXRecordResult.retCode != TXRecordCommon.RECORD_RESULT_OK) {
            ImageView liveRecord = (ImageView) findViewById(R.id.record);
            if(liveRecord != null) liveRecord.setBackgroundResource(R.drawable.start_record);
            mRecording = false;

            if (mRecordProgress != null) {
                mRecordProgress.setProgress(0);
            }
            if (mProgressTime != null) {
                mProgressTime.setText(String.format(Locale.CHINA, "%s","00:00"));
            }
            Toast.makeText(TCVideoRecordActivity.this.getApplicationContext(),"录制失败，原因：" + mTXRecordResult.descMsg, Toast.LENGTH_SHORT).show();
        } else {
            View recordLayout = TCVideoRecordActivity.this.findViewById(R.id.record_layout);
            View publishLayout = TCVideoRecordActivity.this.findViewById(R.id.publishLayout);
            View controlLayout = TCVideoRecordActivity.this.findViewById(R.id.record_control);
            if (recordLayout != null) {
                recordLayout.setVisibility(View.VISIBLE);
            }
            if (publishLayout != null) {
                publishLayout.setVisibility(View.GONE);
            }
            if (controlLayout != null) {
                controlLayout.setVisibility(View.VISIBLE);
            }

            if (mRecordProgress != null) {
                mRecordProgress.setProgress(0);
            }
            mProgressTime.setText(String.format(Locale.CHINA, "%s","00:00"));
            startPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:
                for (int ret : grantResults) {
                    if (ret != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                TXRecordCommon.TXUGCSimpleConfig param = new TXRecordCommon.TXUGCSimpleConfig();
                param.videoQuality = TXRecordCommon.VIDEO_QUALITY_MEDIUM;
                param.isFront = mFront;
                mTXCameraRecord.startCameraSimplePreview(param,mVideoView);
                mTXCameraRecord.setBeautyDepth(mBeautyParams.mBeautyStyle, mBeautyParams.mBeautyProgress, mBeautyParams.mWhiteProgress, mBeautyParams.mRuddyProgress);
                mTXCameraRecord.setMotionTmpl(mBeautyParams.mMotionTmplPath);
                break;
            default:
                break;
        }
    }

    private void requestAudioFocus() {

        if (null == mAudioManager) {
            mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        }

        if (null == mOnAudioFocusListener) {
            mOnAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {

                @Override
                public void onAudioFocusChange(int focusChange) {
                    try {
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            mTXCameraRecord.setVideoRecordListener(null);
                            stopRecord(false);
                        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            mTXCameraRecord.setVideoRecordListener(null);
                            stopRecord(false);
                        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            };
        }
        try {
            mAudioManager.requestAudioFocus(mOnAudioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abandonAudioFocus() {
        try {
            if (null != mAudioManager && null != mOnAudioFocusListener) {
                mAudioManager.abandonAudioFocus(mOnAudioFocusListener);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
