package net.zgxyzx.video.common.ddzx;


public class Constants {

    public static final String BASE_NAME =SysDebug.IS_DEBUG? ".dadaodata.com/":".zgxyzx.net/";
//    public static final String BASE_URL = "http://192.168.0.132/zhibo/trunk/public/api/";
    public static final String BASE_URL = "http://live.api"+BASE_NAME+"api/";


    public static final String SERVER_KEY = "JF0XMw6XhwU8jXHH";

    public static final String DO_LOGIN = BASE_URL+"user/login";
    public static final String LIVE_LIST = BASE_URL+"Video/liveListV2";///直播列表
    public static final String VIDEO_LIST = BASE_URL+"Video/videoListV2";///视频列表
    public static final String CREATE_VIDEO = BASE_URL+"Video/insertLive";//创建直播

    public static final String DO_REGISTER = BASE_URL+"user/register";//创建直播
    public static final String USER_RESETPASSWORD = BASE_URL+"user/resetPassword";//忘记密码

    public static final String USER_RESET_PASSWORD = BASE_URL+"user/updatePassword";//忘记密码

    public static final String INDEX_CHECKNICK =BASE_URL+ "index/checkNick";//验证昵称

    public static final String INDEX_UPDATENICK = BASE_URL+"index/updateNick";//修改昵称

    public static final String  VIDEO_CHECKPASS = BASE_URL+"Video/checkPass";//验证yaoqigma


    public static final String  INDEX_CHECKROLE = BASE_URL+"index/checkRole";//验证主播

    public static final String VIDEO_CHECKHADPASS = BASE_URL+"Video/checkHadPass";//验证用户是否观看过

    public static final String GET_QINIU_IMAGE_TOKEN = "http://base.api"+BASE_NAME+"api/Qiniu/getToken";

    public static final String INDEX_HEADPIC = BASE_URL+"index/headPic";//上传头像

    public static final String USER_INFO = "user_info";

    public static final String SERVER_IMAGE_URL = "http://image.zgxyzx.net/";

    public static final String  GET_CODE = "http://base.api"+BASE_NAME+"api/sms/sendCode";//
}
