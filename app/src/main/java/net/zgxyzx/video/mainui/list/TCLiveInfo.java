package net.zgxyzx.video.mainui.list;

public class TCLiveInfo {
    public String id;
    public String   userid;
    public String   groupid;
    public int      timestamp;
    public int      type;
    public int      viewercount;
    public int      likecount;
    public String   title;
    public String   playurl;
    public String   hls_play_url;
    public String   fileid;
    public String name;
    public String pic_cover;

    //TCLiveUserInfo
    public TCLiveUserInfo userinfo;


    public static class TCLiveUserInfo {
        public String nickname;
        public String headpic;
        public String frontcover;
        public String location;
    }
}
