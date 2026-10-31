package com.github.tvbox.osc.api;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.base.App;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/** Built-in catalog of publisher-hosted public media; no downloaded executable plugin. */
public final class PublicMediaSpider extends Spider {
    private JsonObject catalog() {
        try (InputStream input = App.getInstance().getAssets().open("public_media.json");
             InputStreamReader reader = new InputStreamReader(input, "UTF-8")) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception error) {
            throw new IllegalStateException("无法读取公开影音目录", error);
        }
    }

    private String result(JsonArray list) {
        JsonObject result = new JsonObject();
        result.add("list", list);
        result.addProperty("page", 1);
        result.addProperty("pagecount", 1);
        result.addProperty("limit", list.size());
        result.addProperty("total", list.size());
        return result.toString();
    }

    @Override
    public String homeContent(boolean filter) {
        return catalog().toString();
    }

    @Override
    public String homeVideoContent() {
        return result(catalog().getAsJsonArray("list"));
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        JsonArray list = new JsonArray();
        if ("1".equals(pg)) {
            for (JsonElement element : catalog().getAsJsonArray("list")) {
                if (tid.equals(element.getAsJsonObject().get("type_id").getAsString())) {
                    list.add(element);
                }
            }
        }
        return result(list);
    }

    @Override
    public String detailContent(List<String> ids) {
        JsonArray list = new JsonArray();
        for (JsonElement element : catalog().getAsJsonArray("list")) {
            if (ids.contains(element.getAsJsonObject().get("vod_id").getAsString())) {
                list.add(element);
            }
        }
        return result(list);
    }

    @Override
    public String searchContent(String key, boolean quick) {
        JsonArray list = new JsonArray();
        String query = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        for (JsonElement element : catalog().getAsJsonArray("list")) {
            if (element.getAsJsonObject().get("vod_name").getAsString()
                    .toLowerCase(Locale.ROOT).contains(query)) {
                list.add(element);
            }
        }
        return result(list);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        // Only URLs present in the bundled catalog can be played through this source.
        for (JsonElement element : catalog().getAsJsonArray("list")) {
            for (String episode : element.getAsJsonObject().get("vod_play_url").getAsString().split("#")) {
                int separator = episode.indexOf('$');
                if (separator >= 0 && id.equals(episode.substring(separator + 1))) {
                    JsonObject result = new JsonObject();
                    result.addProperty("parse", 0);
                    result.addProperty("url", id);
                    return result.toString();
                }
            }
        }
        return "{}";
    }
}
