package top.bogey.touch_tool.ui.recorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinTouchPath;
import top.bogey.touch_tool.ui.blueprint.picker.FullScreenPicker;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.callback.ResultCallback;

@SuppressLint("ViewConstructor")
public class TouchRecorderFloatView extends FullScreenPicker<PinTouchPath> {
    private final Paint paint;
    private final Handler handler;

    private final List<PinTouchPath.PathPart> pathParts = new ArrayList<>();
    private long lastTime;

    public TouchRecorderFloatView(@NonNull Context context, ResultCallback<PinTouchPath> callback) {
        super(context, callback);

        handler = new Handler(Looper.getMainLooper());

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(DisplayUtil.getAttrColor(context, R.attr.colorPrimaryLight));
        paint.setStrokeWidth(10);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void realShow() {
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);

        if (pathParts.isEmpty()) return;
        for (List<Point> points : getPathPoints()) {
            if (points.size() > 1) {
                Path path = new Path();
                for (Point point : points) {
                    if (path.isEmpty()) path.moveTo(point.x - location[0], point.y - location[1]);
                    else path.lineTo(point.x - location[0], point.y - location[1]);
                }
                canvas.drawPath(path, paint);
            } else if (points.size() == 1) {
                Point point = points.get(0);
                canvas.drawPoint(point.x - location[0], point.y - location[1], paint);
            }
        }
    }

    private Set<List<Point>> getPathPoints() {
        Set<List<Point>> pathPoints = new HashSet<>();
        Map<Integer, List<Point>> points = new HashMap<>();
        pathParts.forEach(pathPart -> {
            pathPart.getPoints().forEach(point -> {
                List<Point> list = points.computeIfAbsent(point.getId(), k -> new ArrayList<>());
                list.add(new Point(point));
                if (point.isEnd()) {
                    pathPoints.add(list);
                    points.remove(point.getId());
                }
            });
        });
        pathPoints.addAll(points.values());
        return pathPoints;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                pathParts.clear();
                lastTime = System.currentTimeMillis();
                addPathPart(event, -1);
            }
            case MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> {
                addPathPart(event, -1);
            }
            case MotionEvent.ACTION_POINTER_UP -> {
                int pointerId = event.getPointerId(event.getActionIndex());
                addPathPart(event, pointerId);
            }
            case MotionEvent.ACTION_UP -> {
                int pointerId = event.getPointerId(event.getActionIndex());
                addPathPart(event, pointerId);
                longTouchSupport(null);

                callback.onResult(new PinTouchPath(pathParts));
            }
        }
        invalidate();
        return true;
    }

    private void addPathPart(MotionEvent event, int endId) {
        long currTime = System.currentTimeMillis();
        PinTouchPath.PathPart pathPart = new PinTouchPath.PathPart((int) (currTime - lastTime));
        for (int i = 0; i < event.getPointerCount(); i++) {
            int pointerId = event.getPointerId(i);
            float currX = event.getX(i), currY = event.getY(i);
            for (int j = 0; j < event.getHistorySize(); j++) {
                currX = event.getHistoricalX(i, j);
                currY = event.getHistoricalY(i, j);
            }
            PinTouchPath.PathPoint pathPoint = new PinTouchPath.PathPoint(pointerId, (int) currX, (int) currY);
            pathPoint.setEnd(pointerId == endId);
            pathPart.addPoint(pathPoint);
        }
        pathParts.add(pathPart);
        longTouchSupport(pathPart);
        lastTime = currTime;
    }

    private void longTouchSupport(PinTouchPath.PathPart part) {
        handler.removeCallbacksAndMessages(null);
        if (part == null) return;
        handler.postDelayed(() -> {
            PinTouchPath.PathPart pathPart = new PinTouchPath.PathPart(part);
            pathPart.setTime(100);
            pathParts.add(pathPart);
            lastTime = System.currentTimeMillis();
            longTouchSupport(pathPart);
        }, 100);
    }
}
