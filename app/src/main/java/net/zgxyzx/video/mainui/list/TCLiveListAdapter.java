package net.zgxyzx.video.mainui.list;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ScreenUtils;
import com.bumptech.glide.Glide;

import net.zgxyzx.video.R;
import net.zgxyzx.video.common.utils.TCUtils;

import java.util.ArrayList;

/**
 * 直播列表的Adapter
 * 列表项布局格式: R.layout.listview_video_item
 * 列表项数据格式: TCLiveInfo
 */
public class TCLiveListAdapter extends ArrayAdapter<TCLiveInfo> {
    private int resourceId;
    private Activity mActivity;
    private ArrayList<TCLiveInfo> objects;
    RelativeLayout.LayoutParams coverParams ;

    public TCLiveListAdapter(FragmentActivity activity) {
        super(activity, R.layout.listview_video_item);
        resourceId = R.layout.listview_video_item;
    }

    public void setNewDate(ArrayList<TCLiveInfo> tcLiveInfos) {
        this.objects = tcLiveInfos;
        notifyDataSetChanged();
    }

    private class ViewHolder{
        TextView tvTitle;
        TextView tvHost;
        TextView tvMembers;
        TextView tvAdmires;
        TextView tvLbs;
        ImageView ivCover;
        ImageView ivAvatar;
        ImageView ivLogo;
    }

    public TCLiveListAdapter(Activity activity, ArrayList<TCLiveInfo> objects) {
        super(activity, R.layout.listview_video_item, objects);
        resourceId = R.layout.listview_video_item;
        mActivity = activity;
        coverParams = new RelativeLayout.LayoutParams(ScreenUtils.getScreenWidth(),(int)(ScreenUtils.getScreenWidth()*0.4));


    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView != null) {
            holder = (ViewHolder)convertView.getTag();
        } else {
            convertView = LayoutInflater.from(getContext()).inflate(resourceId, null);

            holder = new ViewHolder();
            holder.ivCover = (ImageView) convertView.findViewById(R.id.cover);
            holder.tvTitle = (TextView) convertView.findViewById(R.id.live_title);
            holder.tvHost = (TextView) convertView.findViewById(R.id.host_name);
            holder.tvMembers = (TextView) convertView.findViewById(R.id.live_members);
            holder.tvAdmires = (TextView) convertView.findViewById(R.id.praises);
            holder.tvLbs = (TextView) convertView.findViewById(R.id.live_lbs);
            holder.ivAvatar = (ImageView) convertView.findViewById(R.id.avatar);
            holder.ivLogo = (ImageView) convertView.findViewById(R.id.live_logo);

            convertView.setTag(holder);
        }

        if (position>getCount()-1){
            return convertView;
        }

        TCLiveInfo data = getItem(position);

        //直播封面
        String cover = data.userinfo.frontcover;
        if (TextUtils.isEmpty(cover)){
            holder.ivCover.setImageResource(R.mipmap.video_cover);
        }else{
             Glide.with(mActivity).load(cover).placeholder(R.mipmap.video_cover).into(holder.ivCover);
        }

        //主播头像
        TCUtils.showPicWithUrl(mActivity,holder.ivAvatar,data.userinfo.headpic,R.mipmap.my_photo);
        //主播昵称
        if (!TextUtils.isEmpty(data.userinfo.nickname)){
            holder.tvHost.setText("@" + data.userinfo.nickname);
        }

        holder.ivCover.setLayoutParams(coverParams);



        //直播标题
        holder.tvTitle.setText(data.name);
        //直播观看人数
        holder.tvMembers.setText(""+data.viewercount);
        //直播点赞数
        holder.tvAdmires.setText(""+data.likecount);
        //视频类型，直播或者回放
        if (data.type == 0) {
            holder.ivLogo.setVisibility(View.VISIBLE);
            holder.ivLogo.setImageResource(R.drawable.live);
        } else if (data.type == 1) {
            holder.ivLogo.setVisibility(View.VISIBLE);
            holder.ivLogo.setImageResource(R.drawable.playback);
        } else {
            holder.ivLogo.setVisibility(View.INVISIBLE);
        }
        return convertView;
    }

}
