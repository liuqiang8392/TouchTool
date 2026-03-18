package top.bogey.touch_tool.bean.save;

import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        mmkv.encode(history, true);
    }

    public void removeSearchHistory(String history) {
        mmkv.remove(history);
    }

    public void cleanSearchHistory() {
        mmkv.clearAll();
    }

    public List<String> getSearchHistory() {
        Set<String> set = new HashSet<>();
        List<String> list = new ArrayList<>();
        String[] keys = mmkv.allKeys();
        if (keys == null) return list;
        for (String key : keys) {
            if (mmkv.decodeBool(key) && !set.contains(key)) {
                list.add(key);
                set.add(key);
            }
            if (list.size() >= 10) break;
        }
        return list;
    }
}
