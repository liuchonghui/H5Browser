package tools.android.h5browser;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.tencent.sonic.sdk.SonicCacheInterceptor;
import com.tencent.sonic.sdk.SonicConfig;
import com.tencent.sonic.sdk.SonicEngine;
import com.tencent.sonic.sdk.SonicSession;
import com.tencent.sonic.sdk.SonicSessionConfig;
import com.tencent.sonic.sdk.SonicSessionConnection;
import com.tencent.sonic.sdk.SonicSessionConnectionInterceptor;

import tools.android.h5browser.h5.CustomWebChromeClient;
import tools.android.h5browser.h5.CustomWebView;
import tools.android.h5browser.h5.CustomWebViewClient;
import tools.android.h5browser.sonic.SonicRuntimeImpl;
import tools.android.h5browser.sonic.SonicSessionClientImpl;

public class H5Activity extends Activity {
    private LinearLayout mQuitLayout;
    private LinearLayout mBackLayout;
    private LinearLayout mForwardLayout;
    private LinearLayout mRefreshLayout;
    private LinearLayout mToolbarTmpLayout;
    private LinearLayout mCloseBtnLayout;
    private LinearLayout mEmptyLayout;
    private RelativeLayout mContentLayout;
    private Button mRefreshBtn;
    private ImageView mPrevImg;
    private ImageView mNextImg;

    private CustomWebView mWebView;
    private CustomWebViewClient mWebViewClient;

    private boolean mReceivedError;

    AsyncTask<Void, Void, Integer> mTast;

    private SonicSession sonicSession;
    private String url;
    private SonicSessionClientImpl sonicSessionClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        url = getSegmentAnyWay(getIntent().getData(), getIntent().getExtras(), "url");
        initTencentSonic();

        setContentView(R.layout.h5br_page_layout);

