package com.webviewdemo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 采用了不同webkit版本内核，4.4后直接使用了Chrome内核
 */
public class MainActivity extends AppCompatActivity {
    private final String TAG = "yxw";
    private WebView mWebView;
    private LinearLayout mLayout;
    private TextView mTitle;
    private TextView mLoadingStatus;
    private TextView mLoadingProgress;
    private Button mButton1;
    private Button mButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = findViewById(R.id.layout);
        mTitle = findViewById(R.id.title);
        mLoadingStatus = findViewById(R.id.loading_status);
        mLoadingProgress = findViewById(R.id.loading_progress);
        mButton1 = findViewById(R.id.button1);
        mButton2 = findViewById(R.id.button2);
        //mWebView = findViewById(R.id.webview);
        //也可自定义生成WebView，再添加到父容器中，避免WebView内存泄露
        //不在xml中定义 Webview ，而是在需要的时候在Activity中创建，并且Context使用 getApplicationgContext()
        mWebView = new WebView(getApplicationContext());
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        mWebView.setLayoutParams(params);
        mLayout.addView(mWebView);

        //Webview的最常用的工具类：WebSettings类、WebViewClient类、WebChromeClient类
        initWebSettings();
        //加载url
        initUrl();
        //使用本地webview打开
        initWebViewClient();
        initWebChromeClient();

        //WebView与 JS 的交互
        callMethod();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebSettings() {
        //对WebView进行配置和管理
        WebSettings webSettings = mWebView.getSettings();
        // 设置与Js交互的权限
        // 若加载的 html 里有JS 在执行动画等操作，会造成资源浪费（CPU、电量）
        // 在 onStop 和 onResume 里分别把 setJavaScriptEnabled() 给设置成 false 和 true 即可
        webSettings.setJavaScriptEnabled(true);

        //设置自适应屏幕，两者合用
        webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小

        //缩放操作
        webSettings.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        webSettings.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件

        //其他细节操作
        webSettings.setAllowFileAccess(true); //设置可以访问文件
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口,允许JS弹窗
        webSettings.setLoadsImagesAutomatically(true); //支持自动加载图片
        webSettings.setDefaultTextEncodingName("utf-8");//设置编码格式

        //设置WebView缓存
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        //缓存模式如下：
        //LOAD_CACHE_ONLY: 不使用网络，只读取本地缓存数据
        //LOAD_DEFAULT: （默认）根据cache-control决定是否从网络上取数据。
        //LOAD_NO_CACHE: 不使用缓存，只从网络获取数据.
        //LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。

    }

    private void initUrl() {
        //方式1. 加载一个网页：
        //mWebView.loadUrl("http://www.baidu.com/");
        //方式2：加载apk包中的html页面
        mWebView.loadUrl("file:///android_asset/test.html");
        //方式3：加载手机本地的html页面
        //mWebView.loadUrl("content://com.android.htmlfileprovider/sdcard/test.html");
    }

