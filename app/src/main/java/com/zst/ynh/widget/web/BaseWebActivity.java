package com.zst.ynh.widget.web;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.blankj.utilcode.util.StringUtils;
import com.zst.ynh.BuildConfig;
import com.zst.ynh.R;
import com.zst.ynh.config.ApiUrl;
import com.zst.ynh.utils.WeakHandler;
import com.zst.ynh.utils.WebViewUtils;
import com.zst.ynh_base.mvp.view.BaseActivity;
import com.zst.ynh_base.util.VersionUtil;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;

public abstract class BaseWebActivity extends BaseActivity {

    @BindView(R.id.contain_fragment)
    protected FrameLayout containFragment;
    protected WebView webView;
    ProgressBar progressBar;
    protected String titleStr;
    protected String url;
    protected boolean isSetSession;//是否需要设置sessionid 的cookie
    protected boolean isLoadFailed;

    @Override
    public void onRetry() {
        webView.loadUrl(url);
        isLoadFailed = false;
    }

    @Override
    public void initView() {
        webView = new WebView(this);
        containFragment.removeAllViews();
        containFragment.addView(webView);
        initWebView();
    }

    private void initWebView() {

        progressBar = findViewById(R.id.progress_bar);
        if (getIntent() != null) {
            initViews();
            if (isSetSession) {
                WebViewUtils.synchronousWebCookies();
            }
        }
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        WebSettings settings = webView.getSettings();
        settings.setTextZoom(100);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setUserAgentString(settings.getUserAgentString() + "/" + BuildConfig.USER_AGENT + "/" + VersionUtil.getLocalVersion(this));

        addJavaScriptInterface();
        setWebClient();

        webView.loadUrl(url);
    }


    protected class BaseWebViewClient extends WebViewClient {

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            Log.d("WebActivity", "onReceivedError:" + description + ";failingUrl" + failingUrl);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return;
            }
            isLoadFailed = true;
            view.loadUrl("about:blank"); // 避免出现默认的错误界面
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            Log.d("WebActivity", "onReceivedError:" + error.getDescription());
            if (request.isForMainFrame()) { // 或者： if(request.getUrl().toString() .equals(getUrl()))
                // 在这里显示自定义错误页
                isLoadFailed = true;
                view.loadUrl("about:blank"); // 避免出现默认的错误界面
            }

        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            Log.d("WebActivity", "onReceivedHttpError:" + errorResponse.getReasonPhrase());
            if (request.isForMainFrame()) {
                view.loadUrl("about:blank"); // 避免出现默认的错误界面
                isLoadFailed = true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d("WebActivity", "onPageStarted:" + url);
            if (progressBar != null) {
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d("WebActivity", "onPageFinished:" + url);
            if (isLoadFailed) {
                loadErrorView();
                mTitleBar.setTitle("网页加载失败");
            } else {
                loadContentView();
            }

            if (view.canGoBack()) {
                mTitleBar.setLeftImageVisible(View.VISIBLE);
            } else {
                mTitleBar.setLeftImageVisible(View.GONE);
            }
        }
    }

    protected class BaseWebChromeClient extends WebChromeClient {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            Log.d("WebActivity", "onReceivedTitle:" + title);
            if (isLoadFailed) {
                return;
            }

            if (title.contains("404") || title.contains("500") || title.contains("Error") || title.contains("http") || title.contains("https") || title.contains(ApiUrl.BASE_URL)) {
                view.loadUrl("about:blank");// 避免出现默认的错误界面
                isLoadFailed = true;
                return;
            }

            if (!StringUtils.isEmpty(title)) {
                titleStr = title;
                mTitleBar.setTitle(title);
            }
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            Log.d("WebActivity", "onProgressChanged:" + newProgress);
            progressBar.setProgress(newProgress);
            if (newProgress == 100) {
                //加载完毕让进度条消失
                progressBar.setVisibility(View.INVISIBLE);
            }
            super.onProgressChanged(view, newProgress);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            // 如果先调用destroy()方法，则会命中if (isDestroyed()) return;这一行代码，需要先onDetachedFromWindow()，再
            ViewParent parent = webView.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(webView);
            }

//            if(isSetSession){
//                WebViewUtils.removeCookie();
//            }
            webView.stopLoading();
            // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
            webView.getSettings().setJavaScriptEnabled(false);
            webView.clearHistory();
            webView.clearView();
            webView.removeAllViews();
            webView.destroy();
        }
    }



    protected abstract void initViews();
    protected abstract void setWebClient();
    protected abstract void addJavaScriptInterface();

}
