package net.zgxyzx.video.common.ddzx.beans;


import java.io.Serializable;

public class CreateLiveInfo implements Serializable {

    public int code;//1,
    public String msg;//成功,
    public LiveInfo data;


    public class LiveInfo implements Serializable{
        public int id;//132,
        public String name;//hhhh,
        public String stream_id;//3897_132,
        public String start_time;//1505179798,
        public String end_time;//1505266198,
        public String length;//0,
        public String is_recorded;//1,
        public String uid;//1,
        public String cover;//,
        public String pass;//P5Y9NCRS,
        public String push_url;//rtmp;////3897.livepush.myqcloud.com/live/3897_132?bizid=3897&txSecret=db635b6f91b5091762ae3b262cb564ad&txTime=59B88A16,
        public String pull_url;//rtmp;////3897.liveplay.myqcloud.com/live/3897_132,
        public String status;//2,
        public String view_limit;//0
    }
}
