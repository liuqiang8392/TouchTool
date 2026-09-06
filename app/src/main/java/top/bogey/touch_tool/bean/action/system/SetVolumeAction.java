package top.bogey.touch_tool.bean.action.system;

import android.content.Context;
import android.media.AudioManager;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.service.TaskRunnable;

public class SetVolumeAction extends ExecuteAction {
    private final transient Pin volumeTypePin = new Pin(new PinSingleSelect(R.array.volume_type), R.string.set_volume_action);
    protected final transient Pin volumePin = new Pin(new PinInteger(), R.string.set_volume_action_value);

    public SetVolumeAction() {
        super(ActionType.SET_VOLUME);
        addPins(volumeTypePin, volumePin);
    }

    public SetVolumeAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(volumeTypePin, volumePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinSingleSelect volumeType = getPinValue(runnable, volumeTypePin);
        PinNumber<?> volume = getPinValue(runnable, volumePin);

        Context context = MainApplication.getInstance();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int idx = volumeType.getIndex();
        if (idx == 0) {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int value = Math.clamp(volume.intValue(), 0, maxVolume);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
        } else if (idx == 1) {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            int value = Math.clamp(volume.intValue(), 0, maxVolume);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, value, 0);
        } else if (idx == 2) {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int value = Math.clamp(volume.intValue(), 0, maxVolume);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, value, 0);
        }
        executeNext(runnable, outPin);
    }
}
