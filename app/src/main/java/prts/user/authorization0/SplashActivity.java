package prts.user.authorization0;

// SplashActivity.java
// SecondaryStartup.java (使用XML布局版本)
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.Toast;
import android.os.Build;
import android.provider.Settings;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;

public class SplashActivity extends Activity {

    private static final int SPLASH_DELAY = 3000; // 3秒延迟 
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1001;
    private boolean isCheckingPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_startup);
        
        checkOverlayPermission();

        // 3秒后跳转到 VideoPlayerActivity
        new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    jumpToVideoPlayer();
                }
            }, SPLASH_DELAY);
    }

    private void jumpToVideoPlayer() {
        // 检查权限前重置标记
        isCheckingPermission = false;

        // 先检查权限，如果有权限才继续执行
        if (checkOverlayPermission()) {
            startVideoPlayerAndService();
        }
    }
    
    /**
     * 检查悬浮窗权限的方法
     */
    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!Settings.canDrawOverlays(this)) {
        // 没有权限，请求权限
            requestOverlayPermission();
             return false;
             }
             }
             return true;
            }

                private void startVideoPlayerAndService() {
                try {
                    new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashActivity.this, VideoPlayerActivity.class);
                startActivity(intent);
                }
            }, 2000);
            /* 
            Intent intent = new Intent(SplashActivity.this, VideoPlayerActivity.class);
                startActivity(intent);
*/
            Intent keepalive = new Intent(SplashActivity.this, KeepAliveService.class);
        try {
            startService(keepalive);
            } catch (Exception e) {
            stopService(keepalive);
        Toast.makeText(this, "启动服务失败，请授予悬浮窗权限！", Toast.LENGTH_SHORT).show();
    }
    finish(); // 结束当前Activity
    } catch (Exception e) {
        e.printStackTrace();
            Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
            }
                }
    private void requestOverlayPermission() {
        isCheckingPermission = true;

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("PRTS Analysis OS")
            .setMessage("请博士授予权限。\n\n允许显示在其他应用上方，这样才能确保通行证运行正常。\n\n请在接下来的窗口里面找到 “通行认证OS” 并授予权限。")
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dia, int which) {
                    Intent settings = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())
                    );
                    startActivityForResult(settings, OVERLAY_PERMISSION_REQUEST_CODE);
                }
            })
            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isCheckingPermission = false;
                    Toast.makeText(SplashActivity.this, "需要悬浮窗权限才能正常使用", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    isCheckingPermission = false;
                    finish();
                }
            })
            .create();
        dialog.show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE && isCheckingPermission) {
            isCheckingPermission = false;

            // 延迟检查，确保权限状态已更新
            new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (Settings.canDrawOverlays(SplashActivity.this)) {
                                // 用户授予了权限
                                startVideoPlayerAndService();
                            } else {
                                // 用户拒绝了权限
                                Toast.makeText(SplashActivity.this, "需要悬浮窗权限才能正常使用", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            // 低于6.0的系统，直接启动
                            startVideoPlayerAndService();
                        }
                    }
                }, 300); // 延迟300ms确保权限状态更新
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 处理从设置返回但没有触发onActivityResult的情况
        if (isCheckingPermission) {
            new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (Settings.canDrawOverlays(SplashActivity.this)) {
                                isCheckingPermission = false;
                                startVideoPlayerAndService();
                            }
                        }
                    }
                }, 500);
        }
    }
    
    
    

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Handler().removeCallbacksAndMessages(null);
    }
}
