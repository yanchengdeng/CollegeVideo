package net.zgxyzx.video.mainui.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.Gson;

import net.zgxyzx.video.R;
import net.zgxyzx.video.common.ddzx.Constants;
import net.zgxyzx.video.common.ddzx.LoginUtils;
import net.zgxyzx.video.common.ddzx.beans.BaseInfo;
import net.zgxyzx.video.common.ddzx.beans.ResultVieoInfo;
import net.zgxyzx.video.common.ddzx.beans.VideoItemInfo;
import net.zgxyzx.video.common.ddzx.beans.VideoJsonRequset;
import net.zgxyzx.video.common.utils.TCConstants;
import net.zgxyzx.video.linkmic.TCLinkMicLivePlayActivity;
import net.zgxyzx.video.play.TCLivePlayerActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * 直播列表页面，展示当前直播、回放、UGC视频
 * 界面展示使用：GridView+SwipeRefreshLayout
 * 列表数据Adapter：TCLiveListAdapter, TCUGCVideoListAdapter
 * 数据获取接口： TCLiveListMgr
 */
public class TCLiveListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {
    public static final int START_LIVE_PLAY = 100;
    private static final String TAG = "TCLiveListFragment";
    private GridView mVideoListView;
    private TCLiveListAdapter mVideoListViewAdapter;
    private TCUGCVideoListAdapter mUGCListViewAdapter = null;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    //避免连击
    private long mLastClickTime = 0;

    private TextView mLiveText;
    private TextView mVodText;
    private TextView mUGCText;
    private ImageView mLiveImage;
    private ImageView mVodImage;
    private ImageView mUGCImage;
    private int mDataType = TCLiveListMgr.LIST_TYPE_LIVE;
    private boolean mLiveListFetched = false;
    private boolean mUGCListFetched = false;
    View mEmptyView;

//    private ProgressBar progressBar;


    private ArrayList<TCLiveInfo> tcLiveInfosList = new ArrayList<>();//直播视频
    private ArrayList<TCLiveInfo> tcVideoInfoList = new ArrayList<>();//视频回、放

    public TCLiveListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videolist, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout_list);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light, android.R.color.holo_orange_light, android.R.color.holo_red_light);
        mSwipeRefreshLayout.setOnRefreshListener(this);

