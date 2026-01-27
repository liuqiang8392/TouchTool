package top.bogey.touch_tool.ui.blueprint;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;
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
        private final int MORE_OFFSET = SettingSaver.getInstance().getArrangeCardOffset();
        private final int SCALE = MORE_OFFSET * 2;

        public Action action;
        public boolean execute;
        // 动作对应卡片区域
        public RectF actionArea = new RectF();
        // 公共参数区域
        public RectF commonParamsArea;
        // 动作使用的参数与自身共同占据的区域大小
        public RectF allArea = new RectF();

        public List<ActionArea> executes = new ArrayList<>();
        public List<ActionArea> params = new ArrayList<>();

        private float gridSize;

        public ActionArea(CardLayoutView cardLayoutView, Set<Action> handledActions, Action action, RectF commonParamsArea, boolean execute) {
            this.action = action;
            this.execute = execute;
            this.commonParamsArea = commonParamsArea;

            gridSize = cardLayoutView.getGridSize();
            float scaleGridSize = gridSize * SCALE;

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
            RectF paramsArea = new RectF();
            for (Action paramAction : params) {
                ActionArea area = new ActionArea(cardLayoutView, handledActions, paramAction, new RectF(), false);
                this.params.add(area);
                paramsArea.right = Math.max(paramsArea.width(), area.allArea.width());
                paramsArea.bottom += area.allArea.height() + gridSize;
            }

            // 参数区域不为空，偏移动作区域，同时尝试扩展公共参数区域
            if (!paramsArea.isEmpty()) {
                float offset = paramsArea.width() + scaleGridSize;
                actionArea.offset(offset, 0);
                commonParamsArea.right = Math.max(commonParamsArea.right, offset);
            }

            // 计算所有区域，即动作区域和参数区域的总和
            allArea.union(paramsArea);
            allArea.union(actionArea);

            // 继续计算向下的执行
            for (int i = 0, executesSize = executes.size(); i < executesSize; i++) {
                Action executeAction = executes.get(i);
                // 第一个执行共用通用参数区域
                ActionArea area = new ActionArea(cardLayoutView, handledActions, executeAction, i == 0 ? commonParamsArea : new RectF(), true);
                this.executes.add(area);
            }
        }

        public ActionArea(CardLayoutView cardLayoutView, Set<Action> handledActions, List<Action> startActions) {
            gridSize = cardLayoutView.getGridSize();

            for (Action action : startActions) {
                if (handledActions.contains(action)) return;
                handledActions.add(action);

                ActionArea area = new ActionArea(cardLayoutView, handledActions, action, new RectF(), true);
                executes.add(area);
            }
            commonParamsArea = new RectF();
        }

        /**
         * 整理动作区域
         *
         * @param cardLayoutView         布局界面，用来获取网格大小
         * @param start                  动作区域的开始位置
         * @param parentCommonParamsArea 父级动作的公共参数区域
         * @return 动作区域最终占据的宽度
         */
        public int arrange(CardLayoutView cardLayoutView, Point start, RectF parentCommonParamsArea) {
            gridSize = cardLayoutView.getGridSize();
            float scaleGridSize = gridSize * SCALE;

            // 设置动作位置
            if (action != null && !action.isLocked()) {
                int actionStartX;
                if (execute) {
                    // 执行动作只需要偏移参数区域
                    actionStartX = (int) (start.x + commonParamsArea.width());
                } else {
                    // 参数动作需要右对齐，所以用父参数区域去减间隔和自身的宽度
                    actionStartX = (int) (start.x + parentCommonParamsArea.width() - scaleGridSize - actionArea.width());
                }
                int actionStartY = start.y;
                int x = formatToGrid(actionStartX);
                int y = formatToGrid(actionStartY);
                action.setPos(x, y);
            }

            int executeWidth = (int) (actionArea.width());

            // 每个子执行需要互不干扰，所以需要每次偏移之前的执行宽度
            int areaTotalExecuteWidth = 0;
            int executeStartX = start.x;
            int executeStartY = (int) (start.y + allArea.height() + (action == null ? 0 : scaleGridSize));
            for (int i = 0; i < executes.size(); i++) {
                ActionArea area = executes.get(i);
                int areaExecuteWidth = area.arrange(cardLayoutView, new Point(executeStartX, executeStartY), commonParamsArea);
                executeStartX += (int) (scaleGridSize + area.commonParamsArea.width() + areaExecuteWidth);
                if (i == 0) areaTotalExecuteWidth += areaExecuteWidth;
                else areaTotalExecuteWidth += (int) (scaleGridSize + area.commonParamsArea.width() + areaExecuteWidth);
            }

            int paramStartX = start.x;
            int paramStartY = start.y;
            for (ActionArea area : params) {
                RectF paramArea = commonParamsArea;
                if (!execute) {
                    paramArea = new RectF(parentCommonParamsArea);
                    paramArea.right += (-scaleGridSize - actionArea.width());
                }
                area.arrange(cardLayoutView, new Point(paramStartX, paramStartY), paramArea);
                paramStartY += (int) (gridSize + area.allArea.height());
            }

            return Math.max(executeWidth, areaTotalExecuteWidth);
        }

        private int formatToGridPx(float size) {
            return (int) (Math.ceil(size / gridSize) * gridSize);
        }

        private int formatToGrid(float size) {
            return (int) (size / gridSize);
        }
    }
}
