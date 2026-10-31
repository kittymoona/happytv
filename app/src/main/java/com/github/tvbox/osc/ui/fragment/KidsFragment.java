package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.ui.activity.SearchActivity;
import com.github.tvbox.osc.ui.tv.TvFocusAnimator;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import me.jessyan.autosize.utils.AutoSizeUtils;

/** Editorial age/interest shortcuts, searched against the configured sources on demand. */
public final class KidsFragment extends BaseLazyFragment {
    public static final String CATEGORY_ID = "yuekan_kids_shortcuts";
    private static final String[] AGES = {"全部年龄", "2–3岁", "4–6岁", "7–9岁", "10–12岁"};
    private static final String[] TYPES = {"全部内容", "启蒙认知", "生活习惯", "科普探索", "冒险成长", "经典故事"};
    // Age masks refer to the four advisory bands above, not official ratings.
    private static final class Program {
        final String title;
        final int ages, type;
        Program(String title, int ages, int type) {
            this.title = title; this.ages = ages; this.type = type;
        }
    }
    private static final Program[] PROGRAMS = {
        new Program("小猪佩奇", 3, 2),
        new Program("布鲁伊", 6, 2),
        new Program("汪汪队立大功", 6, 4),
        new Program("海底小纵队", 6, 3),
        new Program("超级飞侠", 6, 4),
        new Program("熊出没", 12, 4),
        new Program("小小智慧树", 1, 1),
        new Program("宝宝巴士", 3, 1),
        new Program("贝瓦儿歌", 3, 1),
        new Program("巧虎", 3, 2),
        new Program("天线宝宝", 1, 1),
        new Program("花园宝宝", 1, 2),
        new Program("嗨！道奇", 3, 2),
        new Program("丹尼尔虎的邻居们", 3, 2),
        new Program("数字积木", 3, 1),
        new Program("螺丝钉", 6, 3),
        new Program("巴布工程师", 3, 3),
        new Program("恐龙列车", 2, 3),
        new Program("智慧树", 2, 1),
        new Program("大耳朵图图", 6, 2),
        new Program("萌鸡小队", 3, 2),
        new Program("小羊肖恩", 6, 2),
        new Program("百变布鲁可", 2, 3),
        new Program("小马宝莉", 6, 4),
        new Program("喜羊羊与灰太狼", 12, 4),
        new Program("猪猪侠", 12, 4),
        new Program("开心超人联盟", 12, 4),
        new Program("神奇的朋友咖宝蛋神", 6, 4),
        new Program("神奇校车", 12, 3),
        new Program("动物兄弟", 4, 3),
        new Program("哪吒传奇", 12, 5),
        new Program("西游记", 12, 5),
        new Program("围棋少年", 8, 5),
        new Program("大闹天宫", 12, 5),
        new Program("葫芦兄弟", 12, 5),
        new Program("阿凡提的故事", 12, 5),
        new Program("舒克贝塔", 12, 4),
        new Program("赛尔号", 12, 4)
    };
    private int age, type;
    private GridLayout programs;
    private ScrollView scroll;
    private TextView summary;

    public static boolean isKidsCategory(String name) {
        return name != null && (name.contains("少儿") || name.contains("儿童") || name.contains("亲子"));
    }
    public static KidsFragment newInstance() { return new KidsFragment(); }
    @Override protected int getLayoutResID() { return R.layout.fragment_kids; }
    private int mm(float value) { return AutoSizeUtils.mm2px(mContext, value); }

    @Override protected void init() {
        programs = findViewById(R.id.kidsPrograms);
        scroll = findViewById(R.id.kidsProgramsScroll);
        summary = findViewById(R.id.kidsSelectionSummary);
        filters(findViewById(R.id.kidsAgeFilters), "年龄", AGES, true);
        filters(findViewById(R.id.kidsTypeFilters), "兴趣", TYPES, false);
        View more = findViewById(R.id.kidsMore);
        more.setOnFocusChangeListener((v, f) -> TvFocusAnimator.animate(v, f));
        more.setOnClickListener(v -> search(v, null));
        renderPrograms();
    }

    private void filters(LinearLayout host, String heading, String[] values, boolean ageRow) {
        TextView label = text(heading, 20);
        label.setBackgroundResource(R.drawable.channel_filter_label);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(mm(82), mm(30));
        headingParams.setMargins(0, 0, mm(12), 0);
        host.addView(label, headingParams);
        HorizontalScrollView viewport = new HorizontalScrollView(mContext);
        viewport.setHorizontalScrollBarEnabled(false);
        viewport.setClipChildren(true);
        viewport.setClipToPadding(true);
        LinearLayout options = new LinearLayout(mContext);
        options.setGravity(Gravity.CENTER_VERTICAL);
        viewport.addView(options);
        host.addView(viewport, new LinearLayout.LayoutParams(0, -1, 1f));
        for (int i = 0; i < values.length; i++) {
            final int selection = i;
            TextView option = text(values[i], 21);
            option.setPadding(mm(18), 0, mm(18), 0);
            option.setBackgroundResource(R.drawable.channel_option);
            option.setTextColor(ContextCompat.getColorStateList(mContext, R.color.channel_option_text));
            option.setSelected(i == (ageRow ? age : type));
            focus(option);
            option.setOnClickListener(v -> {
                if (ageRow) age = selection; else type = selection;
                for (int j = 0; j < options.getChildCount(); j++)
                    options.getChildAt(j).setSelected(j == selection);
                renderPrograms();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, mm(32));
            params.setMargins(mm(4), mm(4), mm(4), mm(4));
            options.addView(option, params);
        }
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(mContext);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, mm(size));
        view.setTextColor(ContextCompat.getColor(mContext, R.color.tv_text_primary));
        view.setGravity(Gravity.CENTER);
        return view;
    }
    private void focus(View view) {
        view.setFocusable(true);
        view.setClickable(true);
        view.setOnFocusChangeListener((v, f) -> TvFocusAnimator.animate(v, f));
    }
    private void renderPrograms() {
        programs.removeAllViews();
        int count = 0;
        for (Program program : PROGRAMS) {
            if (age != 0 && (program.ages & (1 << (age - 1))) == 0) continue;
            if (type != 0 && type != program.type) continue;
            TextView card = text(program.title + "\n" + TYPES[program.type], 23);
            card.setMaxLines(2);
            card.setPadding(mm(10), mm(8), mm(10), mm(8));
            card.setBackgroundResource(R.drawable.tv_home_control_focus);
            card.setTextColor(ContextCompat.getColorStateList(mContext, R.color.channel_option_text));
            focus(card);
            card.setOnClickListener(v -> search(v, program.title));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(count / 4), GridLayout.spec(count % 4, 1f));
            params.width = 0;
            params.height = mm(84);
            params.setMargins(mm(5), mm(5), mm(5), mm(5));
            programs.addView(card, params);
            count++;
        }
        summary.setText(AGES[age] + " · " + TYPES[type] + " · " + count + "部节目"
                + (count == 0 ? "，试试其他筛选条件" : "，选择后搜索"));
        scroll.scrollTo(0, 0);
    }
    private void search(View view, String title) {
        FastClickCheckUtil.check(view);
        HawkConfig.hotVodDelete = false;
        if (title == null) { jumpActivity(SearchActivity.class); return; }
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        jumpActivity(SearchActivity.class, bundle);
    }
}
