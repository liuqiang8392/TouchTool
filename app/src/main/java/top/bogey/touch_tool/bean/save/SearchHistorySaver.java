package top.bogey.touch_tool.bean.save;

import android.util.Pair;

import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.List;

public class SearchHistorySaver {
    private static SearchHistorySaver instance;

    public static SearchHistorySaver getInstance() {
        synchronized (SearchHistorySaver.class) {
            if (instance == null) {
                instance = new SearchHistorySaver();
            }
        }
        return instance;
    }

    private final MMKV mmkv = MMKV.mmkvWithID("SEARCH_HISTORY_DB", MMKV.SINGLE_PROCESS_MODE);

    public void addSearchHistory(String history) {
        // 先移除，再添加，保证添加的在最前面
        mmkv.remove(history);
        mmkv.encode(history, System.currentTimeMillis());
    }

    public void removeSearchHistory(String history) {
        mmkv.remove(history);
    }

    public void cleanSearchHistory() {
        mmkv.clearAll();
    }

    public List<String> getSearchHistory() {
        String[] keys = mmkv.allKeys();
        if (keys == null) return new ArrayList<>();

        List<Pair<String, Long>> list = new ArrayList<>();
        for (String key : keys) {
            long time = mmkv.decodeLong(key);
            list.add(new Pair<>(key, time));
        }
        list.sort((o1, o2) -> o2.second.compareTo(o1.second));

        List<String> result = new ArrayList<>();
        for (Pair<String, Long> pair : list) {
            result.add(pair.first);
        }
        return result.subList(0, Math.min(result.size(), 10));
    }
}
