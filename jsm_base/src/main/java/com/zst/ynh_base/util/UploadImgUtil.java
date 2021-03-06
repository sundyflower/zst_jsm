package com.zst.ynh_base.util;

import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.zst.ynh_base.net.BaseResponseData;
import com.zst.ynh_base.uploadimg.ProgressHelper;
import com.zst.ynh_base.uploadimg.ProgressListener;
import com.zst.ynh_base.uploadimg.ProgressUIListener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class UploadImgUtil {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            //设置超时，不设置可能会报异常
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();




    public static class FileBean {
        private int picType;
        private String fileSrc;//文件路径
        private String upLoadKey;//上传文件的字段
        private HashMap<String, String> extarParms = new HashMap<String, String>();

        public void setFileSrc(String fileSrc) {
            this.fileSrc = fileSrc;
        }

        public String getFileSrc() {
            return fileSrc;
        }

        public String getUpLoadKey() {
            return upLoadKey;
        }

        public void setUpLoadKey(String upLoadKey) {
            this.upLoadKey = upLoadKey;
        }

        public void addExtraParms(String key, String value) {
            extarParms.put(key, value);
        }

        public HashMap<String, String> getExtraParms() {
            return extarParms;
        }

        public int getPicType() {
            return picType;
        }

        public void setPicType(int picType) {
            this.picType = picType;
        }
    }

    /************
     * 文件上传
     */
    public static void upLoadImg(final String serviceUrl, String sessionId, final FileBean bean, final ProgressListener listener) throws Exception {
        if (bean == null) {
            listener.onFailed(null, new Exception("图片上传失败"));
            return;
        }
        if (serviceUrl == null || serviceUrl.trim().length() == 0) {
            listener.onFailed(null, new Exception("图片上传失败"));
            return;
        }
        File file = new File(bean.getFileSrc());
        if (!file.exists()) {
            listener.onFailed(null, new Exception("图片文件不存在"));
            return;
        }

        //构造上传请求，类似web表单
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        builder.addFormDataPart("attach", file.getName(), RequestBody.create(null, file));
        Log.d("tag", file.getName());
        Set set = bean.getExtraParms().entrySet();
        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            builder.addFormDataPart(entry.getKey().toString(), entry.getValue().toString());
        }
        RequestBody requestBody = builder.build();
        if(listener!=null){
           requestBody = ProgressHelper.withProgress(requestBody,listener);
        }
        //进行包装，使其支持进度回调
        Request request = new Request.Builder()
                .addHeader("Cookie", "SESSIONID=" + sessionId)
                .url(serviceUrl)
                .post(requestBody).build();

        //开始请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException arg1) {
                listener.onFailed(call, new Exception("图片上传失败" + arg1.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response arg1) throws IOException {
                String result = arg1.body().string();
                BaseResponseData baseResponseData = JSON.parseObject(result, BaseResponseData.class);
                if (baseResponseData != null && "0".equals(baseResponseData.getCode())) {
                    listener.onSuccess(call, arg1);
                } else {
                    if (baseResponseData != null) {
                        listener.onFailed(call, new Exception("图片上传失败" + baseResponseData.getMessage()));
                    } else {
                        listener.onFailed(call, new Exception("图片上传失败"));
                    }
                }
            }
        });
    }

    public static void download(String url, final ProgressListener listener, final File file){
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        builder.get();
        Call call = client.newCall(builder.build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                listener.onFailed(call,e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(listener!=null){
                    ResponseBody responseBody = ProgressHelper.withProgress(response.body(), listener);
                    BufferedSource source = responseBody.source();

                    BufferedSink sink = Okio.buffer(Okio.sink(file));
                    source.readAll(sink);
                    sink.flush();
                    source.close();
                }


            }
        });
    }
}
