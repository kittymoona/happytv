package com.github.tvbox.osc.ui.tv;

import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

/** Stable persisted layout ids; names are shared by home and settings. */
public final class HomeLayouts {
    public static final String[] NAMES = {
            "A · 继续观看与片单", "B · 大图展示", "C · 个人首页",
            "D · 继续看与直播", "E · 大按钮导航", "F · 三张影片卡"
    };
    public static final String[] DESCRIPTIONS = {
            "四个分类入口，继续观看与当前源片单各一行",
            "一张大图突出影片，旁边分类入口，下方当前源片单",
            "突出最近一部影片，下方最近观看，不显示源片单",
            "继续看和直播最显眼，下方电影、剧集、综艺",
            "大字大按钮，适合主要看直播和追剧",
            "四个分类入口，一屏显示三部最近观看的影片"
    };
    public static int selected() {
        int value = Hawk.get(HawkConfig.HOME_LAYOUT, 1);
        return value >= 0 && value < NAMES.length ? value : 1;
    }
    private HomeLayouts() { }
}
