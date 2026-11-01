package com.github.tvbox.osc.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.Observer;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.activity.SearchActivity;
import com.github.tvbox.osc.ui.adapter.GridAdapter;
import com.github.tvbox.osc.ui.tv.HomeChannels;
import com.github.tvbox.osc.ui.tv.TvFocusAnimator;
import com.github.tvbox.osc.ui.tv.widget.LoadMoreView;
import com.github.tvbox.osc.util.ImgUtil;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import me.jessyan.autosize.utils.AutoSizeUtils;

/** Movie/series channel with source-backed hot, filter and ranking sections. */
public final class ChannelFragment extends BaseLazyFragment {
    private HomeChannels.Group group;
    private MovieSort.SortData category;
    private int mode;
    private int nextPage = 1;
    private int requestVersion;
    private boolean loading;
    private boolean ended;
    private boolean pendingMenuFocus;
    private LinearLayout content, menu, filterRows, hero, pageHeader;
    private TextView status, filterSummary;
    private TvRecyclerView grid;
    private GridAdapter adapter;
    private final List<Movie.Video> videos = new ArrayList<>();
    private final Stack<MovieSort.SortData> folders = new Stack<>();
    private final List<Runnable> cancelObservers = new ArrayList<>();

    public static ChannelFragment newInstance(HomeChannels.Group group) {
        ChannelFragment fragment = new ChannelFragment();
        Bundle args = new Bundle();
        args.putSerializable("group", group);
        fragment.setArguments(args);
        return fragment;
    }

