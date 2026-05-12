package prts.user.authorization0;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.FileOutputStream;
import android.widget.Toast;
import android.content.Intent;

public class VideoPlayerActivity extends Activity {
    private static final int REQUEST_READ_STORAGE = 1;
    private static final String TAG = "VideoPlayer";

    // UI组件
    private VideoView videoView;
    private ImageView careerIcon;  // 左下角职业图标
    private TextView operatorName;  // 职业图标上方的干员名称
    private TextView operatorStar;  // 职业图标上方的星级
    private TextView introductionText;  // 右下角介绍文本
    
    
    // 视频相关
    private List<String> videoPaths = new ArrayList<>();
    private Map<String, CharacterConfig> characterConfigs = new HashMap<>();
    private Random random = new Random();

    // Handler和GC
    private Handler handler = new Handler();
    private Runnable gcRunnable = new Runnable() {
        @Override
        public void run() {
            System.gc();
            handler.postDelayed(this, 10 * 60 * 1000);
        }
    };

    private long firstBackTime;
    private static final String TARGET_FOLDER = "Authorization";

    // 角色配置类
    private static class CharacterConfig {
        String videoPath;
        Bitmap careerIcon;
        String operatorName;
        int starLevel;
        String introduction;
        String careerText;

        CharacterConfig(String videoPath, Bitmap careerIcon, String operatorName, 
                        int starLevel, String introduction, String careerText) {
            this.videoPath = videoPath;
            this.careerIcon = careerIcon;
            this.operatorName = operatorName;
            this.starLevel = starLevel;
            this.introduction = introduction;
            this.careerText = careerText;
        }
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - firstBackTime > 0) {
            firstBackTime = System.currentTimeMillis();
            return;
        }
        System.gc();
        super.onBackPressed();
        System.gc();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler.post(gcRunnable);
        

        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_video_player);

        // 初始化UI组件
        initUIComponents();

        // 检查权限
        if (checkPermission()) {
            loadVideosAndConfigs();
            

            // 在启动时请求权限
            
        }

        // 设置视频播放完成监听器
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    playVideo(0);
                }
            });
    }
    

    

    private void initUIComponents() {
        videoView = findViewById(R.id.videoView);
        careerIcon = findViewById(R.id.career_icon);
        operatorName = findViewById(R.id.operator_name);
        operatorStar = findViewById(R.id.operator_star);
        introductionText = findViewById(R.id.introduction_text);
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
                loadVideosAndConfigs();
            }
        }
    }

    private void loadVideosAndConfigs() {
        File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File targetDir = new File(dcimDir, TARGET_FOLDER);

        if (targetDir.exists() && targetDir.isDirectory()) {
            scanVideosAndConfigs(targetDir);
        }

        if (!videoPaths.isEmpty()) {
            Collections.shuffle(videoPaths);
            playVideo(0);
        }
    }

    private void scanVideosAndConfigs(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanVideosAndConfigs(file);
                } else {
                    String fileName = file.getName().toLowerCase();
                    String filePath = file.getAbsolutePath();

                    // 检查是否是视频文件
                    if (fileName.endsWith(".mp4") || fileName.endsWith(".3gp") || 
                        fileName.endsWith(".mkv") || fileName.endsWith(".webm")) {
                        videoPaths.add(filePath);
                    }
                    // 检查是否是.usr配置文件（实际上是zip）
                    else if (fileName.endsWith(".usr")) {
                        try {
                            CharacterConfig config = parseCharacterConfig(filePath);
                            if (config != null) {
                                characterConfigs.put(fileName, config);
                                videoPaths.add(config.videoPath);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse character config: " + filePath, e);
                        }
                    }
                }
            }
        }
    }

    private CharacterConfig parseCharacterConfig(String usrFilePath) {
        try {
            FileInputStream fis = new FileInputStream(usrFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;

            String videoPath = null;
            Bitmap careerIcon = null;
            String operatorName = null;
            int starLevel = 0;
            String introduction = null;
            String careerText = null;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.equals("char.mp4")) {
                    // 提取视频文件到临时目录
                    File tempFile = File.createTempFile("char_", ".mp4", getCacheDir());
                    videoPath = tempFile.getAbsolutePath();
                    extractZipEntry(zis, tempFile);
                } 
                else if (entryName.equals("职业.png")) {
                    // 读取职业图标
                    byte[] buffer = readZipEntry(zis);
                    careerIcon = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                } 
                else if (entryName.equals("介绍.txt")) {
                    // 读取介绍文件
                    String content = readZipEntryAsString(zis);
                    introduction = parseIntroduction(content);
                    careerText = parseCareer(content);
                } 
                else if (entryName.equals("信息.txt")) {
                    // 读取信息文件
                    String content = readZipEntryAsString(zis);
                    String[] lines = content.split("\n");
                    if (lines.length >= 2) {
                        operatorName = lines[0].trim();
                        try {
                            starLevel = Integer.parseInt(lines[1].trim());
                        } catch (NumberFormatException e) {
                            starLevel = 1;
                        }
                    }
                }

                zis.closeEntry();
            }

            zis.close();
            fis.close();

            if (videoPath != null && careerIcon != null && operatorName != null) {
                return new CharacterConfig(videoPath, careerIcon, operatorName, 
                                           starLevel, introduction, careerText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing character config", e);
        }
        return null;
    }

    private String parseIntroduction(String content) {
        // 解析介绍内容
        String[] lines = content.split("\n");
        StringBuilder introBuilder = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("[名称]") || line.startsWith("[性别]") || 
                line.startsWith("[阵营]") || line.startsWith("[职业]")) {
                introBuilder.append(line).append("\n");
            }
        }

        return introBuilder.toString();
    }

    private String parseCareer(String content) {
        // 提取职业信息
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("[职业]")) {
                return line.substring(4).trim();
            }
        }
        return "";
    }

    private byte[] readZipEntry(ZipInputStream zis) throws Exception {
        List<Byte> byteList = new ArrayList<>();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = zis.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                byteList.add(buffer[i]);
            }
        }

        byte[] result = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            result[i] = byteList.get(i);
        }

        return result;
    }

    private String readZipEntryAsString(ZipInputStream zis) throws Exception {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(zis));
        String line;

        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }

        return content.toString();
    }

    private void extractZipEntry(ZipInputStream zis, File outputFile) throws Exception {
        FileOutputStream fos = new FileOutputStream(outputFile);
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = zis.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }

        fos.close();
    }

    private void playVideo(int index) {
        if (videoPaths.isEmpty()) return;

        try {
            Thread.sleep(100);
            Log.d(TAG, "Sleep Success");
        } catch (InterruptedException e) {
            Log.d(TAG, "Sleep interrupted: " + e);
        }

        String videoPath = videoPaths.get(index);
        videoView.setVideoURI(Uri.parse(videoPath));
        videoView.start();

        // 尝试显示对应的角色信息
        displayCharacterInfo(videoPath);
    }

    private void displayCharacterInfo(String videoPath) {
        // 查找对应的角色配置
        for (final CharacterConfig config : characterConfigs.values()) {
            if (config.videoPath.equals(videoPath)) {
                // 更新UI
                runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (config.careerIcon != null) {
                                careerIcon.setImageBitmap(config.careerIcon);
                            }
                            if (!TextUtils.isEmpty(config.operatorName)) {
                                operatorName.setText(config.operatorName);
                            }
                            operatorStar.setText(getStarText(config.starLevel));
                            if (!TextUtils.isEmpty(config.introduction)) {
                                introductionText.setText(config.introduction);
                            }

                            // 显示UI元素
                            careerIcon.setVisibility(View.VISIBLE);
                            operatorName.setVisibility(View.VISIBLE);
                            operatorStar.setVisibility(View.VISIBLE);
                            introductionText.setVisibility(View.VISIBLE);
                        }
                    });
                return;
            }
        }

        // 如果没有找到配置，隐藏UI元素
        runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    careerIcon.setVisibility(View.GONE);
                    operatorName.setVisibility(View.GONE);
                    operatorStar.setVisibility(View.GONE);
                    introductionText.setVisibility(View.GONE);
                }
            });
    }

    private String getStarText(int starLevel) {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < starLevel; i++) {
            stars.append("★");
        }
        return stars.toString();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return false;
            case KeyEvent.KEYCODE_VOLUME_UP:
                playNextRandomVideo();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void playNextRandomVideo() {
        if (videoPaths.isEmpty()) return;
        playVideo(random.nextInt(videoPaths.size()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) {
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null && !videoView.isPlaying()) {
            videoView.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(gcRunnable);
        
        if (videoView != null) {
            videoView.stopPlayback();
        }

        // 清理角色配置中的Bitmap
        for (CharacterConfig config : characterConfigs.values()) {
            if (config.careerIcon != null && !config.careerIcon.isRecycled()) {
                config.careerIcon.recycle();
            }
        }
        characterConfigs.clear();
    }
}
