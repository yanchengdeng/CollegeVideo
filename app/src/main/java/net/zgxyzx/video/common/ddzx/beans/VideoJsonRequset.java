package net.zgxyzx.video.common.ddzx.beans;


import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.blankj.utilcode.util.LogUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class VideoJsonRequset extends JsonRequest<JSONObject> {
    public VideoJsonRequset(int method, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url, jsonRequest == null?null:jsonRequest.toString(), listener, errorListener);
        LogUtils.w("dyc",url);
        LogUtils.w("dyc","---"+jsonRequest);
    }

    public VideoJsonRequset(String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        this(jsonRequest == null?0:1, url, jsonRequest, listener, errorListener);
    }

    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String je = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));
            BaseInfo baseInfo = new Gson().fromJson(je,BaseInfo.class);
            if (baseInfo.code==1) {
                return Response.success(new JSONObject(je), HttpHeaderParser.parseCacheHeaders(response));
            }else{
                return Response.error((new VolleyError(baseInfo.msg)));
            }
        } catch (UnsupportedEncodingException var3) {
            return Response.error(new ParseError(var3));
        } catch (JSONException var4) {
            return Response.error(new ParseError(var4));
        }
    }

    public static String getErrorMessage(Object error) {
        if (error instanceof TimeoutError) {
            return "连接服务器失败!";
        } else if (isServerProblem(error)) {
            return handleServerError(error);
        } else if (isNetworkProblem(error)) {
            return "无网络连接!";
        }
        return "网络异常,请稍后再试!";
    }

    private static boolean isNetworkProblem(Object error) {
        return (error instanceof NetworkError)
                || (error instanceof NoConnectionError);
    }

    private static boolean isServerProblem(Object error) {
        return (error instanceof ServerError)
                || (error instanceof AuthFailureError);
    }

    private static String handleServerError(Object err) {
        VolleyError error = (VolleyError) err;

        NetworkResponse response = error.networkResponse;
        if (response != null) {
            switch (response.statusCode) {
                case 404:
                case 422:
                case 401:
                    try {
                        // server might return error like this { "error":
                        // "Some error occured" }
                        // Use "Gson" to parse the result
                        HashMap<String, String> result = new Gson().fromJson(
                                new String(response.data),
                                new TypeToken<Map<String, String>>() {
                                }.getType());

                        if (result != null && result.containsKey("error")) {
                            return result.get("error");
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // invalid request
                    return error.getMessage();

                default:
                    return "连接服务器失败!";
            }
        }
        return "网络异常,请稍后再试!";
    }

}
