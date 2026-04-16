package top.bogey.touch_tool.ui.blueprint.card;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.FloatInputConfigCardBinding;
import top.bogey.touch_tool.ui.blueprint.picker.FloatBaseCallback;
import top.bogey.touch_tool.ui.blueprint.pin.PinInputConfigView;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;
import top.bogey.touch_tool.ui.custom.float_view.ActionFloatViewCallback;
import top.bogey.touch_tool.ui.custom.float_view.KeepAliveFloatView;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.callback.BooleanResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public class InputConfigActionFloatCard extends ActionCard implements FloatInterface {

    public static void showInputConfig(Task task, Action action, int timeout, BooleanResultCallback callback) {
        showInputConfig(task, action, timeout, callback, EAnchor.CENTER, EAnchor.CENTER, SettingSaver.FLOAT_VIEW_POS.get());
    }

    public static void showInputConfig(Task task, Action action, int timeout, BooleanResultCallback callback, EAnchor anchor, EAnchor gravity, Point location) {
        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            InputConfigActionFloatCard card = new InputConfigActionFloatCard(keepView.getThemeContext(), task, action, timeout, callback);
            card.show();
            FloatWindow.setLocation(InputConfigActionFloatCard.class.getName(), anchor, gravity, location);
        });
    }

    private final BooleanResultCallback callback;
    private int timeout;
    private boolean canceled = false;
    private FloatInputConfigCardBinding binding;

    public InputConfigActionFloatCard(Context context, Task task, Action action, int timeout, BooleanResultCallback callback) {
        super(context, task, action);
        this.callback = callback;
        this.timeout = timeout;
        if (timeout > 0) {
            refreshTimeout();
            binding.cancelButton.setOnClickListener(v -> {
                canceled = true;
                binding.cancelButton.setVisibility(GONE);
            });
        } else binding.cancelButton.setVisibility(GONE);

        binding.title.setText(action.getValidDescription());
        setCardBackgroundColor(DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorSurfaceContainerHighest));
        setElevation(0);
        setStrokeWidth(0);

        binding.closeButton.setOnClickListener(v -> {
            callback.onResult(false);
            dismiss();
        });
    }

    @Override
    public void init() {
        binding = FloatInputConfigCardBinding.inflate(LayoutInflater.from(getContext()), this, true);
        binding.enterButton.setOnClickListener(v -> {
            callback.onResult(true);
            dismiss();
        });
    }

    @SuppressLint("DefaultLocale")
    public void refreshTimeout() {
        if (canceled) return;
        if (timeout <= 0) {
            callback.onResult(true);
            dismiss();
            return;
        }
        timeout -= 100;
        binding.cancelButton.setText(String.format("%.1fs", timeout / 1000f));
        postDelayed(this::refreshTimeout, 100);
    }

    @Override
    public void refreshCardInfo() {

    }

    @Override
    public void refreshCardLockState() {

    }

    @Override
    public boolean check() {
        return true;
    }

    @Override
    public void addPinView(Pin pin, int offset) {
        if (pin.isDynamic()) {
            PinView pinView = new PinInputConfigView(getContext(), this, pin);
            binding.inBox.addView(pinView);
            pinView.expand(Action.ExpandType.FULL);
            pinViews.put(pin.getId(), pinView);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.originOnMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int[] location = new int[2];
            binding.scrollView.getLocationOnScreen(location);
            if (new RectF(location[0], location[1], location[0] + binding.scrollView.getWidth(), location[1] + binding.scrollView.getHeight()).contains(x, y)) {
                FloatWindow.setDragAble(InputConfigActionFloatCard.class.getName(), false);
                return super.onInterceptTouchEvent(event);
            }
            FloatWindow.setDragAble(InputConfigActionFloatCard.class.getName(), true);
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public void show() {
        Point point = SettingSaver.FLOAT_VIEW_POS.get();
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(InputConfigActionFloatCard.class.getName())
                .setLocation(EAnchor.CENTER, point.x, point.y)
                .setExistEditText(true)
                .setCallback(new ActionFloatViewCallback(InputConfigActionFloatCard.class.getName()))
                .show();
        FloatBaseCallback.Block = true;
    }

    @Override
    public void dismiss() {
        FloatWindow.dismiss(InputConfigActionFloatCard.class.getName());
        FloatBaseCallback.Block = false;
    }
}
