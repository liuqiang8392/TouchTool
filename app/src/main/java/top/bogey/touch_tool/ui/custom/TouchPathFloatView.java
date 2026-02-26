package top.bogey.touch_tool.ui.custom;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinTouchPath;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinTouchPath.PathPart;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class TouchPathFloatView extends AppCompatImageView implements FloatInterface {
    private final static int LINE_WIDTH = 10;
    private final static int PADDING = LINE_WIDTH * 2;

    private final String tag = UUID.randomUUID().toString();

    private final Paint paint;
    private Canvas canvas = null;

    private List<PathPart> pathParts;
    private float timeScale;
    private Rect gestureArea;

    private final HashMap<Integer, Point> lastPoints = new HashMap<>();
    private boolean showing = false;

    public static void showGesture(int x, int y) {
        PathPart pathPart = new PathPart(0, x, y);
        showGesture(Collections.singletonList(pathPart), 1f);
    }

    public static void showGesture(List<PathPart> pathParts, float timeScale) {
        boolean showTouch = SettingSaver.getInstance().isShowGestureTrack();
        if (!showTouch) return;

        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            TouchPathFloatView floatView = new TouchPathFloatView(keepView.getThemeContext());
            floatView.innerShowGesture(pathParts, timeScale);
            floatView.show();
        });
    }

    public TouchPathFloatView(@NonNull Context context) {
        super(context);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(DisplayUtil.getAttrColor(getContext(), R.attr.colorPrimaryLight));
        paint.setStrokeWidth(LINE_WIDTH);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
    }

    private void innerShowGesture(List<PathPart> pathParts, float timeScale) {
        this.pathParts = pathParts;
        this.timeScale = timeScale <= 0 ? 1f : timeScale;
        List<Point> points = new ArrayList<>();
        for (PathPart part : pathParts) {
            points.addAll(part.getPoints());
        }
        gestureArea = DisplayUtil.getPointsArea(points);

        int width = gestureArea.width() + PADDING * 2;
        int height = gestureArea.height() + PADDING * 2;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        canvas.translate(PADDING - gestureArea.left, PADDING - gestureArea.top);
        setImageBitmap(bitmap);

        int statusBarHeight = DisplayUtil.getStatusBarHeight(null, null);
        gestureArea.offset(0, -statusBarHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = gestureArea.width() + PADDING * 2;
        int height = gestureArea.height() + PADDING * 2;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!showing) {
            showing = true;
            startAnimate();
        }
    }

    private void startAnimate() {
        if (pathParts.size() == 1) {
            PathPart pathPart = pathParts.get(0);
            paint.setStrokeWidth(LINE_WIDTH * 2);
            pathPart.getPoints().forEach(point -> {
                canvas.drawCircle(point.x, point.y, LINE_WIDTH, paint);
                postDelayed(() -> animate().alpha(0).withEndAction(this::dismiss), pathPart.getTime());
            });
        } else {
            int time = 0;
            for (PathPart pathPart : pathParts) {
                time += pathPart.getTime();
            }

            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration((long) (time / timeScale));
            animator.addUpdateListener(animation -> {
                float now = animation.getCurrentPlayTime() * timeScale;
                int index = 0;
                int totalTime = 0;
                float percent = 0;
                for (int i = 0; i < pathParts.size(); i++) {
                    PathPart pathPart = pathParts.get(i);
                    if (now < totalTime + pathPart.getTime()) {
                        index = i;
                        percent = (now - totalTime) / pathPart.getTime();
                        break;
                    }
                    totalTime += pathPart.getTime();
                }
                if (index > 0) {
                    PathPart lastPart = pathParts.get(index - 1);
                    PathPart part = pathParts.get(index);
                    for (PinTouchPath.PathPoint point : part.getPoints()) {
                        PinTouchPath.PathPoint lastPathPoint = lastPart.getPoint(point.getId());
                        if (lastPathPoint == null) continue;

                        int nextX = (int) ((point.x - lastPathPoint.x) * percent + lastPathPoint.x);
                        int nextY = (int) ((point.y - lastPathPoint.y) * percent + lastPathPoint.y);
                        Point lastPoint = lastPoints.computeIfAbsent(point.getId(), id -> new Point(lastPathPoint.x, lastPathPoint.y));
                        canvas.drawLine(lastPoint.x, lastPoint.y, nextX, nextY, paint);
                        lastPoint.set(nextX, nextY);
                    }
                }
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animate().alpha(0).withEndAction(() -> dismiss());
                }
            });

            animator.start();
        }
    }

    @Override
    public void show() {
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(tag)
                .setLocation(EAnchor.TOP_LEFT, gestureArea.left - PADDING, gestureArea.top - PADDING)
                .setAnchor(EAnchor.TOP_LEFT)
                .setDragAble(false)
                .setSpecial(true)
                .setHideByScreenshot(true)
                .setFlag(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                .show();
    }

    @Override
    public void dismiss() {
        FloatWindow.dismiss(tag);
    }
}
