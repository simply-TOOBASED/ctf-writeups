package android.support.v7.widget;

import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

class AppCompatImageHelper {
    private static final int[] VIEW_ATTRS = new int[]{16843033};
    private final TintManager mTintManager;
    private final ImageView mView;

    AppCompatImageHelper(ImageView view, TintManager tintManager) {
        this.mView = view;
        this.mTintManager = tintManager;
    }

    void loadFromAttributes(AttributeSet attrs, int defStyleAttr) {
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(this.mView.getContext(), attrs, VIEW_ATTRS, defStyleAttr, 0);
        try {
            if (a.hasValue(0)) {
                this.mView.setImageDrawable(a.getDrawable(0));
            }
            a.recycle();
        } catch (Throwable th) {
            a.recycle();
        }
    }

    void setImageResource(int resId) {
        if (resId != 0) {
            this.mView.setImageDrawable(this.mTintManager != null ? this.mTintManager.getDrawable(resId) : ContextCompat.getDrawable(this.mView.getContext(), resId));
        } else {
            this.mView.setImageDrawable(null);
        }
    }
}
