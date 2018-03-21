package net.zgxyzx.video.common.ddzx;


import android.text.TextUtils;

import com.blankj.utilcode.util.SPUtils;
import com.google.gson.Gson;

import net.zgxyzx.video.common.ddzx.beans.ResultUserInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class LoginUtils {

    public static boolean isLogin() {
        ResultUserInfo userInfo = getUserInfo();
        if (userInfo == null) {
            return false;
        } else {
            return true;
        }
    }

    public static void saveUserInofo(ResultUserInfo resultInfo) {
        SPUtils.getInstance().put(Constants.USER_INFO, new Gson().toJson(resultInfo));

    }

    public static ResultUserInfo getUserInfo() {
        String ss = SPUtils.getInstance().getString(Constants.USER_INFO);
        if (TextUtils.isEmpty(ss)) {
            return null;
        } else {
            ResultUserInfo userInfo = new Gson().fromJson(ss, ResultUserInfo.class);
            return userInfo;
        }

    }

    public static JSONObject getParams(JSONObject params){

        try {
            params.put("time", System.currentTimeMillis());
            if (LoginUtils.getUserInfo()!=null && !TextUtils.isEmpty(LoginUtils.getUserInfo().data.token)){
                params.put("token",LoginUtils.getUserInfo().data.token);
            }
            String sign = DataConvertUtils.convertParamsData(params,Constants.SERVER_KEY);
            params.put("sign",sign);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return params;
    }

    //get方式的参数
    public static String getTypeParams(String url, HashMap<String, String> maps) {
        StringBuilder sb = new StringBuilder();
        sb.append(url + "?");
        if (maps != null && !maps.isEmpty()) {
            for (String key : maps.keySet()) {
                sb.append(key).append("=").append(maps.get(key)).append("&");
            }
        }
        long currentTime = System.currentTimeMillis();
        sb.append("&time=").append(currentTime);
        String token = LoginUtils.getUserInfo().data.token;
        if (!TextUtils.isEmpty(token)) {
            sb.append("&token=").append(token);
        }

        maps.put("time", String.valueOf(currentTime));
        if (!TextUtils.isEmpty(token)) {
            maps.put("token", token);
        }
        String sign = DataConvertUtils.convertParamsData(maps, Constants.SERVER_KEY);
        sb.append("&sign=").append(sign);
        return sb.toString();
    }


}
