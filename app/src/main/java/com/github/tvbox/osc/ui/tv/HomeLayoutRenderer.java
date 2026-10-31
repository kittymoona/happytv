package com.github.tvbox.osc.ui.tv;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.util.ImgUtil;
import java.util.List;
import me.jessyan.autosize.utils.AutoSizeUtils;

/** Six compositions with shared data, focus semantics and navigation callbacks. */
public final class HomeLayoutRenderer {
    public interface Actions {
        void category(String category);
        boolean available(String category);
        void open(VodInfo item, boolean resume);
        void history();
    }
    private final Context context;
    private final Actions actions;
    private final List<VodInfo> history;
    private final List<VodInfo> selections;
    private final int layout;
    private float viewportScale = 1f;
    private static final int WHITE = Color.WHITE;
    private static final int SECONDARY = Color.rgb(226,235,255);

    public HomeLayoutRenderer(Context context, Actions actions, int layout,
                              List<VodInfo> history, List<VodInfo> selections) {
        this.context = context;
        this.actions = actions;
        this.layout = layout;
        this.history = history;
        this.selections = selections;
    }

    public void render(FrameLayout host) {
        // The parent owns the usable area after navigation and bottom controls.
        // Do not size cards against the physical display or before that area is measured.
        if (host.getWidth() == 0 || host.getHeight() == 0) {
            host.post(() -> render(host));
            return;
        }
        viewportScale = Math.min(1f, Math.min(
                host.getWidth() / (float) AutoSizeUtils.mm2px(context, 1200),
                host.getHeight() / (float) AutoSizeUtils.mm2px(context, 450)));
        View focused = host.findFocus();
        Object previousTag = focused == null ? null : focused.getTag();
        boolean hadFocus = focused != null;
        host.removeAllViews();
        LinearLayout content = column();
        host.addView(content, new FrameLayout.LayoutParams(-1,-1));
        switch (layout) {
            case 0:
                add(content, categories(true, false), 78);
                if (!history.isEmpty()) section(content, "继续观看", history, 4, true, 1f);
                section(content, "当前源片单", selections, 5, false, 1f);
                break;
            case 1:
                heroWithCategories(content, false);
                section(content, "精选片单", selections, 5, false, 1f);
                break;
            case 2:
                heroWithCategories(content, true);
                if (!history.isEmpty()) section(content, "最近观看", history, 5, true, 0.65f);
                break;
            case 3:
                LinearLayout primary = row();
                weighted(primary, hero(), 1.8f);
                weighted(primary, category("live", true), 1f);
                weighted(content, primary, 1.4f);
                add(content, label("找节目", 27, WHITE), 46);
                weighted(content, categories(false, true), 1f);
                break;
            case 4:
                add(content, label("想看什么？", 32, WHITE), 52);
                LinearLayout first = row();
                weighted(first, resumeButton(), 1f);
                weighted(first, category("live", true), 1f);
                weighted(content, first, 1f);
                weighted(content, categories(false, true), 1f);
                add(content, label("找不到想看的节目？按下方「搜索」", 23, SECONDARY), 50);
                break;
            default:
                add(content, categories(true, false), 78);
                if (!history.isEmpty()) section(content, "继续观看", history, 3, true, 1f);
                else weighted(content, emptyHistory(), 1f);
                break;
        }
        if (hadFocus) {
            View restore = previousTag == null ? null : content.findViewWithTag(previousTag);
            if (restore != null) restore.requestFocus();
            else content.post(() -> {
                List<View> focusables = content.getFocusables(View.FOCUS_FORWARD);
                if (!focusables.isEmpty()) focusables.get(0).requestFocus();
            });
        }
        if (BuildConfig.DEBUG) {
            content.post(() -> android.util.Log.i("HomeLayoutFit",
                    "layout=" + layout + " viewport=" + host.getWidth() + "x" + host.getHeight()
                            + " scale=" + viewportScale + " clipped=" + countClipped(content)));
        }
    }

