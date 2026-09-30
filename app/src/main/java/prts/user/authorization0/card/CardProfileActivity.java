package prts.user.authorization0.card;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import prts.user.authorization0.R;

// CardProfileActivity.java
// 填写/修改自己的游戏名片资料
public class CardProfileActivity extends Activity {

    private EditText gameEdit;
    private EditText uidEdit;
    private Button regionButton;
    private String region = Region.CN.code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_profile);

        gameEdit = findViewById(R.id.edit_game);
        uidEdit = findViewById(R.id.edit_uid);
        regionButton = findViewById(R.id.button_region);

        CardProfile existing = CardStore.readProfile(this);
        if (existing != null) {
            if (existing.game != null) {
                gameEdit.setText(existing.game);
            }
            if (existing.uid != null) {
                uidEdit.setText(existing.uid);
            }
            if (Region.isValid(existing.region)) {
                region = existing.region;
            }
        }
        updateRegionButton();

        regionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegionPicker();
            }
        });
        findViewById(R.id.button_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void updateRegionButton() {
        regionButton.setText("游戏区域：" + region);
    }

    private void showRegionPicker() {
        final String[] codes = Region.codes();
        new AlertDialog.Builder(this)
                .setTitle("选择游戏区域")
                .setItems(codes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        region = codes[which];
                        updateRegionButton();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void save() {
        String game = gameEdit.getText().toString().trim();
        String uid = uidEdit.getText().toString().trim();

        if (game.isEmpty()) {
            Toast.makeText(this, "请填写游戏名", Toast.LENGTH_SHORT).show();
            return;
        }
        if (game.length() > 32) {
            Toast.makeText(this, "游戏名不能超过 32 个字符", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!uid.matches("[A-Za-z0-9_\\-]{1,32}")) {
            Toast.makeText(this, "游戏内 UID 只能包含字母、数字、_ 和 -（1~32 位）", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Region.isValid(region)) {
            Toast.makeText(this, "请选择游戏区域", Toast.LENGTH_SHORT).show();
            return;
        }

        String node = CardPrefs.getOrCreateNodeCode(this);
        CardProfile p = new CardProfile();
        p.game = game;
        p.uid = uid;
        p.region = region;
        p.node = node;
        p.name = "RI_PRTS_" + node;
        p.updatedAt = CardStore.nowEpochSec();

        if (CardStore.writeProfile(this, p)) {
            Toast.makeText(this, "名片已保存", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }
}