//        progressBar = (ProgressBar) view.findViewById(R.id.progressbar);
        mVideoListView = (GridView) view.findViewById(R.id.live_list);

        mVideoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    if (mDataType == TCLiveListMgr.LIST_TYPE_UGC) {
                        if (i >= mUGCListViewAdapter.getCount()) {
                            return;
                        }
                    } else if (i >= mVideoListViewAdapter.getCount()) {
                        return;
                    }
                    if (0 == mLastClickTime || System.currentTimeMillis() - mLastClickTime > 1000) {
                        TCLiveInfo item;
                        if (mDataType == TCLiveListMgr.LIST_TYPE_UGC) {
                            item = mUGCListViewAdapter.getItem(i);
                        } else {
                            item = mVideoListViewAdapter.getItem(i);

                        }
                        if (item == null) {
                            Log.e(TAG, "live list item is null at position:" + i);
                            return;
                        }
                        if (item.type == 0) {
                            checkHasVieo(item);
                        } else {

                            startLivePlay(item);
                        }
                    }
                    mLastClickTime = System.currentTimeMillis();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mEmptyView = view.findViewById(R.id.tv_listview_empty);

        mLiveText = (TextView) view.findViewById(R.id.text_live);
        mVodText = (TextView) view.findViewById(R.id.text_vod);
        mUGCText = (TextView) view.findViewById(R.id.text_ugc);

        mLiveImage = (ImageView) view.findViewById(R.id.image_live);
        mVodImage = (ImageView) view.findViewById(R.id.image_vod);
        mUGCImage = (ImageView) view.findViewById(R.id.image_ugc);

        mLiveText.setOnClickListener(this);
        mVodText.setOnClickListener(this);
        mUGCText.setOnClickListener(this);

        refreshListView();

        return view;

    }

    private void checkHasVieo(final TCLiveInfo item) {
        JSONObject params = new JSONObject();
        try {
            params.put("uid", LoginUtils.getUserInfo().data.id);
            params.put("id", item.id);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.VIDEO_CHECKHADPASS, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                LogUtils.w(jsonObject);

                BaseInfo baseInfo = new Gson().fromJson(jsonObject.toString(), BaseInfo.class);
                if (baseInfo.code == 1) {
                    startLivePlay(item);
                } else {
                    shwoDialog(item);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                if (!NetworkUtils.isConnected()) {
                    ToastUtils.showShort("请打开网络");
                } else {
                    shwoDialog(item);
                }
            }
        });
        Volley.newRequestQueue(getActivity()).add(request);
    }

    private void shwoDialog(final TCLiveInfo item) {

        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.show();
        Window window = alertDialog.getWindow();
        window.setContentView(R.layout.dialog_code_layout);
        TextView tvcancel = (TextView) window.findViewById(R.id.tv_cancle);
        alertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        TextView tvSure = (TextView) window.findViewById(R.id.tv_sure);

        final EditText editText = (EditText) window.findViewById(R.id.et_code);

        tvcancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        tvSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(editText.getEditableText().toString())) {
                    ToastUtils.showShort("请输入邀请码");
                } else {
                    doValid(editText.getEditableText().toString().trim(), item);
                    alertDialog.dismiss();
                }
            }
        });
    }

    private void doValid(String trim, final TCLiveInfo item) {
        JSONObject params = new JSONObject();
        try {
            params.put("pass", trim);
            params.put("id", item.id);
            params.put("uid", LoginUtils.getUserInfo().data.id);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.VIDEO_CHECKPASS, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                LogUtils.w(jsonObject);

                BaseInfo baseInfo = new Gson().fromJson(jsonObject.toString(), BaseInfo.class);
                if (baseInfo.code == 1) {
                    startLivePlay(item);
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
        Volley.newRequestQueue(getActivity()).add(request);
    }

    private int pageNum = 1;

    //获取视频列表
    private void getViedeoList() {
//        progressBar.setVisibility(View.VISIBLE);
        if (pageNum == 1)
            mSwipeRefreshLayout.setRefreshing(true);
        final JSONObject params = new JSONObject();
        try {
            params.put("status", mDataType);
            params.put("page_id", pageNum);
            params.put("page_size", 200);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        tcVideoInfoList.clear();

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.VIDEO_LIST, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                LogUtils.w(jsonObject);
                setStatus(mDataType);
                mLiveText.setClickable(true);
                mSwipeRefreshLayout.setRefreshing(false);
//                progressBar.setVisibility(View.GONE);
                ResultVieoInfo resultInfo = new Gson().fromJson(jsonObject.toString(), ResultVieoInfo.class);
                if (resultInfo.code == 1) {
                    if (resultInfo.data != null && resultInfo.data.size() > 0) {
                        ArrayList<TCLiveInfo> tcLiveInfos = new ArrayList<>();
                        for (VideoItemInfo item : resultInfo.data) {
                            TCLiveInfo tcLiveInfo = new TCLiveInfo();
                            tcLiveInfo.playurl = item.pull_url;
                            tcLiveInfo.type = 1;
                            tcLiveInfo.id = item.id;
                            tcLiveInfo.name = item.name;
//                            tcLiveInfo.playurl = "http://3897.liveplay.myqcloud.com/live/3897_dcb0bf503d.flv";
//                            tcLiveInfo.hls_play_url = item.pull_url;
                            TCLiveInfo.TCLiveUserInfo userInfo = new TCLiveInfo.TCLiveUserInfo();
                            userInfo.nickname = item.nickname;
                            userInfo.headpic = item.headpic;
                            userInfo.frontcover = item.cover;
                            tcLiveInfo.userinfo = userInfo;
                            tcLiveInfos.add(tcLiveInfo);
                        }

                        tcVideoInfoList = tcLiveInfos;
                        mVideoListViewAdapter = new TCLiveListAdapter(getActivity(), tcLiveInfos);
                        mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                        mVideoListView.setAdapter(mVideoListViewAdapter);
                    } else {
                        mVideoListViewAdapter = new TCLiveListAdapter(getActivity(), tcVideoInfoList);
                        mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                        mVideoListView.setAdapter(mVideoListViewAdapter);
                    }

                } else {
                    mVideoListViewAdapter = new TCLiveListAdapter(getActivity(), tcVideoInfoList);
                    mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                    mVideoListView.setAdapter(mVideoListViewAdapter);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                LogUtils.w(volleyError);
                mSwipeRefreshLayout.setRefreshing(false);
                mLiveText.setClickable(true);
                setStatus(mDataType);
//                progressBar.setVisibility(View.GONE);
                mVideoListViewAdapter = new TCLiveListAdapter(getActivity(), tcVideoInfoList);
                mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                mVideoListView.setAdapter(mVideoListViewAdapter);
            }
        });
        Volley.newRequestQueue(getActivity()).add(request);
    }


    //获取直播列表
    private void getLiveList() {
//        progressBar.setVisibility(View.VISIBLE);
        mSwipeRefreshLayout.setRefreshing(true);
        JSONObject params = new JSONObject();
        try {
            params.put("status", mDataType);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        tcLiveInfosList.clear();

        VideoJsonRequset request = new VideoJsonRequset(Request.Method.POST, Constants.LIVE_LIST, LoginUtils.getParams(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                LogUtils.w(jsonObject);
                setStatus(mDataType);
                mSwipeRefreshLayout.setRefreshing(false);
                mVodText.setClickable(true);
//                progressBar.setVisibility(View.GONE);
                ResultVieoInfo resultInfo = new Gson().fromJson(jsonObject.toString(), ResultVieoInfo.class);
                if (resultInfo.code == 1) {
                    if (resultInfo.data != null && resultInfo.data.size() > 0) {
                        ArrayList<TCLiveInfo> tcLiveInfos = new ArrayList<>();
                        for (VideoItemInfo item : resultInfo.data) {
                            TCLiveInfo tcLiveInfo = new TCLiveInfo();
                            tcLiveInfo.playurl = item.pull_url;
                            tcLiveInfo.type = 0;
                            tcLiveInfo.id = item.id;
                            tcLiveInfo.name = item.name;
//                            tcLiveInfo.playurl = "http://3897.liveplay.myqcloud.com/live/3897_dcb0bf503d.flv";
//                            tcLiveInfo.hls_play_url = item.pull_url;
                            TCLiveInfo.TCLiveUserInfo userInfo = new TCLiveInfo.TCLiveUserInfo();
                            userInfo.nickname = item.nickname;
                            userInfo.headpic = item.headpic;
                            userInfo.frontcover = item.cover;
                            tcLiveInfo.userinfo = userInfo;
                            tcLiveInfos.add(tcLiveInfo);
                        }

                        tcLiveInfosList = tcLiveInfos;
                        mVideoListViewAdapter = new TCLiveListAdapter(getActivity(), tcLiveInfos);
                        mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                        mVideoListView.setAdapter(mVideoListViewAdapter);
                    } else {
                        mVideoListViewAdapter = new TCLiveListAdapter(getActivity(), tcLiveInfosList);
                        mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                        mVideoListView.setAdapter(mVideoListViewAdapter);
                    }


                } else {
                    mVideoListViewAdapter = new TCLiveListAdapter(getActivity(), tcLiveInfosList);
                    mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                    mVideoListView.setAdapter(mVideoListViewAdapter);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                LogUtils.w(volleyError);
//                progressBar.setVisibility(View.GONE);
                setStatus(mDataType);
                mSwipeRefreshLayout.setRefreshing(false);
                mVodText.setClickable(true);
                mVideoListViewAdapter = new TCLiveListAdapter(getActivity(), tcLiveInfosList);
                mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                mVideoListView.setAdapter(mVideoListViewAdapter);
            }
        });
        Volley.newRequestQueue(getActivity()).add(request);
    }


    @Override
    public void onRefresh() {
        refreshListView();
    }

    /**
     * 刷新直播列表
     */
    private void refreshListView() {
//        if (reloadLiveList()) {
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if (mDataType == TCLiveListMgr.LIST_TYPE_LIVE) {
                    getLiveList();
                } else {
                    getViedeoList();
                }
            }
        });
