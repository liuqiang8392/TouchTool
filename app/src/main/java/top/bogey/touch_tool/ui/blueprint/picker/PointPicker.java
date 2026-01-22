package top.bogey.touch_tool.ui.blueprint.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.databinding.FloatPickerPointBinding;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.callback.ResultCallback;

@SuppressLint("ViewConstructor")
public class PointPicker extends FullScreenPicker<Point> {
    private final FloatPickerPointBinding binding;
    private final Paint bitmapPaint;

    private float currentX, currentY;
    private float lastX, lastY;
    private boolean picking = false;

    public PointPicker(@NonNull Context context, ResultCallback<Point> callback, Point point) {
        super(context, callback);
        binding = FloatPickerPointBinding.inflate(LayoutInflater.from(context), this, true);
        currentX = point.x;
        currentY = point.y;

        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);

        binding.backButton.setOnClickListener(v -> dismiss());

        binding.saveButton.setOnClickListener(v -> {
            callback.onResult(new Point((int) currentX, (int) currentY));
            dismiss();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();

        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            if (currentX == 0 && currentY == 0) {
                currentX = x;
                currentY = y;
            }
            picking = false;
            lastX = x;
            lastY = y;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            float dx = x - lastX;
            float dy = y - lastY;
            if (dx == 0 && dy == 0) return true;
            currentX += dx / 5;
            currentY += dy / 5;
            currentX = Math.max(0, Math.min(currentX, getWidth() + location[0] - 1));
            currentY = Math.max(0, Math.min(currentY, getHeight() + location[1] - 1));
            lastX = x;
            lastY = y;
            picking = true;
        }

        if (action == MotionEvent.ACTION_UP) {
            if (picking) {
                picking = false;
            } else {
                currentX = x;
                currentY = y;
            }
        }
        refreshUI();
        return true;
    }

    @Override
    protected void realShow() {
        refreshUI();
    }

    @SuppressLint("DefaultLocale")
    private void refreshUI() {
        binding.posText.setText(String.format("%.0f, %.0f", currentX, currentY));
        binding.posBox.post(() -> {
            binding.posBox.setX(currentX - binding.posBox.getWidth() / 2f - location[0]);
            float px = DisplayUtil.dp2px(getContext(), 4);
            if (currentY + binding.posBox.getHeight() > getHeight()) {
                binding.posBox.setScaleY(-1f);
                binding.posText.setScaleY(-1f);
                binding.posBox.setY(currentY - binding.posBox.getHeight() + px - location[1]);
            } else {
                binding.posBox.setScaleY(1f);
                binding.posText.setScaleY(1f);
                binding.posBox.setY(currentY - px - location[1]);
            }
        });

        invalidate();
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (picking) {
            float scale = 8;
            int width = (int) (getWidth() / scale);
            int height = (int) (getHeight() / scale);
            Rect area = new Rect((int) (currentX - width / 2f), (int) (currentY - height / 2f), (int) (currentX + width / 2f), (int) (currentY + height / 2f));
            float x = getWidth() / 2f - area.width() * scale / 2 - area.left * scale;
            float y = getHeight() / 2f - area.height() * scale / 2 - area.top * scale;
            canvas.translate(x, y);
            canvas.scale(scale, scale);
        }

        Bitmap screenShot = screenInfo.getScreenShot();
        if (screenShot != null) canvas.drawBitmap(screenShot, 0, 0, bitmapPaint);

        super.dispatchDraw(canvas);
    }
}
