package tools.android.h5browser.h5;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CustomWebViewClient extends WebViewClient {

    public interface IWebViewNotifyListener {
        public void notifyPageStared(WebView view, String url, Bitmap favicon);
        public void notifyPageFinished(WebView view, String url);
        public void notifyPageError();
    }

    public interface IWebViewAwakeIntent {
        public void awake(Intent intent);
    }

    private IWebViewNotifyListener mNotifyListener = null;
    private IWebViewAwakeIntent mAwakeIntentListener = null;
    private Context mContext;

    public CustomWebViewClient(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if(url.startsWith("http:") || url.startsWith("https:") ) {
            return super.shouldOverrideUrlLoading(view, url);
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            if (null != mAwakeIntentListener) {
                mAwakeIntentListener.awake(intent);
            }
        } catch (android.content.ActivityNotFoundException anfe) {
        }
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (null != mNotifyListener) {
            mNotifyListener.notifyPageStared(view, url, favicon);
        }
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (null != mNotifyListener) {
            mNotifyListener.notifyPageFinished(view, url);
        }
        super.onPageFinished(view, url);
    }

    @Override
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
        handler.proceed();
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        if (null != mNotifyListener) {
            mNotifyListener.notifyPageError();
        }
    }

    @TargetApi(23)
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        try {
            Uri url = request.getUrl();
            String host = url.getHost();
            if ("127.0.0.1".equals(host)) {
                return;
            }
        } catch (Exception e) {

        }
        if (null != mNotifyListener) {
            mNotifyListener.notifyPageError();
        }
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, final HttpAuthHandler handler, final String host, final String realm) {
    }

    public void setNotifyListener(IWebViewNotifyListener listener) {
        this.mNotifyListener = listener;
    }

    public IWebViewAwakeIntent getAwakeIntentListener() {
        return mAwakeIntentListener;
    }

    public void setAwakeIntentListener(IWebViewAwakeIntent awakeIntentListener) {
        this.mAwakeIntentListener = awakeIntentListener;
    }
}
