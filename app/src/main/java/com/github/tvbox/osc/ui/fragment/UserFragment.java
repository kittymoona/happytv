package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.ui.activity.*;
import com.github.tvbox.osc.ui.tv.HomeLayoutRenderer;
import com.github.tvbox.osc.ui.tv.HomeLayouts;
import com.github.tvbox.osc.ui.tv.TvFocusAnimator;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import java.util.ArrayList;
import java.util.List;

public class UserFragment extends BaseLazyFragment implements HomeLayoutRenderer.Actions {
    private FrameLayout content;
    private SourceViewModel selectionsModel;
    private List<VodInfo> history = new ArrayList<>();
    private final List<VodInfo> selections = new ArrayList<>();
    private String requestedSource;
    private int displayedLayout = -1;
    private boolean initialized;

    public static UserFragment newInstance() { return new UserFragment(); }
    @Override protected int getLayoutResID() { return R.layout.fragment_user; }

    @Override protected void init() {
        content = findViewById(R.id.homeLayoutContent);
        content.addOnLayoutChangeListener((view,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom) -> {
            if (initialized && right > left && bottom > top
                    && (right-left != oldRight-oldLeft || bottom-top != oldBottom-oldTop)) {
                render();
            }
        });
        int[] controls = {R.id.tvHistory,R.id.tvSearch,R.id.tvPush,R.id.tvFavorite,R.id.tvSetting};
        Class<?>[] screens = {HistoryActivity.class,SearchActivity.class,PushActivity.class,
                CollectActivity.class,SettingActivity.class};
        for (int i = 0; i < controls.length; i++) {
            final Class<?> screen = screens[i];
            View control = findViewById(controls[i]);
            control.setOnFocusChangeListener((v,f) -> TvFocusAnimator.animate(v,f));
            control.setOnClickListener(v -> {
                FastClickCheckUtil.check(v);
                HawkConfig.hotVodDelete = false;
                startActivity(new android.content.Intent(mContext,screen));
            });
        }
        selectionsModel = new ViewModelProvider(this).get(SourceViewModel.class);
        selectionsModel.listResult.observe(getViewLifecycleOwner(), result -> {
            if (result != null && !TextUtils.isEmpty(result.sourceKey)
                    && !TextUtils.equals(result.sourceKey,requestedSource)) return;
            if (!TextUtils.equals(requestedSource,ApiConfig.get().getHomeSourceBean().getKey())) return;
            selections.clear();
            if (result != null && result.movie != null && result.movie.videoList != null) {
                for (Movie.Video video : result.movie.videoList) {
                    if (video == null || TextUtils.isEmpty(video.id) || video.action != null) continue;
                    VodInfo item = new VodInfo();
                    item.id = video.id;
                    item.name = video.name;
                    item.pic = video.pic;
                    item.note = video.note;
                    item.sourceKey = requestedSource;
                    selections.add(item);
                    if (selections.size() == 5) break;
                }
            }
            if (currentVisibleState && HomeLayouts.selected() <= 1) render();
        });
        initialized = true;
    }
    @Override protected void onFragmentResume() {
        super.onFragmentResume();
        if (!initialized) return;
        history = RoomDataManger.getAllVodRecord(5);
        render();
        loadSelections();
    }
    // BaseLazyFragment dispatches the visible resume once after returning from settings/detail.
    private void render() {
        displayedLayout = HomeLayouts.selected();
        new HomeLayoutRenderer(mContext,this,displayedLayout,history,selections).render(content);
    }
    private void loadSelections() {
        if (displayedLayout > 1) return;
        String source = ApiConfig.get().getHomeSourceBean().getKey();
        if (TextUtils.equals(source,requestedSource)) return;
        MovieSort.SortData selected = home().getHomeCategory("series");
        if (selected == null) selected = home().getHomeCategory("movie");
        if (selected == null) return;
        requestedSource = source;
        selections.clear();
        // Independent snapshot: homepage requests never modify category filters or GridFragment.
        MovieSort.SortData request = new MovieSort.SortData(selected.id,selected.name);
        selectionsModel.getList(request,1);
    }
    private HomeActivity home() { return (HomeActivity) mActivity; }
    @Override public boolean available(String category) {
        return "live".equals(category) || home().hasHomeCategory(category);
    }
    @Override public void category(String category) {
        HawkConfig.hotVodDelete = false;
        if ("live".equals(category)) jumpActivity(LivePlayActivity.class);
        else if (!home().openHomeCategory(category))
            Toast.makeText(mContext,"当前源没有此分类，请在设置中切换首页源",Toast.LENGTH_SHORT).show();
    }
    @Override public void history() {
        HawkConfig.hotVodDelete = false;
        jumpActivity(HistoryActivity.class);
    }
    @Override public void open(VodInfo item, boolean resume) {
        HawkConfig.hotVodDelete = false;
        Bundle bundle = new Bundle();
        bundle.putString("title",item.name);
        if (ApiConfig.get().getSource(item.sourceKey) == null) {
            jumpActivity(SearchActivity.class,bundle);
            return;
        }
        bundle.putString("id",item.id);
        bundle.putString("sourceKey",item.sourceKey);
        bundle.putString("picture",item.pic);
        bundle.putBoolean("resumePlayback",resume);
        jumpActivity(DetailActivity.class,bundle);
    }
}
