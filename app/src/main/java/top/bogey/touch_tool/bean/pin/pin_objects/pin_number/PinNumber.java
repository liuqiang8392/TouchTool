package top.bogey.touch_tool.bean.pin.pin_objects.pin_number;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.math.BigDecimal;

import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.PinType;

public abstract class PinNumber<T extends Number> extends PinObject {
    protected T value;

    protected PinNumber(PinSubType subType) {
        super(PinType.NUMBER, subType);
    }

    protected PinNumber(PinSubType subType, T value) {
        this(subType);
        this.value = value;
    }

    protected PinNumber(JsonObject jsonObject) {
        super(jsonObject);
    }

    @Override
    public void sync(PinBase value) {
        if (value instanceof PinNumber<?> pinNumber) {
            this.value = (T) pinNumber.value;
        }
    }

    @Override
    public boolean linkFromAble(PinBase pin) {
        if (getType().getGroup() == pin.getType().getGroup()) {
            if (isDynamic() || pin.isDynamic()) return true;
            return pin instanceof PinNumber<?>;
        }
        return false;
    }

    @Override
    public boolean linkToAble(PinBase pin) {
        if (getType().getGroup() == pin.getType().getGroup()) {
            if (isDynamic() || pin.isDynamic()) return true;
            return pin instanceof PinNumber<?>;
        }
        return false;
    }

    public int intValue() {
        return value.intValue();
    }

    public long longValue() {
        return value.longValue();
    }

    public float floatValue() {
        return value.floatValue();
    }

    public double doubleValue() {
        return value.doubleValue();
    }

    private BigDecimal bigDecimalValue() {
        return new BigDecimal(value.toString());
    }

    @NonNull
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PinNumber<?> pinNumber)) return false;

        return bigDecimalValue().compareTo(pinNumber.bigDecimalValue()) == 0;
    }

    @Override
    public int hashCode() {
        int result = getType().hashCode();
        result = 31 * result + bigDecimalValue().stripTrailingZeros().hashCode();
        return result;
    }
}