    private void initWebViewClient() {
        //处理各种通知 & 请求事件
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //JS调用android的方法，预先约定协议，然后解析对应不同的逻辑，有点像回调的方式
                Uri uri = Uri.parse(url);
                // 如果url的协议 = 预先约定的 js 协议
                // 就解析往下解析参数
                if ("js".equals(uri.getScheme())) {
                    // 如果 authority  = 预先约定协议里的 webview，即代表都符合约定的协议
                    // 所以拦截url,下面JS开始调用Android需要的方法
                    if ("webview".equals(uri.getAuthority())) {
                        Toast.makeText(MainActivity.this, "js传递到了Android侧", Toast.LENGTH_SHORT)
                                .show();
                        //如果要回调结果给js，就只能通过android调用js方法的形式
                    }
                } else {
                    //复写shouldOverrideUrlLoading()方法，使得打开网页时不调用系统浏览器，而是在本WebView中显示
                    view.loadUrl(url);
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                //设定加载开始的操作
                //作用：开始载入页面调用的，我们可以设定一个loading的页面，告诉用户程序在等待网络响应。
                Log.v(TAG, "onPageStarted");
                mLoadingStatus.setText("加载开始");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //设定加载结束的操作
                //作用：在页面加载结束时调用。我们可以关闭loading条，切换程序动作。
                Log.v(TAG, "onPageFinished");
                mLoadingStatus.setText("加载结束");
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                //设定加载资源的操作
                //作用：在加载页面资源时会调用，每一个资源（比如图片）的加载都会调用一次。
                Log.v(TAG, "onLoadResource");
            }

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                    WebResourceError error) {
                //作用：加载页面的服务器出现错误时（如404）调用
                //App里面使用webview控件的时候遇到了诸如404这类的错误的时候，若也显示浏览器里面的那种错误提示页面就显得很丑陋了，
                //这个时候就需要加载一个本地的错误提示页面，即webview如何加载一个本地的页面
                //写一个html文件（error_handle.html），用于出错时展示给用户看的提示页面
                Log.v(TAG, "onReceivedError,ErrorCode:" + error.getErrorCode());
                switch (error.getErrorCode()) {
                    case 404:
                        view.loadUrl("file:///android_assets/error_handle.html");
                        break;
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                //作用：处理https请求
                //webView默认是不处理https请求的，页面显示空白，需要以下处理：
                Log.v(TAG, "onReceivedSslError");
                handler.proceed();    //表示等待证书响应
                // handler.cancel();      //表示挂起连接，为默认方式
                // handler.handleMessage(null);    //可做其他处理
            }
        });

        // 特别注意：5.1以上默认禁止了https和http混用，以下方式是开启
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    private void initWebChromeClient() {
        //辅助 WebView 处理 Javascript 的对话框,网站图标,网站标题等等。
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                //获得网页的加载进度并显示
                Log.v(TAG, "onProgressChanged, newProgress:" + newProgress);
                mLoadingProgress.setText("加载进度：" + newProgress + "%");
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                //获取Web页中的标题
                Log.v(TAG, "onReceivedTitle:" + title);
                mTitle.setText("标题：" + title);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message,
                    final JsResult result) {
                //支持javascript的警告框
                new AlertDialog.Builder(MainActivity.this).setTitle("JsAlert")
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message,
                    final JsResult result) {
                //支持javascript的确认框
                new AlertDialog.Builder(MainActivity.this).setTitle("JsConfirm")
                        .setMessage(message)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .setCancelable(false)
                        .show();
                // 返回布尔值：判断点击时确认还是取消
                // true表示点击了确认；false表示点击了取消；
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                    JsPromptResult result) {
                //支持javascript输入框
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }
        });
    }

    private void callMethod() {

        //Android通过WebView调用 JS 代码，通过evaluateJavascript直接调用
        mButton1.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                mWebView.evaluateJavascript("test:callJS()", new ValueCallback<String>() {
                    //通过onJsAlert拦截会弹native的弹窗。
                    @Override
                    public void onReceiveValue(String value) {
                    }
                });
            }
        });

        mButton2.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                mWebView.evaluateJavascript("test:add(" + 3 + "," + 4 + ")",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                Toast.makeText(MainActivity.this, "3+4=" + value,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void webViewLife() {
        //激活WebView为活跃状态，能正常执行网页的响应
        mWebView.onResume();

        //当页面被失去焦点被切换到后台不可见状态，需要执行onPause
        //通过onPause动作通知内核暂停所有的动作，比如DOM的解析、plugin的执行、JavaScript执行。
        mWebView.onPause();

        //当应用程序(存在webview)被切换到后台时，这个方法不仅仅针对当前的webview而是全局的全应用程序的webview
        //它会暂停所有webview的layout，parsing，javascripttimer。降低CPU功耗。
        mWebView.pauseTimers();
        //恢复pauseTimers状态
        mWebView.resumeTimers();

        //销毁Webview
        //在关闭了Activity时，如果Webview的音乐或视频，还在播放。就必须销毁Webview
        //但是注意：webview调用destory时,webview仍绑定在Activity上
        //这是由于自定义webview构建时传入了该Activity的context对象
        //因此需要先从父容器中移除webview,然后再销毁webview:
        //rootLayout.removeView(webView);
        mWebView.destroy();
    }

    private void webViewBackForward() {
        //是否可以后退
        mWebView.canGoBack();
        //后退网页
        mWebView.goBack();

        //是否可以前进
        mWebView.canGoForward();
        //前进网页
        mWebView.goForward();

        //以当前的index为起始点前进或者后退到历史记录中指定的steps
        //如果steps为负数则为后退，正数则为前进
        int intsteps = 1;
        mWebView.goBackOrForward(intsteps);

        //常用情况是，系统返回键控制网页后退
        //如果在不做任何处理前提下，浏览网页时点击系统的返回键,整个浏览器会调用finish()而直接结束自身
        //需要处理成：点击返回键后，是网页回退而不是退出浏览器
        //解决方案：在当前Activity中处理并消费掉onBackPressed
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        //在Activity销毁（WebView）的时候，先让WebView加载null内容，然后移除WebView，再销毁WebView，最后置空。避免WebView内存泄露
        if (mWebView != null) {
            mWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mWebView.clearHistory();
            ((ViewGroup) mWebView.getParent()).removeView(mWebView);
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }

    private void webViewCache() {
        //清除网页访问留下的缓存
        //由于内核缓存是全局的因此这个方法不仅仅针对webview而是针对整个应用程序.
        mWebView.clearCache(true);

        //清除当前webview访问的历史记录
        //只会webview访问历史记录里的所有记录除了当前访问记录
        mWebView.clearHistory();

        //这个api仅仅清除自动完成填充的表单数据，并不会清除WebView存储到本地的数据
        mWebView.clearFormData();
    }
}
