package prts.user.authorization0;

// ApplicationActivity.java
// 应用中心：图标+文字网格（一行4个），内置功能与系统应用统一按 Aa-Zz 排序
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ApplicationActivity extends Activity {

    private static class Entry {
        final String label;
        final Drawable icon;
        final Intent intent;
        final String packageName; // 内置功能为 null

        Entry(String label, Drawable icon, Intent intent) {
            this(label, icon, intent, null);
        }

        Entry(String label, Drawable icon, Intent intent, String packageName) {
            this.label = label;
            this.icon = icon;
            this.intent = intent;
            this.packageName = packageName;
        }
    }

    private final List<Entry> allEntries = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    private EntryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application);

        buildEntries();
        entries.addAll(allEntries);

        adapter = new EntryAdapter();

        GridView gridView = findViewById(R.id.app_grid);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view,
                                    int position, long id) {
                launchEntry(entries.get(position));
            }
        });
        // 长按打开应用详情
        gridView.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view,
                                           int position, long id) {
                openAppDetails(entries.get(position));
                return true;
            }
        });

        setupSearchBox();
    }

    private void setupSearchBox() {
        android.widget.EditText searchBox = findViewById(R.id.search_box);
        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntries(s.toString().trim());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
        // 输入法 Go/回车：若是网址则打开内置浏览器
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO
                    || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String text = v.getText().toString().trim();
                    if (isWebUrl(text)) {
                        openBrowser(text);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void filterEntries(String query) {
        entries.clear();
        if (query.isEmpty()) {
            entries.addAll(allEntries);
        } else {
            String q = query.toLowerCase();
            for (Entry entry : allEntries) {
                if (entry.label.toLowerCase().contains(q)) {
                    entries.add(entry);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // 判断输入是否为网址（含协议头，或形如 xxx.yyy 的域名且不含空格）
    private boolean isWebUrl(String text) {
        if (text.isEmpty() || text.contains(" ")) {
            return false;
        }
        String lower = text.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return true;
        }
        // 形如 example.com / example.com/path 或带端口
        return lower.matches("^[a-z0-9\\-]+(\\.[a-z0-9\\-]+)+(:\\d+)?(/.*)?$");
    }

    private void openBrowser(String url) {
        String full = url;
        String lower = full.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            full = "http://" + full;
        }
        try {
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.setData(android.net.Uri.parse(full));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开浏览器: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void buildEntries() {
        allEntries.clear();

        // 内置功能：媒体播放器
        allEntries.add(new Entry("媒体播放器",
                                 getResources().getDrawable(android.R.drawable.ic_media_play),
                                 new Intent(this, MediaPlayerActivity.class)));

        allEntries.addAll(loadLaunchableApps());

        // 统一按 Aa-Zz 排序（中文按拼音）
        final Collator collator = Collator.getInstance(Locale.CHINA);
        Collections.sort(allEntries, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                return collator.compare(a.label, b.label);
            }
        });
    }

    private List<Entry> loadLaunchableApps() {
        List<Entry> apps = new ArrayList<>();
        try {
            PackageManager pm = getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolved = pm.queryIntentActivities(main, 0);

            String myPackage = getPackageName();
            for (ResolveInfo info : resolved) {
                if (info.activityInfo == null || myPackage.equals(info.activityInfo.packageName)) {
                    continue;
                }
                // 系统预装应用（含已更新的）只显示：图库、相机、设置
                boolean systemApp = (info.activityInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
                if (systemApp && !isCoreSystemApp(info, pm)) {
                    continue;
                }
                CharSequence label = info.loadLabel(pm);
                String name = label != null ? label.toString() : info.activityInfo.packageName;

                Intent launch = new Intent(Intent.ACTION_MAIN);
                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setClassName(info.activityInfo.packageName, info.activityInfo.name);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                apps.add(new Entry(name, info.loadIcon(pm), launch,
                                   info.activityInfo.packageName));
            }
        } catch (Exception e) {
            Toast.makeText(this, "加载应用列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return apps;
    }

    // 判断是否为需要保留显示的图库 / 相机 / 设置
    private boolean isCoreSystemApp(ResolveInfo info, PackageManager pm) {
        String pkg = info.activityInfo.packageName.toLowerCase();

        // 设置
        if (pkg.equals("com.android.settings")) {
            return true;
        }

        // 相机：启动 Activity 声明了 STILL_IMAGE_CAMERA 类别
        if (info.filter != null) {
            java.util.Iterator<String> cats = info.filter.categoriesIterator();
            while (cats.hasNext()) {
                String cat = cats.next();
                if ("android.intent.category.STILL_IMAGE_CAMERA".equals(cat)) {
                    return true;
                }
            }
        }

        // 图库：包名或应用名称包含关键词
        if (pkg.contains("gallery") || pkg.contains("photos")) {
            return true;
        }
        CharSequence label = info.loadLabel(pm);
        if (label != null) {
            String l = label.toString().toLowerCase();
            if (l.contains("gallery") || l.contains("photos")
                || l.contains("图库") || l.contains("相册") || l.contains("相机") || l.contains("设置")) {
                return true;
            }
        }
        return false;
    }

    private void launchEntry(Entry entry) {
        try {
            startActivity(entry.intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法启动: " + entry.label, Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppDetails(Entry entry) {
        if (entry.packageName == null) {
            // 内置功能没有应用详情页
            return;
        }
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + entry.packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开应用详情: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private class EntryAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Entry getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.item_application_grid, parent, false);
            }
            Entry entry = entries.get(position);
            ((ImageView) view.findViewById(R.id.app_icon)).setImageDrawable(entry.icon);
            ((TextView) view.findViewById(R.id.app_label)).setText(entry.label);
            return view;
        }
    }
}
