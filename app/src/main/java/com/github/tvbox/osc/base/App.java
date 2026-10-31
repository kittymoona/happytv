package com.github.tvbox.osc.base;

import android.app.Activity;
import androidx.multidex.MultiDexApplication;

import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.callback.EmptyCallback;
import com.github.tvbox.osc.callback.LoadingCallback;
import com.github.tvbox.osc.data.AppDataManager;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.FileUtils;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.kingja.loadsir.core.LoadSir;
import com.orhanobut.hawk.Hawk;
import com.p2p.P2PClass;
import com.whl.quickjs.android.QuickJSLoader;
import com.github.catvod.crawler.JsLoader;

import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.unit.Subunits;

/**
 * @author pj567
 * @date :2020/12/17
 * @description:
 */
public class App extends MultiDexApplication {
    private static App instance;
    private static final long startupClock = android.os.SystemClock.elapsedRealtime();
    private static final StringBuilder startupProfile = new StringBuilder();
    public static synchronized void startupMark(String stage) {
        startupProfile.append(stage).append("=")
                .append(android.os.SystemClock.elapsedRealtime() - startupClock).append("ms\n");
    }
    public static synchronized void saveStartupProfile() {
        if (!com.github.tvbox.osc.BuildConfig.DEBUG) return;
        try (java.io.FileOutputStream output = instance.openFileOutput("startup-profile.txt", MODE_PRIVATE)) {
            output.write(startupProfile.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException ignored) { }
    }

    private static P2PClass p;
    public static String burl;
    private static String dashData;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        startupMark("applicationStart");
        initParams();
        startupMark("preferencesReady");
        // OKGo
        OkGoHelper.init(); //台标获取
        startupMark("networkAndEpgReady");
        // 初始化Web服务器
        ControlManager.init(this);
        //初始化数据库
        AppDataManager.init();
        startupMark("databaseReady");
        LoadSir.beginBuilder()
                .addCallback(new EmptyCallback())
                .addCallback(new LoadingCallback())
                .commit();
        AutoSizeConfig.getInstance().setCustomFragment(true).getUnitsManager()
                .setSupportDP(false)
                .setSupportSP(false)
                .setSupportSubunits(Subunits.MM);
        PlayerHelper.init();
        startupMark("playerLibrariesReady");
        QuickJSLoader.init();
        FileUtils.cleanPlayerCache();
        startupMark("applicationReady");
    }

    private void initParams() {
        // Hawk
        Hawk.init(this).build();
        if (!Hawk.get("yuekan_danmu_off_migrated", false)) {
            Hawk.put(HawkConfig.DANMU_OPEN, false);
            Hawk.put("yuekan_danmu_off_migrated", true);
        }
        if (!Hawk.get("yuekan_default_layout_migrated", false)) {
            Hawk.put(HawkConfig.HOME_LAYOUT, 1);
            Hawk.put("yuekan_default_layout_migrated", true);
        }
        if (!Hawk.get(HawkConfig.HOME_REC_NONE_MIGRATED, false)) {
            Hawk.put(HawkConfig.HOME_REC, HawkConfig.HOME_REC_NONE);
            Hawk.put(HawkConfig.HOME_REC_NONE_MIGRATED, true);
        }
        String configuredApi = Hawk.get(HawkConfig.API_URL, "");
        if (configuredApi == null || configuredApi.trim().isEmpty()
                || "asset://public_config.json".equals(configuredApi)) {
            Hawk.put(HawkConfig.API_URL, "http://www.饭太硬.net/tv");
        }
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) Hawk.put(HawkConfig.DEBUG_OPEN, false);
        if (Hawk.get(HawkConfig.PLAYER_IS_LIVE, false)) Hawk.put(HawkConfig.PLAYER_IS_LIVE, false);
        if (!Hawk.contains(HawkConfig.PLAY_TYPE)) {
            Hawk.put(HawkConfig.PLAY_TYPE, 2);
        } else if (Hawk.get(HawkConfig.PLAY_TYPE, 2) == 0) {
            Hawk.put(HawkConfig.PLAY_TYPE, 2);
        }
    }

    public static App getInstance() {
        return instance;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        JsLoader.destroy();
    }


    private VodInfo vodInfo;
    public void setVodInfo(VodInfo vodinfo){
        this.vodInfo = vodinfo;
    }
    public VodInfo getVodInfo(){
        return this.vodInfo;
    }

    public static P2PClass getp2p() {
        try {
            if (p == null) {
                p = new P2PClass(FileUtils.getExternalCachePath());
            }
            return p;
        } catch (Exception e) {
            LOG.e(e.toString());
            return null;
        }
    }

    public Activity getCurrentActivity() {
        return AppManager.getInstance().currentActivity();
    }

    public void setDashData(String data) {
        dashData = data;
    }
    public String getDashData() {
        return dashData;
    }
}
