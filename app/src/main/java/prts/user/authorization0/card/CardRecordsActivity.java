package prts.user.authorization0.card;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import prts.user.authorization0.R;

// CardRecordsActivity.java
// 查看收到的名片记录
public class CardRecordsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_records);

        ListView list = findViewById(R.id.list_cards);
        TextView empty = findViewById(R.id.text_empty);

        List<ReceivedCard> cards = CardStore.readReceived(this);
        if (cards.isEmpty()) {
            list.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
            return;
        }
        empty.setVisibility(View.GONE);

        List<String> rows = new ArrayList<String>();
        for (int i = cards.size() - 1; i >= 0; i--) {
            ReceivedCard c = cards.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append(c.receivedAtStr == null ? "" : c.receivedAtStr).append('\n');
            sb.append("对方：").append(c.peerName == null ? "unknown" : c.peerName).append('\n');
            sb.append("游戏：").append(c.game == null ? "-" : c.game)
                    .append("  区域：").append(c.region == null ? "-" : c.region).append('\n');
            sb.append("UID：").append(c.uid == null ? "-" : c.uid);
            rows.add(sb.toString());
        }

        list.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rows));
    }
}
