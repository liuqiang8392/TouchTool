package top.bogey.touch_tool.bean.save.setting;


import android.os.Parcelable;

import com.tencent.mmkv.MMKV;

public class SettingSave<T> {
    private final String key;

    private final T defaultValue;

    private final static MMKV mmkv = MMKV.defaultMMKV();

    public SettingSave(String key, T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public T get() {
        Object value = switch (defaultValue) {
            case String stringValue -> mmkv.decodeString(key, stringValue);
            case Integer integerValue -> mmkv.decodeInt(key, integerValue);
            case Boolean booleanValue -> mmkv.decodeBool(key, booleanValue);
            case Float floatValue -> mmkv.decodeFloat(key, floatValue);
            case Parcelable parcelableValue -> mmkv.decodeParcelable(key, parcelableValue.getClass());
            case null, default -> defaultValue;
        };
        return (T) value;
    }

    public void set(T value) {
        switch (value) {
            case String stringValue -> mmkv.encode(key, stringValue);
            case Integer integerValue -> mmkv.encode(key, integerValue);
            case Boolean booleanValue -> mmkv.encode(key, booleanValue);
            case Float floatValue -> mmkv.encode(key, floatValue);
            case Parcelable parcelableValue -> mmkv.encode(key, parcelableValue);
            default -> {
            }
        }
    }
}
