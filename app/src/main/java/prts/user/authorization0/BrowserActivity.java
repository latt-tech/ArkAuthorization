package prts.user.authorization0;

// BrowserActivity.java
// 隐藏的浏览器页面：不带 URI 单独打开时静默退出，不显示任何界面或提示；
// 通过 Intent 的 URI（getData()）传入网址时加载网页
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent() != null ? getIntent().getData() : null;
        if (uri == null) {
            // 未传入网址：静默退出，不加载布局、不提示
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_browser);

        webView = findViewById(R.id.browser_webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.loadUrl(uri.toString());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 返回键优先回退网页历史
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
