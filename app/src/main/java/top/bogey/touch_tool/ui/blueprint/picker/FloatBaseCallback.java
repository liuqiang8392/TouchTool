package top.bogey.touch_tool.ui.blueprint.picker;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.float_window_manager.FloatCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class FloatBaseCallback implements FloatCallback {
    private static final String TAG = FloatBaseCallback.class.getName();
    public static boolean Block = false;


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
    }

    @Override
    public void onShow(String tag) {
        Log.d(TAG, "onShow: " + tag);
        FloatWindow.hideAll(tag);

        MainActivity activity = MainApplication.getInstance().getActivity();
        if (activity != null) {
            if (Block || (SettingSaver.FREE_FORM_OPTIMIZE.get() && DisplayUtil.isInFreeFormMode(activity))) return;
            activity.moveTaskToBack(true);
        }
    }

    @Override
    public void onHide() {
        Log.d(TAG, "onHide: ");
    }

    @Override
    public void onDismiss() {
        Log.d(TAG, "onDismiss: ");
        if (!FloatWindow.showLast()) {
            MainActivity activity = MainApplication.getInstance().getActivity();
            if (activity != null) {
                if (Block || (SettingSaver.FREE_FORM_OPTIMIZE.get() && DisplayUtil.isInFreeFormMode(activity))) return;
                ActivityManager manager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
                try {
                    manager.moveTaskToFront(activity.getTaskId(), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDrag() {
        Log.d(TAG, "onDrag: ");
    }

    @Override
    public void onDragEnd() {
        Log.d(TAG, "onDragEnd: ");
    }

    @Override
    public void onRotate() {
        Log.d(TAG, "onRotate: ");
    }

    @Override
    public boolean onTouch(MotionEvent event) {
        return false;
    }
}
