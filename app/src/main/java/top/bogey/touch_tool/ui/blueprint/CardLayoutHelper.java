package top.bogey.touch_tool.ui.blueprint;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;

public class CardLayoutHelper {
    public static final int CORNER_GRID_COUNT = 2;

    public static Path calculateLinkPath(float gridSize, PointF start, PointF end, boolean vertical) {
        Path path = new Path();
        path.moveTo(start.x, start.y);

        float dx = Math.abs(start.x - end.x);
        float dy = Math.abs(start.y - end.y);
        float minOffset = gridSize * CORNER_GRID_COUNT;
        if (dx <= minOffset && dy <= minOffset) {
            path.lineTo(end.x, end.y);
            return path;
        }

        PointF startPoint = new PointF(start.x, start.y);
        PointF endPoint = new PointF(end.x, end.y);
        if (vertical) {
            startPoint.y += gridSize * CORNER_GRID_COUNT;
            endPoint.y -= gridSize * CORNER_GRID_COUNT;
        } else {
            startPoint.x += gridSize * CORNER_GRID_COUNT;
            endPoint.x -= gridSize * CORNER_GRID_COUNT;
        }

        path.lineTo(startPoint.x, startPoint.y);

        boolean isXPositive = startPoint.x < endPoint.x;
        boolean isYPositive = startPoint.y < endPoint.y;
        int xScale = isXPositive ? 1 : -1;
        int yScale = isYPositive ? 1 : -1;

        dx = Math.abs(endPoint.x - startPoint.x);
        dy = Math.abs(endPoint.y - startPoint.y);
        boolean xLong = dx > dy;
        float halfLen = Math.abs(dx - dy) / 2;

        /*
        vertical:
            xLong:
                isYPositive: ↓ ← ↓, ↓ → ↓
                ! isYPositive: ← ↖ ←, → ↗ →
            ! xLong:
                isYPositive: ↓ ↙ ↓, ↓ ↘ ↓
                ! isYPositive: ← ↑ ←, → ↑ →
            dx < gridSize / 2:
                isYPositive: ↓
                ! isYPositive: ← ↑ →
        ! vertical:
            xLong:
                isXPositive:  → ↗ →, → ↘ →
                ! isXPositive: ↑ ← ↑, ↓ ← ↓
            ! xLong:
                isXPositive:  → ↑ →, → ↓ →
                ! isXPositive: ↑ ↖ ↑, ↓ ↙ ↓
            dy < gridSize / 2:
                isXPositive:  →
                ! isXPositive: ↓ ← ↑
        */

        boolean flag = true;
        float gridOffset = gridSize * (2 + CORNER_GRID_COUNT);
        if (vertical) {
            if (!isYPositive) {
                if (dx < gridOffset && dy > gridOffset) { //← ↑ →
                    float x = Math.min(endPoint.x, startPoint.x) - gridOffset;
                    path.lineTo(x, startPoint.y);
                    path.lineTo(x, endPoint.y);
                    flag = false;
                }
            }

            if (flag) {
                if (xLong) {
                    if (isYPositive) {  //↓ ← ↓, ↓ → ↓
                        path.lineTo(startPoint.x, startPoint.y + dy / 2);
                        path.lineTo(endPoint.x, endPoint.y - dy / 2);
                    } else {            //← ↖ ←, → ↗ →
                        path.lineTo(startPoint.x + halfLen * xScale, startPoint.y);
                        path.lineTo(endPoint.x - halfLen * xScale, endPoint.y);
                    }
                } else {
                    if (isYPositive) {  //↓ ↙ ↓, ↓ ↘ ↓
                        path.lineTo(startPoint.x, startPoint.y + halfLen);
                        path.lineTo(endPoint.x, endPoint.y - halfLen);
                    } else {            //← ↑ ←, → ↑ →
                        path.lineTo(startPoint.x + dx * xScale / 2, startPoint.y);
                        path.lineTo(endPoint.x - dx * xScale / 2, endPoint.y);
                    }
                }
            }
        } else {
            if (!isXPositive) {
                if (dy < gridOffset && dx > gridOffset) { //↓ ← ↑
                    float y = Math.max(endPoint.y, startPoint.y) + gridSize * (CORNER_GRID_COUNT + 4);
                    path.lineTo(startPoint.x, y);
                    path.lineTo(endPoint.x, y);
                    flag = false;
                }
            }

            if (flag) {
                if (xLong) {
                    if (isXPositive) {  //→ ↗ →, → ↘ →
                        path.lineTo(startPoint.x + halfLen, startPoint.y);
                        path.lineTo(endPoint.x - halfLen, endPoint.y);
                    } else {            //↑ ← ↑, ↓ ← ↓
                        path.lineTo(startPoint.x, startPoint.y + dy * yScale / 2);
                        path.lineTo(endPoint.x, endPoint.y - dy * yScale / 2);
                    }
                } else {
                    if (isXPositive) {  //→ ↑ →, → ↓ →
                        path.lineTo(startPoint.x + dx / 2, startPoint.y);
                        path.lineTo(endPoint.x - dx / 2, endPoint.y);
                    } else {            //↑ ↖ ↑, ↓ ↙ ↓
                        path.lineTo(startPoint.x, startPoint.y + halfLen * yScale);
                        path.lineTo(endPoint.x, endPoint.y - halfLen * yScale);
                    }
                }
            }
        }

        path.lineTo(endPoint.x, endPoint.y);
        path.lineTo(end.x, end.y);

        return path;
    }