//        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (START_LIVE_PLAY == requestCode) {
                if (0 != resultCode) {
                    //观看直播返回错误信息后，刷新列表，但是不显示动画
//                    reloadLiveList();
                } else {
                    if (data == null) {
                        return;
                    }
                    //更新列表项的观看人数和点赞数
                    String userId = data.getStringExtra(TCConstants.PUSHER_ID);
                    for (int i = 0; i < mVideoListViewAdapter.getCount(); i++) {
                        TCLiveInfo info = mVideoListViewAdapter.getItem(i);
                        if (info != null && info.userid.equalsIgnoreCase(userId)) {
                            info.viewercount = (int) data.getLongExtra(TCConstants.MEMBER_COUNT, info.viewercount);
                            info.likecount = (int) data.getLongExtra(TCConstants.HEART_COUNT, info.likecount);
                            mVideoListViewAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 重新加载直播列表
     */
    private boolean reloadLiveList() {
        switch (mDataType) {
            case TCLiveListMgr.LIST_TYPE_LIVE:
                return TCLiveListMgr.getInstance().reloadLiveList(mDataType, mLiveListener);
            case TCLiveListMgr.LIST_TYPE_VOD:
                return TCLiveListMgr.getInstance().reloadLiveList(mDataType, mVodListener);
            case TCLiveListMgr.LIST_TYPE_UGC:
                return TCLiveListMgr.getInstance().reloadLiveList(mDataType, mUGCListener);
        }
        return false;
    }

    void onGetListData(int retCode, ArrayList<TCLiveInfo> result, boolean refresh) {
        if (retCode == 0) {
            mVideoListViewAdapter.clear();
            if (result != null) {
                mVideoListViewAdapter.addAll((ArrayList<TCLiveInfo>) result.clone());
            }
            if (refresh) {
                mVideoListViewAdapter.notifyDataSetChanged();
            }
        } else {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "刷新列表失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    TCLiveListMgr.Listener mLiveListener = new TCLiveListMgr.Listener() {

        @Override
        public void onLiveList(int retCode, ArrayList<TCLiveInfo> result, boolean refresh) {
            if (mDataType == TCLiveListMgr.LIST_TYPE_LIVE) {
                onGetListData(retCode, result, refresh);
                mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
            }
            mSwipeRefreshLayout.setRefreshing(false);
        }
    };

    TCLiveListMgr.Listener mVodListener = new TCLiveListMgr.Listener() {

        @Override
        public void onLiveList(int retCode, ArrayList<TCLiveInfo> result, boolean refresh) {
            if (mDataType == TCLiveListMgr.LIST_TYPE_VOD) {
                onGetListData(retCode, result, refresh);
                mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
            }
            mSwipeRefreshLayout.setRefreshing(false);
        }
    };

    TCLiveListMgr.Listener mUGCListener = new TCLiveListMgr.Listener() {

        @Override
        public void onLiveList(int retCode, ArrayList<TCLiveInfo> result, boolean refresh) {
            if (mDataType == TCLiveListMgr.LIST_TYPE_UGC) {
                if (retCode == 0) {

                    mUGCListViewAdapter.clear();
                    if (result != null) {
                        mUGCListViewAdapter.addAll((ArrayList<TCLiveInfo>) result.clone());
                    }
                    if (refresh) {
                        mUGCListViewAdapter.notifyDataSetChanged();
                    }
                } else {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "刷新列表失败", Toast.LENGTH_LONG).show();
                    }
                }
                mEmptyView.setVisibility(mUGCListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
            }
            mSwipeRefreshLayout.setRefreshing(false);
        }
    };

    /**
     * 开始播放视频
     *
     * @param item 视频数据
     */
    private void startLivePlay(final TCLiveInfo item) {
        Intent intent;
        if (TCConstants.TX_ENABLE_LINK_MIC) {
            intent = new Intent(getActivity(), TCLinkMicLivePlayActivity.class);
        } else {
            intent = new Intent(getActivity(), TCLivePlayerActivity.class);
        }
        intent.putExtra(TCConstants.PUSHER_ID, item.userid);
        intent.putExtra(TCConstants.PLAY_URL, item.playurl);
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
        startActivityForResult(intent, START_LIVE_PLAY);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.text_live:
                mDataType = TCLiveListMgr.LIST_TYPE_LIVE;
                mLiveText.setClickable(false);
                getLiveList();
                break;
            case R.id.text_vod:
                mDataType = TCLiveListMgr.LIST_TYPE_VOD;
                mVodText.setClickable(false);
                getViedeoList();
                break;
            case R.id.text_ugc:
                mDataType = TCLiveListMgr.LIST_TYPE_UGC;
                break;
        }
    }

    private void setStatus(int dataType) {
        if (dataType == TCLiveListMgr.LIST_TYPE_LIVE) {

            mLiveText.setBackground(getResources().getDrawable(R.drawable.btn_tab_pressed));
            mVodText.setBackground(null);
//            mVideoListViewAdapter.addAll((ArrayList<TCLiveInfo>) TCLiveListMgr.getInstance().getDataList(mDataType).clone());
//            mVideoListViewAdapter.notifyDataSetChanged();
//            mVideoListView.setNumColumns(1);
//            mVideoListView.setHorizontalSpacing(0);
//            mVideoListView.setVerticalSpacing(0);
//            mVideoListView.setAdapter(mVideoListViewAdapter);
//            mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
        } else if (dataType == TCLiveListMgr.LIST_TYPE_VOD) {
//            mVideoListViewAdapter.clear();
            mVodText.setBackground(getResources().getDrawable(R.drawable.btn_tab_pressed));
            mLiveText.setBackground(null);
//            mVideoListViewAdapter.addAll((ArrayList<TCLiveInfo>) TCLiveListMgr.getInstance().getDataList(mDataType).clone());
//            mVideoListViewAdapter.notifyDataSetChanged();
//            mVideoListView.setNumColumns(1);
//            mVideoListView.setHorizontalSpacing(0);
//            mVideoListView.setVerticalSpacing(0);
//            mVideoListView.setAdapter(mVideoListViewAdapter);
//            mEmptyView.setVisibility(mVideoListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
        } else if (dataType == TCLiveListMgr.LIST_TYPE_UGC) {
            if (mUGCListViewAdapter == null) {
                mUGCListViewAdapter = new TCUGCVideoListAdapter(getActivity(), (ArrayList<TCLiveInfo>) TCLiveListMgr.getInstance().getDataList(mDataType).clone());
            }
            mUGCListViewAdapter.clear();
            mUGCListViewAdapter.addAll((ArrayList<TCLiveInfo>) TCLiveListMgr.getInstance().getDataList(mDataType).clone());
            mUGCListViewAdapter.notifyDataSetChanged();
            if (!mUGCListFetched) {
                refreshListView();
                mUGCListFetched = true;
            }
            mVideoListView.setNumColumns(2);
            mVideoListView.setHorizontalSpacing(6);
            mVideoListView.setVerticalSpacing(6);
            mVideoListView.setAdapter(mUGCListViewAdapter);
            mEmptyView.setVisibility(mUGCListViewAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }
}