    private int countClipped(View view) {
        int count = 0;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getLayout() != null && text.getLayout().getLineCount() > 0
                    && text.getLayout().getLineBottom(0)
                    > text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom()) {
                android.util.Log.w("HomeLayoutFit", "clipped text=" + text.getText());
                count++;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child.getVisibility() != View.GONE) {
                    if (child.getBottom() > group.getHeight() - group.getPaddingBottom() + 1) {
                        android.util.Log.w("HomeLayoutFit", "child exceeds parent tag=" + child.getTag());
                        count++;
                    }
                    count += countClipped(child);
                }
            }
        }
        return count;
    }

    private int mm(float value) { return Math.max(1, Math.round(
            AutoSizeUtils.mm2px(context, value) * viewportScale)); }
    private LinearLayout column() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setClipChildren(false);
        view.setClipToPadding(false);
        return view;
    }
    private LinearLayout row() {
        LinearLayout view = column();
        view.setOrientation(LinearLayout.HORIZONTAL);
        return view;
    }
    private void add(LinearLayout parent, View child, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,mm(height));
        params.setMargins(mm(6),mm(5),mm(6),mm(5));
        parent.addView(child,params);
    }
    private void weighted(LinearLayout parent, View child, float weight) {
        boolean vertical = parent.getOrientation() == LinearLayout.VERTICAL;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                vertical ? -1 : 0, vertical ? 0 : -1, weight);
        params.setMargins(mm(6),mm(5),mm(6),mm(5));
        parent.addView(child,params);
    }
    private TextView label(String value, int size, int color) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, mm(size));
        text.setFontFeatureSettings("kern");
        text.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setMaxLines(1);
        text.setIncludeFontPadding(false);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setFocusable(false);
        return text;
    }
    private void actionable(View view, String tag, Runnable click) {
        view.setTag(tag);
        view.setFocusable(true);
        view.setClickable(true);
        if (view instanceof ViewGroup)
            ((ViewGroup) view).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        view.setBackgroundResource(R.drawable.tv_home_control_focus);
        view.setOnFocusChangeListener((v,focused) -> TvFocusAnimator.animate(v, focused));
        view.setOnClickListener(v -> click.run());
    }
    private LinearLayout categories(boolean live, boolean large) {
        LinearLayout result = row();
        for (String name : new String[]{"movie","series","variety"})
            weighted(result,category(name,large),1f);
        if (live) weighted(result,category("live",large),1f);
        return result;
    }
    private View category(String name, boolean large) {
        String title = "movie".equals(name) ? "电影" : "series".equals(name) ? "剧集"
                : "variety".equals(name) ? "综艺" : "kids".equals(name) ? "少儿" : "直播";
        LinearLayout card = column();
        card.setPadding(mm(16),mm(6),mm(16),mm(6));
        actionable(card,"category:"+name,() -> actions.category(name));
        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = label(("live".equals(name) && large ? "看" : "")+title,
                large ? 30 : 28, WHITE);
        top.addView(heading,new LinearLayout.LayoutParams(0,-1,1f));
        ImageView icon = new ImageView(context);
        icon.setImageResource("movie".equals(name) ? R.drawable.tv_icon_movie
                : "series".equals(name) ? R.drawable.tv_icon_series
                : "variety".equals(name) ? R.drawable.tv_icon_variety
                : "kids".equals(name) ? R.drawable.tv_icon_kids : R.drawable.icon_live);
        icon.setColorFilter(SECONDARY);
        top.addView(icon,new LinearLayout.LayoutParams(mm(large ? 36 : 40),mm(large ? 36 : 40)));
        card.addView(top,new LinearLayout.LayoutParams(-1,large ? mm(36) : -1));
        if (large) {
            TextView subtitle = label("live".equals(name) ? "进入直播"
                    : actions.available(name) ? "进入分类" : "需切换源",22,SECONDARY);
            subtitle.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(subtitle,new LinearLayout.LayoutParams(-1,0,1f));
        }
        card.setContentDescription(title + (actions.available(name) ? "" : "，当前源没有此分类"));
        if (!actions.available(name)) card.setAlpha(0.55f);
        return card;
    }

    private View resumeButton() {
        if (history.isEmpty()) return emptyHistory();
        VodInfo item = history.get(0);
        LinearLayout card = column();
        card.setPadding(mm(22),mm(18),mm(22),mm(18));
        actionable(card,"resume",() -> actions.open(item,true));
        card.addView(label("继续看 · "+item.name,34,WHITE),new LinearLayout.LayoutParams(-1,0,1f));
        card.addView(label(historyNote(item)+"  →",24,SECONDARY),
                new LinearLayout.LayoutParams(-1,mm(46)));
        return card;
    }
    private View emptyHistory() {
        LinearLayout view = column();
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(mm(24),mm(18),mm(24),mm(18));
        actionable(view,"discover",() -> actions.category("series"));
        view.addView(label("找一部喜欢的影片",31,WHITE));
        view.addView(label("打开剧集分类，或使用下方搜索  →",22,SECONDARY));
        return view;
    }
    private View hero() {
        if (history.isEmpty()) {
            if (layout == 1 && !selections.isEmpty()) return hero(selections.get(0),false);
            return emptyHistory();
        }
        return hero(history.get(0),true);
    }
    private View hero(VodInfo item, boolean resume) {
        FrameLayout frame = new FrameLayout(context);
        actionable(frame,"resume",() -> actions.open(item,resume));
        frame.setPadding(mm(3),mm(3),mm(3),mm(3));
        ImageView image = image(item);
        frame.addView(image,new FrameLayout.LayoutParams(-1,-1));
        View shade = new View(context);
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT,0x70091D35,0xED091D35});
        shade.setBackground(gradient);
        frame.addView(shade,new FrameLayout.LayoutParams(-1,mm(146),Gravity.BOTTOM));
        LinearLayout caption = column();
        caption.setPadding(mm(24),mm(6),mm(24),mm(12));
        caption.setGravity(Gravity.BOTTOM);
        caption.addView(label(resume ? "继续观看" : "精选影片",20,SECONDARY));
        caption.addView(label(item.name,34,WHITE),new LinearLayout.LayoutParams(-1,mm(44)));
        String note = resume ? historyNote(item) : safe(item.note);
        String action = resume ? "继续播放 →" : "查看影片 →";
        caption.addView(label(TextUtils.isEmpty(note) ? action : note+"  ·  "+action,20,SECONDARY),
                new LinearLayout.LayoutParams(-1,mm(28)));
        frame.addView(caption,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        frame.setClipToOutline(true);
        return frame;
    }
    private void heroWithCategories(LinearLayout content, boolean personal) {
        LinearLayout top = row();
        weighted(top,hero(),personal ? 1.15f : 1.65f);
        LinearLayout choices = column();
        LinearLayout first = row();
        weighted(first,category("movie",true),1f);
        weighted(first,category("series",true),1f);
        LinearLayout second = row();
        weighted(second,category("variety",true),1f);
        weighted(second,category("live",true),1f);
        weighted(choices,first,1f);
        weighted(choices,second,1f);
        weighted(top,choices,1f);
        weighted(content,top,1f);
    }

    private void section(LinearLayout content, String title, List<VodInfo> items,
                         int limit, boolean resume, float weight) {
        // Empty histories collapse; source outages offer an actionable category.
        LinearLayout section = column();
        LinearLayout header = row();
        header.addView(label(title,layout >= 3 ? 30 : 26,WHITE),
                new LinearLayout.LayoutParams(0,-1,1f));
        TextView all = label(resume ? "全部历史  →" : "进入片单  →",21,WHITE);
        all.setPadding(mm(10),0,mm(10),0);
        actionable(all,"all:"+title,() -> {
            if (resume) actions.history(); else actions.category("series");
        });
        header.addView(all,new LinearLayout.LayoutParams(-2,-1));
        add(section,header,42);
        LinearLayout cards = row();
        if (items.isEmpty()) {
            LinearLayout unavailable = column();
            unavailable.setPadding(mm(20),mm(12),mm(20),mm(12));
            actionable(unavailable,"source-empty",() -> actions.category("series"));
            unavailable.addView(label("打开分类查看影片  →",25,WHITE));
            weighted(cards,unavailable,1f);
        } else {
            int count = Math.min(limit,items.size());
            for (int i = 0; i < count; i++) weighted(cards,tile(items.get(i),resume),1f);
            // With only one record retain a normal card width instead of a stretched poster.
            for (int i = count; i < limit; i++) weighted(cards,new View(context),1f);
        }
        weighted(section,cards,1f);
        weighted(content,section,weight);
    }
    private View tile(VodInfo item, boolean resume) {
        FrameLayout card = new FrameLayout(context);
        actionable(card,(resume ? "history:" : "selection:")+item.sourceKey+":"+item.id,
                () -> actions.open(item,resume));
        card.setPadding(mm(3),mm(3),mm(3),mm(3));
        // Captions overlay the artwork instead of consuming all of a short TV card.
        card.addView(image(item),new FrameLayout.LayoutParams(-1,-1));
        View shade = new View(context);
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT,0x60091D35,0xDD091D35});
        shade.setBackground(gradient);
        card.addView(shade,new FrameLayout.LayoutParams(-1,mm(layout == 5 ? 98 : 80),Gravity.BOTTOM));
        card.setClipToOutline(true);
        LinearLayout caption = column();
        caption.setPadding(mm(10),mm(6),mm(10),mm(6));
        TextView title = label(item.name,layout == 5 ? 28 : 22,WHITE);
        caption.addView(title,new LinearLayout.LayoutParams(-1,mm(layout == 5 ? 38 : 30)));
        String note = resume ? historyNote(item) : safe(item.note);
        if (!TextUtils.isEmpty(note)) {
            TextView subtitle = label(note,
                layout == 5 ? 23 : 18,SECONDARY);
            caption.addView(subtitle,new LinearLayout.LayoutParams(-1,mm(layout == 5 ? 30 : 24)));
        }
        card.addView(caption,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        return card;
    }
    private ImageView image(VodInfo item) {
        ImageView view = new ImageView(context);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setCropToPadding(true);
        GradientDrawable outline = new GradientDrawable();
        outline.setColor(context.getResources().getColor(R.color.tv_surface));
        outline.setCornerRadius(mm(21));
        view.setBackground(outline);
        view.setClipToOutline(true);
        view.setContentDescription(null);
        String artwork = item.pic;
        // A saved history can contain an older thumbnail; prefer fresh artwork for the same title.
        for (VodInfo current : selections) {
            if (TextUtils.equals(current.name,item.name) && !TextUtils.isEmpty(current.pic)) {
                artwork = current.pic;
                break;
            }
        }
        ImgUtil.loadHomeArtwork(artwork,view,item.name);
        return view;
    }
    private String historyNote(VodInfo item) {
        String episode = item.playNote;
        if (TextUtils.isEmpty(episode)) episode = String.valueOf(item.playIndex + 1);
        return episode.matches("\\d+") ? "上次看到第 "+episode+" 集" : "上次看到 "+episode;
    }
    private String safe(String value) { return value == null ? "" : value; }
}