    public static class ActionArea {
        private final int ORIGIN_OFFSET = SettingSaver.getInstance().getArrangeCardOffset();
        private final int OFFSET = ORIGIN_OFFSET * 2;

        public Action action;
        public boolean execute;

        public RectF allArea = new RectF(); // 动作使用的所有参数+参数使用的参数+自身共同占据的区域大小
        public RectF actionArea = new RectF(); // 动作对应卡片区域
        public RectF paramsArea = new RectF(); // 参数区域
        public final RectF commonParamsArea; // 公共参数区域，完整包含参数区域
        public final RectF occupiedArea = new RectF(); // 执行真实占用区域，包含子树

        public List<ActionArea> executes = new ArrayList<>();
        public List<ActionArea> params = new ArrayList<>();

        private final float gridSize;

        public ActionArea(CardLayoutView cardLayoutView, Set<Action> handledActions, Action action, RectF commonParamsArea, boolean execute) {
            this.action = action;
            this.execute = execute;
            this.commonParamsArea = commonParamsArea;

            gridSize = cardLayoutView.getGridSize();
            float gridOffset = gridSize * OFFSET;

            ActionCard card = cardLayoutView.getActionCard(action);
            actionArea = new RectF(0, 0, formatToGridPx(card.getWidth()), formatToGridPx(card.getHeight()));

            // 统计动作参数连接和向下执行连接
            Task task = cardLayoutView.getTask();
            List<Action> executes = new ArrayList<>();
            List<Action> params = new ArrayList<>();
            action.getPins().forEach(pin -> {
                if (!pin.isLinked()) return;
                boolean isExecute = pin.getValue() instanceof PinExecute;
                if (isExecute && !pin.isOut()) return;
                if (pin.isOut() && !(isExecute)) return;

                Pin linkedPin = pin.getLinkedPin(task);
                if (linkedPin == null) return;

                Action linkedAction = task.getAction(linkedPin.getOwnerId());
                if (linkedAction == null) return;

                if (handledActions.contains(linkedAction)) return;
                handledActions.add(linkedAction);

                if (isExecute) {
                    executes.add(linkedAction);
                } else {
                    params.add(linkedAction);
                }
            });

            // 计算参数区域，递归计算
            for (int i = 0; i < params.size(); i++) {
                Action paramAction = params.get(i);
                ActionArea area = new ActionArea(cardLayoutView, handledActions, paramAction, new RectF(), false);
                this.params.add(area);
                paramsArea.right = Math.max(paramsArea.width(), area.allArea.width());
                paramsArea.bottom += (area.allArea.height() + (i == params.size() - 1 ? 0 : gridSize));
            }

            commonParamsArea.right = Math.max(commonParamsArea.width(), paramsArea.width());
            commonParamsArea.bottom += Math.max(actionArea.height(), paramsArea.height());

            allArea.right = paramsArea.width() + gridOffset + actionArea.width();
            allArea.bottom = Math.max(actionArea.height(), paramsArea.height());

            // 继续计算向下的执行
            for (int i = 0, executesSize = executes.size(); i < executesSize; i++) {
                Action executeAction = executes.get(i);
                // 第一个执行共用通用参数区域
                ActionArea area = new ActionArea(cardLayoutView, handledActions, executeAction, i == 0 ? commonParamsArea : new RectF(), true);
                this.executes.add(area);
            }
        }

        public ActionArea(CardLayoutView cardLayoutView, List<Action> startActions) {
            gridSize = cardLayoutView.getGridSize();
            commonParamsArea = new RectF();

            Set<Action> handledActions = new HashSet<>();

            for (Action action : startActions) {
                if (handledActions.contains(action)) return;
                handledActions.add(action);

                ActionArea area = new ActionArea(cardLayoutView, handledActions, action, new RectF(), true);
                executes.add(area);
            }
        }

