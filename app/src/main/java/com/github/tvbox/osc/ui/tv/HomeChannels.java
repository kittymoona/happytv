package com.github.tvbox.osc.ui.tv;

import com.github.tvbox.osc.bean.MovieSort;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Keeps source category IDs intact while grouping the home navigation. */
public final class HomeChannels {
    public static final String MOVIE = "yuekan_channel_movie";
    public static final String SERIES = "yuekan_channel_series";

    public static final class Group implements Serializable {
        public String id;
        public String title;
        public MovieSort.SortData hot;
        public MovieSort.SortData filter;
        public MovieSort.SortData ranking;
        public Group(String id, String title) { this.id = id; this.title = title; }
    }

    public static String channel(String name) {
        if (name == null) return null;
        if (name.contains("电影")) return MOVIE;
        if (name.contains("电视剧") || name.contains("剧集")
                || (name.contains("电视") && !name.contains("直播"))) return SERIES;
        return null;
    }

    public static MovieSort.SortData copy(MovieSort.SortData category) {
        if (category == null) return null;
        MovieSort.SortData copy = new MovieSort.SortData(category.id, category.name);
        copy.sort = category.sort;
        copy.select = category.select;
        copy.flag = category.flag;
        if (category.filterSelect != null) copy.filterSelect.putAll(category.filterSelect);
        if (category.filters != null) for (MovieSort.SortFilter filter : category.filters) {
            MovieSort.SortFilter item = new MovieSort.SortFilter();
            item.key = filter.key;
            item.name = filter.name;
            if (filter.values != null) item.values = new java.util.LinkedHashMap<>(filter.values);
            copy.filters.add(item);
        }
        return copy;
    }

    public static List<MovieSort.SortData> merge(List<MovieSort.SortData> categories,
                                                List<Group> groups) {
        groups.clear();
        List<MovieSort.SortData> navigation = new ArrayList<>();
        for (MovieSort.SortData category : categories) {
            String id = channel(category.name);
            if (id == null) {
                navigation.add(category);
                continue;
            }
            Group group = find(groups, id);
            if (group == null) {
                group = new Group(id, MOVIE.equals(id) ? "电影" : "电视剧");
                groups.add(group);
                navigation.add(new MovieSort.SortData(id, group.title));
            }
            MovieSort.SortData snapshot = copy(category);
            if (category.name.contains("榜")) group.ranking = snapshot;
            else if (category.name.contains("筛选")) group.filter = snapshot;
            else if (group.hot == null || category.name.contains("热播")) group.hot = snapshot;
        }
        for (Group group : groups) {
            if (group.hot == null) group.hot = copy(group.filter != null ? group.filter : group.ranking);
            if (group.filter == null && group.hot != null && !group.hot.filters.isEmpty())
                group.filter = copy(group.hot);
            if (SERIES.equals(group.id) && group.ranking != null) {
                for (MovieSort.SortFilter filter : group.ranking.filters) {
                    if (filter.values == null) continue;
                    filter.values.entrySet().removeIf(value -> value.getKey().contains("综艺")
                            || value.getValue().contains("综艺"));
                    String selected = group.ranking.filterSelect.get(filter.key);
                    if (!filter.values.isEmpty()
                            && (selected == null || !filter.values.containsValue(selected))) {
                        group.ranking.filterSelect.put(filter.key,
                                filter.values.values().iterator().next());
                    }
                }
                group.ranking.filters.removeIf(filter -> filter.values == null || filter.values.isEmpty());
            }
            if (SERIES.equals(group.id) && group.filter != null) {
                List<MovieSort.SortFilter> removed = new ArrayList<>();
                for (MovieSort.SortFilter filter : group.filter.filters) {
                    if ("类型".equals(filter.name) && filter.values != null) {
                        for (Map.Entry<String, String> value : filter.values.entrySet()) {
                            if ("电视剧".equals(value.getKey())) {
                                group.filter.filterSelect.put(filter.key, value.getValue());
                                removed.add(filter);
                                break;
                            }
                        }
                    } else if (filter.name != null && filter.name.contains("综艺形式")) {
                        removed.add(filter);
                    } else if ("电视剧形式".equals(filter.name)) {
                        filter.name = "类型";
                    }
                }
                group.filter.filters.removeAll(removed);
            }
        }
        return navigation;
    }

    public static Group find(List<Group> groups, String id) {
        for (Group group : groups) if (group.id.equals(id)) return group;
        return null;
    }
}
