package top.bogey.touch_tool.bean.other;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SavedScreenNodeInfo {
    private final long time = System.currentTimeMillis();
    private String image;
    private final List<SavedNodeInfo> roots = new ArrayList<>();

    public SavedScreenNodeInfo(Bitmap image, List<NodeInfo> roots) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            image.compress(Bitmap.CompressFormat.WEBP, 100, stream);
            byte[] bytes = stream.toByteArray();
            this.image = Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (IOException ignored) {
        }

        for (NodeInfo root : roots) {
            this.roots.add(new SavedNodeInfo(root));
        }
    }

    public long getTime() {
        return time;
    }

    public Bitmap getImage() {
        if (image == null) return null;
        byte[] bytes = Base64.decode(image, Base64.NO_WRAP);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public List<NodeInfo> getRoots() {
        List<NodeInfo> roots = new ArrayList<>();
        for (SavedNodeInfo root : this.roots) {
            roots.add(new NodeInfo(root));
        }
        return roots;
    }

    public boolean isValid() {
        return image != null && !image.isEmpty() && !roots.isEmpty();
    }
}
