package net.zgxyzx.video.common.ddzx.beans;

import java.io.Serializable;

//AA 返回数据模板
public class AAResponse<T> implements Serializable {


    public String  resultCode;//0000
    public String msg;//查询成功,
    public T data;

}