    @Override protected int getLayoutResID() { return R.layout.fragment_channel; }
    private int mm(float value) { return AutoSizeUtils.mm2px(mContext, value); }
    private LinearLayout column() {
        LinearLayout view = new LinearLayout(mContext);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setClipChildren(false);
        view.setClipToPadding(false);
        return view;
    }
    private LinearLayout row() {
        LinearLayout view = column();
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }
    private TextView label(String text, int size) {
        TextView view = new TextView(mContext);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, mm(size));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setIncludeFontPadding(false);
        return view;
    }
    private void action(View view, Runnable callback) {
        view.setFocusable(true);
        view.setClickable(true);
        view.setBackgroundResource(R.drawable.tv_home_control_focus);
        view.setOnFocusChangeListener((v, focused) -> TvFocusAnimator.animate(v, focused));
        view.setOnClickListener(v -> callback.run());
    }
    private TextView button(String text, Runnable callback) {
        TextView view = label(text, 23);
        view.setGravity(Gravity.CENTER);
        view.setPadding(mm(18), 0, mm(18), 0);
        action(view, callback);
        return view;
    }

    @Override protected void init() {
        group = (HomeChannels.Group) getArguments().getSerializable("group");
        FrameLayout host = findViewById(R.id.channelHost);
        content = column();
        content.setPadding(mm(44), mm(8), mm(44), mm(12));
        content.setClipChildren(true);
        host.addView(content, new FrameLayout.LayoutParams(-1, -1));
        menu = row();
        menu.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) {
                view.post(() -> applyPendingMenuFocus());
            }
            @Override public void onViewDetachedFromWindow(View view) { }
        });
        content.addView(menu, new LinearLayout.LayoutParams(-1, mm(56)));
        filterRows = column();
        filterRows.setClipChildren(true);
        filterRows.setClipToPadding(true);
        filterRows.setBackgroundResource(R.drawable.channel_filter_panel);
        filterRows.setPadding(mm(12), mm(8), mm(12), mm(8));
        LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-1, -2);
        filterParams.setMargins(0, mm(6), 0, mm(12));
        content.addView(filterRows, filterParams);
        hero = row();
        content.addView(hero, new LinearLayout.LayoutParams(-1, mm(175)));
        status = label("", 23);
        content.addView(status, new LinearLayout.LayoutParams(-1, mm(40)));
        grid = new TvRecyclerView(mContext);
        grid.setPreserveFocusAfterLayout(false);
        grid.setClipChildren(true);
        grid.setClipToPadding(true);
        grid.setPadding(mm(5), mm(8), mm(5), mm(8));
        grid.setSpacingWithMargins(mm(12), mm(14));
        grid.setLayoutManager(new V7GridLayoutManager(mContext, 5));
        adapter = new GridAdapter(false, new ImgUtil.Style(0.75f, "rect"));
        grid.setAdapter(adapter);
        content.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));
        filterSummary = label("", 18);
        filterSummary.setGravity(Gravity.CENTER);
        filterSummary.setPadding(mm(20), 0, mm(20), 0);
        filterSummary.setBackgroundResource(R.drawable.channel_filter_summary);
        filterSummary.setVisibility(View.GONE);
        FrameLayout.LayoutParams summaryParams =
                new FrameLayout.LayoutParams(-2, mm(32), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        summaryParams.topMargin = mm(8);
        summaryParams.leftMargin = summaryParams.rightMargin = mm(64);
        host.addView(filterSummary, summaryParams);
        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateSummaryVisibility();
            }
        });
        grid.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) ->
                updateSummaryVisibility());
        adapter.setLoadMoreView(new LoadMoreView());
        adapter.setOnLoadMoreListener(() -> {
            if (!loading && !ended) load(false);
        }, grid);
        adapter.setOnItemClickListener((a, view, position) -> open(videos.get(position)));
        grid.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override public void onItemPreSelected(TvRecyclerView parent, View view, int position) {
                TvFocusAnimator.animate(view, false);
            }
            @Override public void onItemSelected(TvRecyclerView parent, View view, int position) {
                TvFocusAnimator.animate(view, true);
            }
            @Override public void onItemClick(TvRecyclerView parent, View view, int position) {}
        });
        select(0);
        if (pendingMenuFocus) menu.post(this::focusMenu);
    }

    private void select(int selected) {
        mode = selected;
        folders.clear();
        category = mode == 0 ? group.hot : mode == 1 ? group.filter : group.ranking;
        renderMenu();
        renderFilters();
        arrangePage();
        load(true);
    }

    private void detach(View view) {
        if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
    }

    private void arrangePage() {
        detach(menu);
        detach(hero);
        detach(status);
        detach(filterRows);
        adapter.removeAllHeaderView();
        hero.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
        content.setBackgroundColor(Color.TRANSPARENT);
        pageHeader = column();
        if (mode == 0) {
            pageHeader.addView(hero, new LinearLayout.LayoutParams(-1, mm(175)));
            pageHeader.addView(menu, new LinearLayout.LayoutParams(-1, mm(56)));
        } else {
            pageHeader.addView(menu, new LinearLayout.LayoutParams(-1, mm(52)));
        }
        LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-1, -2);
        filterParams.setMargins(0, mm(6), 0, mm(12));
        pageHeader.addView(filterRows, filterParams);
        pageHeader.addView(status, new LinearLayout.LayoutParams(-1, mm(34)));
        adapter.addHeaderView(pageHeader);
        updateFilterSummary();
        filterSummary.setVisibility(View.GONE);
    }

    private void updateFilterSummary() {
        StringBuilder summary = new StringBuilder(group.title);
        if (category != null) for (MovieSort.SortFilter filter : category.filters) {
            if (filter.values == null || filter.values.isEmpty()) continue;
            String active = category.filterSelect.get(filter.key);
            if (active == null) active = filter.values.values().iterator().next();
            for (Map.Entry<String, String> entry : filter.values.entrySet()) {
                if (entry.getValue().equals(active)) {
                    summary.append("  ·  ").append(filter.name).append("：").append(entry.getKey());
                    break;
                }
            }
        }
        filterSummary.setText(summary.toString());
    }

    private void updateSummaryVisibility() {
        if (filterSummary == null || pageHeader == null) return;
        // The summary appears only after the full options have left the viewport.
        View header = grid.getLayoutManager().findViewByPosition(0);
        boolean scrolled = header == null || header.getBottom() <= grid.getPaddingTop();
        filterSummary.setVisibility(mode != 0 && folders.empty()
                && filterRows.getVisibility() == View.VISIBLE && scrolled ? View.VISIBLE : View.GONE);
    }

    private void renderMenu() {
        menu.removeAllViews();
        TextView title = label(group.title, 30);
        menu.addView(title, new LinearLayout.LayoutParams(mm(136), -1));
        String[] names = {"全部筛选", "榜单"};
        for (int i = 0; i < names.length; i++) {
            final int section = i + 1;
            TextView button = button(names[i], () -> {
                select(section);
                menu.getChildAt(section).requestFocus();
            });
            button.setId(View.generateViewId());
            button.setSelected(section == mode);
            button.setBackgroundResource(R.drawable.channel_option);
            button.setTextColor(ContextCompat.getColorStateList(mContext, R.color.channel_option_text));
            button.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode != KeyEvent.KEYCODE_DPAD_UP) return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                    ((HomeActivity) mActivity).focusChannelNavigation();
                return true;
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, mm(44));
            params.setMargins(mm(6), mm(6), mm(6), mm(6));
            menu.addView(button, params);
        }
    }

    private void renderFilters() {
        filterRows.removeAllViews();
        boolean visible = mode != 0 && category != null && !category.filters.isEmpty();
        filterRows.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) return;
        List<MovieSort.SortFilter> ordered = new ArrayList<>(category.filters);
        ordered.sort((a, b) -> Integer.compare("排序".equals(a.name) ? 0 : 1,
                "排序".equals(b.name) ? 0 : 1));
        for (MovieSort.SortFilter filter : ordered) {
            if (filter.values == null || filter.values.isEmpty()) continue;
            LinearLayout line = row();
            line.setClipChildren(true);
            line.setClipToPadding(true);
            TextView heading = label(filter.name, 18);
            heading.setGravity(Gravity.CENTER);
            heading.setTextColor(ContextCompat.getColor(mContext, R.color.tv_text_secondary));
            heading.setBackgroundResource(R.drawable.channel_filter_label);
            heading.setTypeface(heading.getTypeface(), android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(mm(82), mm(28));
            headingParams.setMargins(0, mm(4), mm(12), mm(4));
            line.addView(heading, headingParams);
            HorizontalScrollView horizontal = new HorizontalScrollView(mContext);
            horizontal.setHorizontalScrollBarEnabled(false);
            // Options must stay inside their own viewport, away from the fixed row label.
            horizontal.setClipChildren(true);
            horizontal.setClipToPadding(true);
            LinearLayout options = row();
            options.setClipChildren(true);
            options.setClipToPadding(true);
            horizontal.addView(options);
            line.addView(horizontal, new LinearLayout.LayoutParams(0, mm(36), 1f));
            filterRows.addView(line);
            String active = category.filterSelect.get(filter.key);
            if (active == null) active = filter.values.values().iterator().next();
            for (Map.Entry<String, String> entry : filter.values.entrySet()) {
                final String value = entry.getValue();
                TextView option = button(entry.getKey(), () -> {
                    category.filterSelect.put(filter.key, value);
                    for (int index = 0; index < options.getChildCount(); index++) {
                        TextView child = (TextView) options.getChildAt(index);
                        boolean selected = value.equals(child.getTag());
                        child.setSelected(selected);
                    }
                    updateFilterSummary();
                    load(true);
                });
                option.setTag(value);
                option.setTextSize(TypedValue.COMPLEX_UNIT_PX, mm(18));
                option.setPadding(mm(12), 0, mm(12), 0);
                option.setBackgroundResource(R.drawable.channel_option);
                option.setTextColor(ContextCompat.getColorStateList(mContext, R.color.channel_option_text));
                boolean selected = value.equals(active);
                option.setSelected(selected);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, mm(28));
                params.setMargins(mm(3), mm(4), mm(3), mm(4));
                options.addView(option, params);
            }
        }
    }

    private void load(boolean reset) {
        if (reset) {
            requestVersion++;
            detachRequests();
            nextPage = 1;
            ended = false;
            videos.clear();
            adapter.setEnableLoadMore(false);
            adapter.setNewData(new ArrayList<>());
            hero.removeAllViews();
            grid.scrollToPosition(0);
            filterSummary.setVisibility(View.GONE);
        }
        if (category == null) {
            status.setVisibility(View.VISIBLE);
            status.setText(mode == 1 ? "当前片源未提供筛选分类" : "当前片源未提供榜单");
            loading = false;
            ended = true;
            adapter.setEnableLoadMore(false);
            return;
        }
        loading = true;
        status.setVisibility(View.VISIBLE);
        status.setText(nextPage == 1 ? "正在加载…" : "正在加载更多…");
        final int version = requestVersion;
        final int page = nextPage;
        final String source = ApiConfig.get().getHomeSourceBean().getKey();
        // Each request has its own result stream. Late results cannot replace a newer selection.
        SourceViewModel request = new SourceViewModel();
        MovieSort.SortData snapshot = HomeChannels.copy(category);
        Observer<AbsXml> observer = result -> {
            if (version != requestVersion || !isAdded()
                    || !source.equals(ApiConfig.get().getHomeSourceBean().getKey())) return;
            detachRequests();
            loading = false;
            View focusedHeader = pageHeader.findFocus();
            if (result == null || result.movie == null || result.movie.videoList == null
                    || result.movie.videoList.isEmpty()) {
                ended = true;
                adapter.loadMoreEnd();
                adapter.setEnableLoadMore(false);
                status.setText(videos.isEmpty() ? "暂无结果，换个筛选条件试试" : "已显示全部影片");
                status.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
                return;
            }
            videos.addAll(result.movie.videoList);
            if (page == 1) adapter.setNewData(new ArrayList<>(videos));
            else adapter.addData(result.movie.videoList);
            nextPage = page + 1;
            ended = result.movie.pagecount > 0 && nextPage > result.movie.pagecount;
            if (ended) adapter.loadMoreEnd(); else adapter.loadMoreComplete();
            adapter.setEnableLoadMore(!ended);
            status.setText(mode == 0 ? "重磅热播" : "");
            status.setVisibility(mode == 0 ? View.VISIBLE : View.GONE);
            if (mode == 0 && page == 1) renderHero();
            if (focusedHeader != null) grid.post(focusedHeader::requestFocus);
            android.util.Log.i("YueKanChannel", group.title + " mode=" + mode
                    + " category=" + snapshot.id + " page=" + page
                    + " filters=" + snapshot.filterSelect + " count=" + result.movie.videoList.size());
        };
        request.listResult.observe(getViewLifecycleOwner(), observer);
        cancelObservers.add(() -> request.listResult.removeObserver(observer));
        request.getList(snapshot, page);
    }

    private void renderHero() {
        hero.removeAllViews();
        for (int i = 0; i < Math.min(3, videos.size()); i++) {
            Movie.Video video = videos.get(i);
            FrameLayout card = new FrameLayout(mContext);
            action(card, () -> open(video));
            card.setClipToOutline(true);
            ImageView image = new ImageView(mContext);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ImgUtil.load(video.pic, image, mm(14), mm(410), mm(175), video.name);
            card.addView(image, new FrameLayout.LayoutParams(-1, -1));
            TextView caption = label(video.name, 27);
            caption.setPadding(mm(16), 0, mm(16), 0);
            caption.setBackgroundColor(0xB0092546);
            card.addView(caption, new FrameLayout.LayoutParams(-1, mm(46), Gravity.BOTTOM));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
            params.setMargins(mm(6), mm(6), mm(6), mm(6));
            hero.addView(card, params);
        }
    }

    private void open(Movie.Video video) {
        if (video.action != null) {
            SourceViewModel request = new SourceViewModel();
            request.actionResult.observe(getViewLifecycleOwner(), result -> load(true));
            request.action(video.sourceKey, video.action);
            return;
        }
        if ("folder".equals(video.tag) || "cover".equals(video.tag)) {
            folders.push(HomeChannels.copy(category));
            category = new MovieSort.SortData(video.id, video.name);
            filterRows.setVisibility(View.GONE);
            filterSummary.setVisibility(View.GONE);
            hero.setVisibility(View.GONE);
            load(true);
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("id", video.id);
        bundle.putString("sourceKey", video.sourceKey);
        bundle.putString("title", video.name);
        bundle.putString("picture", video.pic);
        jumpActivity(video.id != null && video.id.startsWith("msearch:")
                ? SearchActivity.class : DetailActivity.class, bundle);
    }

    public boolean back() {
        if (!folders.empty()) {
            category = folders.pop();
            renderFilters();
            updateFilterSummary();
            hero.setVisibility(mode == 0 && folders.empty() ? View.VISIBLE : View.GONE);
            load(true);
            return true;
        }
        if (mode != 0) {
            select(0);
            menu.getChildAt(1).requestFocus();
            return true;
        }
        return false;
    }

    public void focusMenu() {
        pendingMenuFocus = true;
        if (menu == null || menu.getChildCount() < 2) return;
        // The shortcuts and filter options share the scrollable header.
        grid.scrollToPosition(0);
        grid.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        // Wait for a detached header to be attached after scrolling back to the top.
        grid.post(this::applyPendingMenuFocus);
    }

    private void applyPendingMenuFocus() {
        if (!isAdded() || !pendingMenuFocus || menu == null || menu.getChildCount() < 2) return;
        if (menu.getChildAt(Math.max(1, mode)).requestFocus()) pendingMenuFocus = false;
    }

    private void detachRequests() {
        for (Runnable cancel : cancelObservers) cancel.run();
        cancelObservers.clear();
    }

    @Override public void onDestroyView() {
        requestVersion++;
        detachRequests();
        super.onDestroyView();
    }
}