        /**
         * 整理动作区域
         *
         * @param start  动作区域的开始位置
         * @param offset 偏移
         * @return 动作区域最终占据的宽度
         */
        public RectF arrange(PointF start, float offset) {
            float gridOffset = gridSize * OFFSET;

            // 设置动作位置
            if (action != null && !action.isLocked()) {
                float actionStartX = start.x + offset;
                float actionStartY = start.y;
                int x = formatToGrid(actionStartX);
                int y = formatToGrid(actionStartY);
                action.setPos(x, y);
            }

            // 每个子执行需要互不干扰，所以需要每次偏移之前的执行宽度
            float areaTotalWidth = 0;
            float areaTotalHeight = 0;

            float executeStartX = start.x;
            float executeStartY = start.y + allArea.height() + (action == null ? 0 : gridOffset);
            for (int i = 0; i < executes.size(); i++) {
                ActionArea area = executes.get(i);
                RectF areaOccupiedArea = area.arrange(new PointF(executeStartX, executeStartY), area.commonParamsArea.width());

                float realExecuteWidth = area.commonParamsArea.width() + areaOccupiedArea.width() + gridOffset;
                executeStartX += realExecuteWidth;

                if (i == 0) areaTotalWidth += areaOccupiedArea.width();
                else areaTotalWidth += realExecuteWidth;

                areaTotalHeight = Math.max(areaTotalHeight, areaOccupiedArea.height());
            }

            float paramStartX = start.x;
            float paramStartY = start.y;
            for (ActionArea area : params) {
                float paramOffset = commonParamsArea.width() - area.actionArea.width() - gridOffset;
                if (!execute) paramOffset = offset - area.actionArea.width() - gridOffset;
                area.arrange(new PointF(paramStartX, paramStartY), paramOffset);
                paramStartY += (area.allArea.height() + gridOffset);
            }

            areaTotalHeight += (allArea.height() + gridOffset);

            occupiedArea.right = Math.max(actionArea.width(), areaTotalWidth);
            occupiedArea.bottom = Math.max(allArea.height(), areaTotalHeight);
            return occupiedArea;
        }

        // 计算当前支线块
        public void compact() {
            float gridOffset = gridSize * OFFSET;

            List<RectF> rectList = new ArrayList<>();
            for (int i = 0; i < executes.size(); i++) {
                ActionArea area = executes.get(i);
                area.compact();

                if (i != 0) {
                    ActionArea prevArea = executes.get(i - 1);
                    getAllActionArea(rectList, prevArea);
                    RectF rectF = calculateRealActionArea(area);
                    float left = rectF.left - area.commonParamsArea.width();
                    float top = rectF.top;
                    float right = rectF.left + area.occupiedArea.width();
                    float bottom = Math.max(rectF.bottom, rectF.top + area.occupiedArea.height());
                    RectF blockArea = new RectF(left, top, right, bottom);

                    float minLeft = findMinLeft(rectList, blockArea);
                    float targetLeft = minLeft + gridOffset + area.commonParamsArea.width();

                    offsetActionArea(area, formatToGrid(targetLeft - rectF.left));
                }
            }
        }

        private static void offsetActionArea(ActionArea area, int grid) {
            if (grid == 0) return;
            area.action.getPos().offset(grid, 0);

            for (ActionArea execute : area.executes) {
                offsetActionArea(execute, grid);
            }

            for (ActionArea param : area.params) {
                offsetActionArea(param, grid);
            }
        }

        private static float findMinLeft(List<RectF> rectList, RectF blockArea) {
            float minLeft = 0;
            for (RectF rectF : rectList) {
                // 跳过不在同一行的
                if (rectF.bottom < blockArea.top || rectF.top > blockArea.bottom) continue;
                minLeft = Math.max(rectF.right, minLeft);
            }
            return minLeft;
        }

        private static void getAllActionArea(List<RectF> rectList, ActionArea area) {
            rectList.add(calculateRealActionArea(area));

            for (ActionArea execute : area.executes) {
                getAllActionArea(rectList, execute);
            }
            for (ActionArea param : area.params) {
                getAllActionArea(rectList, param);
            }
        }

        private static RectF calculateRealActionArea(ActionArea area) {
            Point pos = area.action.getPos();
            float x = pos.x * area.gridSize;
            float y = pos.y * area.gridSize;
            RectF rectF = new RectF(area.actionArea);
            rectF.offset(x, y);
            return rectF;
        }

        private int formatToGridPx(float size) {
            return (int) (Math.ceil(size / gridSize) * gridSize);
        }

        private int formatToGrid(float size) {
            return (int) (size / gridSize);
        }
    }
}
