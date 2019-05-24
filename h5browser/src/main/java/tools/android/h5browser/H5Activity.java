package tools.android.h5browser;

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
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import tools.android.h5browser.h5.CustomWebChromeClient;
import tools.android.h5browser.h5.CustomWebView;
import tools.android.h5browser.h5.CustomWebViewClient;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        setContentView(R.layout.h5br_page_layout);

        initViews();
        initWebView();
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
        mWebViewClient = new CustomWebViewClient(this.getApplicationContext());
        mWebViewClient.setNotifyListener(mWebViewNotifyListener);
        CustomWebChromeClient webChromeClient = new CustomWebChromeClient(mWebView);
        mWebView.setWebChromeClient(webChromeClient);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            mWebView.setBackgroundColor(0x00000000);
        }
        mWebView.setWebViewClient(mWebViewClient);
        loadH5();
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

    private void loadH5() {
        try {
            Intent intent = getIntent();
            String url = getSegmentAnyWay(intent.getData(), intent.getExtras(), "url");
            if (!TextUtils.isEmpty(url)) {
                showContent();
                loadUrl(url);
                return;
            }

            handleFailed();
        } catch (Exception e) {
            handleFailed();
        }
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

    private void loadUrl(String url) {
        if (null != mWebView) {
            mWebView.loadUrl(url);
        }
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
                loadH5();
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
                        loadH5();
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
        loadH5();
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
