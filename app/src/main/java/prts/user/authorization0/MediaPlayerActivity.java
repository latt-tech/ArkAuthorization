package prts.user.authorization0;

// MediaPlayerActivity.java
// 通过 WebView 播放视频 / 显示图片，文件地址由 Intent 的 URI（getData()）传入
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class MediaPlayerActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_media_player);

        webView = findViewById(R.id.media_webview);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // 允许视频自动播放（无需用户手势）
            settings.setMediaPlaybackRequiresUserGesture(false);
        }
        webView.setWebChromeClient(new WebChromeClient());

        Uri uri = getIntent() != null ? getIntent().getData() : null;
        if (uri == null) {
            // 未传入文件地址时，回退到文件选择器
            startActivity(new Intent(this, FileChooserActivity.class));
            finish();
            return;
        }

        loadMedia(uri);
    }

    private void loadMedia(Uri uri) {
        String mime = resolveMimeType(uri);
        String src = uri.toString();
        String html;

        if (mime != null && mime.startsWith("video/")) {
            html = "<!DOCTYPE html><html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">"
                + "<style>html,body{margin:0;padding:0;height:100%;background:#000;}"
                + "video{width:100%;height:100%;object-fit:contain;}</style></head>"
                + "<body><video src=\"" + src + "\" autoplay controls></video></body></html>";
        } else if (mime != null && mime.startsWith("image/")) {
            html = "<!DOCTYPE html><html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">"
                + "<style>html,body{margin:0;padding:0;height:100%;background:#000;}"
                + "img{width:100%;height:100%;object-fit:contain;}</style></head>"
                + "<body><img src=\"" + src + "\"></body></html>";
        } else {
            Toast.makeText(this, "无法识别的文件类型", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 以文件地址作为 baseURL，保证 WebView 能加载本地 file:// / content:// 资源
        webView.loadDataWithBaseURL(src, html, "text/html", "utf-8", null);
    }

    private String resolveMimeType(Uri uri) {
        String mime = null;
        try {
            mime = getContentResolver().getType(uri);
        } catch (Exception ignored) {
        }
        if (TextUtils.isEmpty(mime)) {
            // 回退：按文件扩展名推断
            String url = uri.toString().toLowerCase();
            int dot = url.lastIndexOf('.');
            if (dot >= 0 && dot < url.length() - 1) {
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(url.substring(dot + 1));
            }
        }
        return mime;
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
