package prts.user.authorization0;

// FileChooserActivity.java
// 简易文件选择器：浏览存储目录，选择视频/图片后跳转 MediaPlayerActivity 播放
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FileChooserActivity extends Activity {

    private ListView listView;
    private TextView currentPathView;
    private File currentDir;

    // 支持播放/显示的文件扩展名
    private static final List<String> MEDIA_EXTENSIONS = Arrays.asList(
        ".mp4", ".3gp", ".mkv", ".webm", ".mov", ".m4v",
        ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp");

    private final List<File> displayedFiles = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);

        listView = findViewById(R.id.file_list);
        currentPathView = findViewById(R.id.current_path);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                                     new ArrayList<String>());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onItemClicked(position);
            }
        });

        // 优先使用传入的目录，否则从外部存储根目录开始
        File startDir = null;
        String dirPath = getIntent() != null ? getIntent().getStringExtra("dir") : null;
        if (!TextUtils.isEmpty(dirPath)) {
            startDir = new File(dirPath);
        }
        if (startDir == null || !startDir.isDirectory()) {
            startDir = Environment.getExternalStorageDirectory();
        }
        navigateTo(startDir);
    }

    private void onItemClicked(int position) {
        if (position == 0) {
            // 第一项是 ".."，返回上级目录
            if (currentDir != null && currentDir.getParentFile() != null) {
                navigateTo(currentDir.getParentFile());
            }
            return;
        }

        File file = displayedFiles.get(position - 1);
        if (file.isDirectory()) {
            navigateTo(file);
        } else {
            openMedia(file);
        }
    }

    private void openMedia(File file) {
        try {
            Intent intent = new Intent(this, MediaPlayerActivity.class);
            intent.setData(Uri.fromFile(file));
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "打开失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateTo(File dir) {
        if (dir == null || !dir.canRead()) {
            Toast.makeText(this, "无法访问该目录", Toast.LENGTH_SHORT).show();
            return;
        }
        currentDir = dir;
        currentPathView.setText(dir.getAbsolutePath());

        displayedFiles.clear();
        List<String> names = new ArrayList<>();
        names.add("..");

        File[] files = dir.listFiles();
        if (files != null) {
            List<File> dirs = new ArrayList<>();
            List<File> mediaFiles = new ArrayList<>();
            for (File f : files) {
                if (f.isDirectory()) {
                    dirs.add(f);
                } else if (isMediaFile(f)) {
                    mediaFiles.add(f);
                }
            }
            Comparator<File> byName = new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            };
            Collections.sort(dirs, byName);
            Collections.sort(mediaFiles, byName);

            for (File f : dirs) {
                displayedFiles.add(f);
                names.add(f.getName() + "/");
            }
            for (File f : mediaFiles) {
                displayedFiles.add(f);
                names.add(f.getName());
            }
        }

        adapter.clear();
        adapter.addAll(names);
        adapter.notifyDataSetChanged();
    }

    private boolean isMediaFile(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : MEDIA_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && currentDir != null) {
            File parent = currentDir.getParentFile();
            // 未到达存储根目录时，返回键用于向上导航
            File root = Environment.getExternalStorageDirectory();
            if (parent != null && !currentDir.equals(root)) {
                navigateTo(parent);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
