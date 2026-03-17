package top.bogey.touch_tool.bean.save.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

public class LetterBox {
    private final Bitmap bitmap;
    private final float scale;
    private final float offsetX;
    private final float offsetY;

    public LetterBox(Bitmap bitmap, int targetWidth, int targetHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        scale = Math.min(targetWidth * 1f / width, targetHeight * 1f / height);

        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        Bitmap resizeBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        this.bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(this.bitmap);

        offsetX = (targetWidth - newWidth) / 2f;
        offsetY = (targetHeight - newHeight) / 2f;

        canvas.drawColor(Color.rgb(114, 114, 114));
        canvas.drawBitmap(resizeBitmap, offsetX, offsetY, null);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public float getScale() {
        return scale;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }
}