        initViews();
        initWebView();

    }

    private void initTencentSonic() {
        if (!SonicEngine.isGetInstanceAllowed()) {
            SonicEngine.createInstance(new SonicRuntimeImpl(getApplication()), new SonicConfig.Builder().build());
        }

        SonicSessionConfig.Builder sessionConfigBuilder = new SonicSessionConfig.Builder();
        sessionConfigBuilder.setSupportLocalServer(true);

        // create sonic session and run sonic flow
        sonicSession = SonicEngine.getInstance().createSession(url, sessionConfigBuilder.build());
        if (null != sonicSession) {
            sonicSession.bindClient(sonicSessionClient = new SonicSessionClientImpl());
        } else {
            // this only happen when a same sonic session is already running,
            // u can comment following codes to feedback as a default mode.
            // throw new UnknownError("create session fail!");
            Toast.makeText(this, "create sonic session fail!", Toast.LENGTH_LONG).show();
            throw new UnknownError("create session fail!");
        }
    }

    private void initViews() {
        mQuitLayout = (LinearLayout) findViewById(R.id.h5br_page_quit_layout);
        mBackLayout = (LinearLayout) findViewById(R.id.h5br_page_back_layout);
        mForwardLayout = (LinearLayout) findViewById(R.id.h5br_page_forward_layout);
        mRefreshLayout = (LinearLayout) findViewById(R.id.h5br_page_refresh_layout);
        mWebView = (CustomWebView) findViewById(R.id.h5br_page_webview_container);
        mToolbarTmpLayout = (LinearLayout) findViewById(R.id.h5br_page_bottom_bar_tmp_layout);
        mCloseBtnLayout = (LinearLayout) findViewById(R.id.h5br_page_close_layout);
        mContentLayout = (RelativeLayout) findViewById(R.id.h5br_page_content_layout);
        mEmptyLayout = (LinearLayout) findViewById(R.id.h5br_page_empty_layout);
        mRefreshBtn = (Button) findViewById(R.id.h5br_page_refresh_btn);
        mRefreshBtn.setOnClickListener(mRetryListener);
        mPrevImg = (ImageView) findViewById(R.id.h5br_page_prev_btn);
        mNextImg = (ImageView) findViewById(R.id.h5br_page_next_btn);

        mQuitLayout.setOnClickListener(mQuitListener);
        mCloseBtnLayout.setOnClickListener(mCloseListener);
        mBackLayout.setOnClickListener(mBackListener);
        mForwardLayout.setOnClickListener(mForwardListener);
        mRefreshLayout.setOnClickListener(mRefreshListener);
        mToolbarTmpLayout.setVisibility(View.INVISIBLE);

        updateStatus();
    }

    private void initWebView() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            mWebView.setBackgroundColor(0x00000000);
        }

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (sonicSession != null) {
                    sonicSession.getSessionClient().pageFinish(url);
                }
            }

            @TargetApi(21)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return shouldInterceptRequest(view, request.getUrl().toString());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (sonicSession != null) {
                    return (WebResourceResponse) sonicSession.getSessionClient().requestResource(url);
                }
                return null;
            }
        });

        WebSettings webSettings = mWebView.getSettings();

        // add java script interface
        // note:if api level lower than 17(android 4.2), addJavascriptInterface has security
        // issue, please use x5 or see https://developer.android.com/reference/android/webkit/
        // WebView.html#addJavascriptInterface(java.lang.Object, java.lang.String)
        webSettings.setJavaScriptEnabled(true);
        mWebView.removeJavascriptInterface("searchBoxJavaBridge_");

        // init webview settings
        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);


        // webview is ready now, just tell session client to bind
        if (sonicSessionClient != null) {
            sonicSessionClient.bindWebView(mWebView);
            sonicSessionClient.clientReady();
        } else { // default mode
            mWebView.loadUrl(url);
        }
    }

    private String getSegment(Bundle extra, String key) {
        String extraString = null;
        if (extra != null) {
            extraString = extra.getString(key);
        }
        return extraString;
    }

    private String getSegmentAnyWay(Uri uri, Bundle extra, String key) {
        String extraString = null;
        if (uri != null) {
            extraString = uri.getQueryParameter(key);
        }
        if (extraString == null || extraString.length() == 0) {
            extraString = getSegment(extra, key);
        }
        return extraString;
    }

    private void handleFailed() {
        mContentLayout.setVisibility(View.GONE);
        mEmptyLayout.setVisibility(View.VISIBLE);
    }

    private void showContent() {
        mContentLayout.setVisibility(View.VISIBLE);
        mEmptyLayout.setVisibility(View.GONE);
    }

    private boolean isErrorPageShowed() {
        if (null != mEmptyLayout && View.VISIBLE == mEmptyLayout.getVisibility()) {
            return true;
        }
        return false;
    }

    private void updateStatus() {
        if (null != mWebView) {
            if (mWebView.canGoBack()) {
                mPrevImg.setImageResource(R.drawable.h5br_page_back_btn_supprt_rtl);
            } else {
                mPrevImg.setImageResource(R.drawable.h5br_page_back_btn_pressed_supprt_rtl);
            }

            if (mWebView.canGoForward()) {
                mNextImg.setImageResource(R.drawable.h5br_page_forward_btn_supprt_rtl);
            } else {
                mNextImg.setImageResource(R.drawable.h5br_page_forward_btn_pressed_supprt_rtl);
            }
        }
    }

    private CustomWebViewClient.IWebViewNotifyListener mWebViewNotifyListener = new CustomWebViewClient.IWebViewNotifyListener() {

        @Override
        public void notifyPageStared(WebView view, String url, Bitmap favicon) {

        }

        @Override
        public void notifyPageFinished(WebView view, String url) {
            mToolbarTmpLayout.setVisibility(View.VISIBLE);
            if (!mReceivedError) {
                showContent();
            }
            updateStatus();
        }

        @Override
        public void notifyPageError() {
            mReceivedError = true;
            handleFailed();
        }
    };

    private View.OnClickListener mRetryListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mReceivedError = false;
            if (mWebView.canGoBack() || mWebView.canGoForward()) {
                mWebView.reload();
            } else {
                if (sonicSession != null) {
                    sonicSession.refresh();
                }
            }
        }
    };

    private View.OnClickListener mQuitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private View.OnClickListener mCloseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private View.OnClickListener mBackListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (null != mWebView && mWebView.canGoBack()) {
                mWebView.goBack();
            }
        }
    };

    private View.OnClickListener mForwardListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (null != mWebView && mWebView.canGoForward()) {
                mWebView.goForward();
            }
        }
    };

    private View.OnClickListener mRefreshListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (null != mWebView) {
                if (isErrorPageShowed()) {
                    mReceivedError = false;
                    if (!mWebView.canGoBack() && !mWebView.canGoForward()) {
                        if (sonicSession != null) {
                            sonicSession.refresh();
                        }
                        return;
                    }
                }

                mWebView.reload();
            }
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (sonicSession != null) {
            sonicSession.refresh();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        try {
            if (null != sonicSession) {
                sonicSession.destroy();
                sonicSession = null;
            }
            if (null != mWebView) {
                mWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
                mWebView.clearHistory();

                ((ViewGroup) mWebView.getParent()).removeView(mWebView);
                mWebView.destroy();
                mWebView = null;
            }

            if (null != mTast) {
                mTast.cancel(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && null != mWebView && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
