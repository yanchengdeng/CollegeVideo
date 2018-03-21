package net.zgxyzx.video.common.ddzx;

import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DataConvertUtils {

    private static final String TAG = DataConvertUtils.class.getSimpleName();

    public static final char[] HEX_DIGITS = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'}; //HEX_DIGITS为16进制字符

    /*
     * 传数组
     * 数组排序
     * 数组拼接成字符串
     */
    public static String sortLetter(String [] input){
        Arrays.sort(input,String.CASE_INSENSITIVE_ORDER);
        StringBuilder _sb = new StringBuilder();
        for (int i=0;i<input.length;i++){
            if(i > 0) {
                _sb.append("&");
            }
            _sb.append(input[i]);
        }
        return _sb.toString();
    }


    /*
     * md5
     */
    public static String MD5(String s) {
        try {
            byte[] btInput = s.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = HEX_DIGITS[byte0 >>> 4 & 0xf];
                str[k++] = HEX_DIGITS[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 转换成加密数据
     * @param paramsMap
     * @return
     */
    public static String convertParamsData(Map<String,String> paramsMap, String key)  {
        Set<Map.Entry<String,String>> set = paramsMap.entrySet();
        String[] paramsArray = new String[set.size()];
        int position = 0;
        for(Iterator<Map.Entry<String,String>> iter = set.iterator(); iter.hasNext();){
            Map.Entry<String,String> element = iter.next();
            paramsArray[position] = element.getKey()+"="+element.getValue();
            position++;
        }
        //MLog.e(TAG,"得到后的数组："+Arrays.toString(paramsArray));

        String return_string = sortLetter(paramsArray);
        //MLog.e(TAG,"排序拼接后的字符串："+return_string);

        String newstr=return_string+key;
       /// MLog.e(TAG,"加key后的新字符串："+newstr);
        @SuppressWarnings("static-access")
        String return_newstr= MD5( newstr);
        //MLog.e(TAG,"md5加密后显示: "+return_newstr);
        /*String return_bigstr= return_newstr.toUpperCase();
        Log.e(TAG,"转换成大写后："+return_bigstr);*/
        return return_newstr;
    }


    /**
     * 转换成加密数据
     * @param paramsMap
     * @return
     */
    public static String convertParamsData(JSONObject paramsMap, String serviceKey)  {
        String[] paramsArray = new String[paramsMap.length()];
//        int position = 0;
//        for(Iterator<Map.Entry<String,String>> iter = set.iterator(); iter.hasNext();){
//            Map.Entry<String,String> element = iter.next();
//            paramsArray[position] = element.getKey()+"="+element.getValue();
//            position++;
//        }


        Iterator iterator = paramsMap.keys();
        try {
            int position = 0;
            while(iterator.hasNext()){
               String key = (String) iterator.next();
                paramsArray[position] =  key+"="+paramsMap.getString(key);
                position++;
            }
        }catch (Exception E){
        }

        //MLog.e(TAG,"得到后的数组："+Arrays.toString(paramsArray));

        String return_string = sortLetter(paramsArray);
        //MLog.e(TAG,"排序拼接后的字符串："+return_string);

        String newstr=return_string+serviceKey;
        /// MLog.e(TAG,"加key后的新字符串："+newstr);
        @SuppressWarnings("static-access")
        String return_newstr= MD5( newstr);
        //MLog.e(TAG,"md5加密后显示: "+return_newstr);
        /*String return_bigstr= return_newstr.toUpperCase();
        Log.e(TAG,"转换成大写后："+return_bigstr);*/
        return return_newstr;
    }

}
