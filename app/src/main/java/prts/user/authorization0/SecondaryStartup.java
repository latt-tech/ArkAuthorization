package prts.user.authorization0;

// SecondaryStartup.java (使用XML布局版本)
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.Toast;
import java.io.IOException;
import android.content.pm.PackageManager;
import android.os.Build;

public class SecondaryStartup extends Activity {

    private static final int SPLASH_DELAY = 11000; // 11秒延迟
    private static final int REQUEST_READ_STORAGE = 1;
    private long firstBackTime;
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - firstBackTime > 0) {
            firstBackTime = System.currentTimeMillis();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_startup);
        BootLogView bootLogView = new BootLogView(this);
        bootLogView.setBootCompleteListener(this);
        setContentView(bootLogView);
        
        try {
            Runtime.getRuntime().exec("mkdir -p /sdcard/DCIM/Authorization");
        } catch (IOException e) {
            Toast.makeText(getApplication(), ""+e, Toast.LENGTH_SHORT).show();
        }

        // 3秒后跳转到 VideoPlayerActivity
        new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    jumpToVideoPlayer();
                }
            }, SPLASH_DELAY);
        checkPermission();
    }
    
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                   REQUEST_READ_STORAGE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_READ_STORAGE && grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 使用运行时权限
            } else {
            }
        }
    }

    private void jumpToVideoPlayer() {
        try {
            Intent intent = new Intent(SecondaryStartup.this, VideoPlayerActivity.class);
            startActivity(intent);
            finish(); // 结束当前Activity
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }
    public void onBootComplete() {
        
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Handler().removeCallbacksAndMessages(null);
    }
}
