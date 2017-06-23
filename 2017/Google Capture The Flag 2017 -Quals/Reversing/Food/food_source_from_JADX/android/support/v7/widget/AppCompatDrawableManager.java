package android.support.v7.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.DrawableContainer.DrawableContainerState;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build.VERSION;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.LruCache;
import android.support.v7.appcompat.C0140R;
import android.util.Log;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.WeakHashMap;

public final class AppCompatDrawableManager {
    private static final int[] COLORFILTER_COLOR_BACKGROUND_MULTIPLY = new int[]{C0140R.drawable.abc_popup_background_mtrl_mult, C0140R.drawable.abc_cab_background_internal_bg, C0140R.drawable.abc_menu_hardkey_panel_mtrl_mult};
    private static final int[] COLORFILTER_COLOR_CONTROL_ACTIVATED = new int[]{C0140R.drawable.abc_textfield_activated_mtrl_alpha, C0140R.drawable.abc_textfield_search_activated_mtrl_alpha, C0140R.drawable.abc_cab_background_top_mtrl_alpha, C0140R.drawable.abc_text_cursor_material};
    private static final int[] COLORFILTER_TINT_COLOR_CONTROL_NORMAL = new int[]{C0140R.drawable.abc_textfield_search_default_mtrl_alpha, C0140R.drawable.abc_textfield_default_mtrl_alpha, C0140R.drawable.abc_ab_share_pack_mtrl_alpha};
    private static final ColorFilterLruCache COLOR_FILTER_CACHE = new ColorFilterLruCache(6);
    private static final boolean DEBUG = false;
    private static final Mode DEFAULT_MODE = Mode.SRC_IN;
    private static AppCompatDrawableManager INSTANCE = null;
    private static final String TAG = "TintManager";
    private static final int[] TINT_CHECKABLE_BUTTON_LIST = new int[]{C0140R.drawable.abc_btn_check_material, C0140R.drawable.abc_btn_radio_material};
    private static final int[] TINT_COLOR_CONTROL_NORMAL = new int[]{C0140R.drawable.abc_ic_ab_back_mtrl_am_alpha, C0140R.drawable.abc_ic_go_search_api_mtrl_alpha, C0140R.drawable.abc_ic_search_api_mtrl_alpha, C0140R.drawable.abc_ic_commit_search_api_mtrl_alpha, C0140R.drawable.abc_ic_clear_mtrl_alpha, C0140R.drawable.abc_ic_menu_share_mtrl_alpha, C0140R.drawable.abc_ic_menu_copy_mtrl_am_alpha, C0140R.drawable.abc_ic_menu_cut_mtrl_alpha, C0140R.drawable.abc_ic_menu_selectall_mtrl_alpha, C0140R.drawable.abc_ic_menu_paste_mtrl_am_alpha, C0140R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha, C0140R.drawable.abc_ic_voice_search_api_mtrl_alpha};
    private static final int[] TINT_COLOR_CONTROL_STATE_LIST = new int[]{C0140R.drawable.abc_edit_text_material, C0140R.drawable.abc_tab_indicator_material, C0140R.drawable.abc_textfield_search_material, C0140R.drawable.abc_spinner_mtrl_am_alpha, C0140R.drawable.abc_spinner_textfield_background_material, C0140R.drawable.abc_ratingbar_full_material, C0140R.drawable.abc_switch_track_mtrl_alpha, C0140R.drawable.abc_switch_thumb_material, C0140R.drawable.abc_btn_default_mtrl_shape, C0140R.drawable.abc_btn_borderless_material};
    private ArrayList<InflateDelegate> mDelegates;
    private WeakHashMap<Context, SparseArray<ColorStateList>> mTintLists;

    public interface InflateDelegate {
        @Nullable
        Drawable onInflateDrawable(@NonNull Context context, @DrawableRes int i);
    }

    private static class ColorFilterLruCache extends LruCache<Integer, PorterDuffColorFilter> {
        public ColorFilterLruCache(int maxSize) {
            super(maxSize);
        }

        PorterDuffColorFilter get(int color, Mode mode) {
            return (PorterDuffColorFilter) get(Integer.valueOf(generateCacheKey(color, mode)));
        }

        PorterDuffColorFilter put(int color, Mode mode, PorterDuffColorFilter filter) {
            return (PorterDuffColorFilter) put(Integer.valueOf(generateCacheKey(color, mode)), filter);
        }

        private static int generateCacheKey(int color, Mode mode) {
            return ((color + 31) * 31) + mode.hashCode();
        }
    }

    public static AppCompatDrawableManager get() {
        if (INSTANCE == null) {
            INSTANCE = new AppCompatDrawableManager();
        }
        return INSTANCE;
    }

    public Drawable getDrawable(@NonNull Context context, @DrawableRes int resId) {
        return getDrawable(context, resId, false);
    }

    public Drawable getDrawable(@NonNull Context context, @DrawableRes int resId, boolean failIfNotKnown) {
        if (this.mDelegates != null) {
            int count = this.mDelegates.size();
            for (int i = 0; i < count; i++) {
                Drawable result = ((InflateDelegate) this.mDelegates.get(i)).onInflateDrawable(context, resId);
                if (result != null) {
                    return result;
                }
            }
        }
        Drawable drawable = ContextCompat.getDrawable(context, resId);
        if (drawable != null) {
            if (VERSION.SDK_INT >= 8) {
                drawable = drawable.mutate();
            }
            ColorStateList tintList = getTintList(context, resId);
            if (tintList != null) {
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTintList(drawable, tintList);
                Mode tintMode = getTintMode(resId);
                if (tintMode != null) {
                    DrawableCompat.setTintMode(drawable, tintMode);
                }
            } else if (resId == C0140R.drawable.abc_cab_background_top_material) {
                return new LayerDrawable(new Drawable[]{getDrawable(context, C0140R.drawable.abc_cab_background_internal_bg), getDrawable(context, C0140R.drawable.abc_cab_background_top_mtrl_alpha)});
            } else if (resId == C0140R.drawable.abc_seekbar_track_material) {
                LayerDrawable ld = (LayerDrawable) drawable;
                setPorterDuffColorFilter(ld.findDrawableByLayerId(16908288), ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlNormal), DEFAULT_MODE);
                setPorterDuffColorFilter(ld.findDrawableByLayerId(16908303), ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlNormal), DEFAULT_MODE);
                setPorterDuffColorFilter(ld.findDrawableByLayerId(16908301), ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlActivated), DEFAULT_MODE);
            } else if (!tintDrawableUsingColorFilter(context, resId, drawable) && failIfNotKnown) {
                drawable = null;
            }
        }
        return drawable;
    }

    public final boolean tintDrawableUsingColorFilter(@NonNull Context context, @DrawableRes int resId, @NonNull Drawable drawable) {
        Mode tintMode = DEFAULT_MODE;
        boolean colorAttrSet = false;
        int colorAttr = 0;
        int alpha = -1;
        if (arrayContains(COLORFILTER_TINT_COLOR_CONTROL_NORMAL, resId)) {
            colorAttr = C0140R.attr.colorControlNormal;
            colorAttrSet = true;
        } else if (arrayContains(COLORFILTER_COLOR_CONTROL_ACTIVATED, resId)) {
            colorAttr = C0140R.attr.colorControlActivated;
            colorAttrSet = true;
        } else if (arrayContains(COLORFILTER_COLOR_BACKGROUND_MULTIPLY, resId)) {
            colorAttr = 16842801;
            colorAttrSet = true;
            tintMode = Mode.MULTIPLY;
        } else if (resId == C0140R.drawable.abc_list_divider_mtrl_alpha) {
            colorAttr = 16842800;
            colorAttrSet = true;
            alpha = Math.round(40.8f);
        }
        if (!colorAttrSet) {
            return false;
        }
        drawable.setColorFilter(getPorterDuffColorFilter(ThemeUtils.getThemeAttrColor(context, colorAttr), tintMode));
        if (alpha != -1) {
            drawable.setAlpha(alpha);
        }
        return true;
    }

    public void addDelegate(@NonNull InflateDelegate delegate) {
        if (this.mDelegates == null) {
            this.mDelegates = new ArrayList();
        }
        if (!this.mDelegates.contains(delegate)) {
            this.mDelegates.add(delegate);
        }
    }

    public void removeDelegate(@NonNull InflateDelegate delegate) {
        if (this.mDelegates != null) {
            this.mDelegates.remove(delegate);
        }
    }

    private static boolean arrayContains(int[] array, int value) {
        for (int id : array) {
            if (id == value) {
                return true;
            }
        }
        return false;
    }

    final Mode getTintMode(int resId) {
        if (resId == C0140R.drawable.abc_switch_thumb_material) {
            return Mode.MULTIPLY;
        }
        return null;
    }

    public final ColorStateList getTintList(@NonNull Context context, @DrawableRes int resId) {
        ColorStateList tint = getTintListFromCache(context, resId);
        if (tint == null) {
            if (resId == C0140R.drawable.abc_edit_text_material) {
                tint = createEditTextColorStateList(context);
            } else if (resId == C0140R.drawable.abc_switch_track_mtrl_alpha) {
                tint = createSwitchTrackColorStateList(context);
            } else if (resId == C0140R.drawable.abc_switch_thumb_material) {
                tint = createSwitchThumbColorStateList(context);
            } else if (resId == C0140R.drawable.abc_btn_default_mtrl_shape || resId == C0140R.drawable.abc_btn_borderless_material) {
                tint = createDefaultButtonColorStateList(context);
            } else if (resId == C0140R.drawable.abc_btn_colored_material) {
                tint = createColoredButtonColorStateList(context);
            } else if (resId == C0140R.drawable.abc_spinner_mtrl_am_alpha || resId == C0140R.drawable.abc_spinner_textfield_background_material) {
                tint = createSpinnerColorStateList(context);
            } else if (arrayContains(TINT_COLOR_CONTROL_NORMAL, resId)) {
                tint = ThemeUtils.getThemeAttrColorStateList(context, C0140R.attr.colorControlNormal);
            } else if (arrayContains(TINT_COLOR_CONTROL_STATE_LIST, resId)) {
                tint = createDefaultColorStateList(context);
            } else if (arrayContains(TINT_CHECKABLE_BUTTON_LIST, resId)) {
                tint = createCheckableButtonColorStateList(context);
            } else if (resId == C0140R.drawable.abc_seekbar_thumb_material) {
                tint = createSeekbarThumbColorStateList(context);
            }
            if (tint != null) {
                addTintListToCache(context, resId, tint);
            }
        }
        return tint;
    }

    private ColorStateList getTintListFromCache(@NonNull Context context, @DrawableRes int resId) {
        if (this.mTintLists == null) {
            return null;
        }
        SparseArray<ColorStateList> tints = (SparseArray) this.mTintLists.get(context);
        if (tints != null) {
            return (ColorStateList) tints.get(resId);
        }
        return null;
    }

    private void addTintListToCache(@NonNull Context context, @DrawableRes int resId, @NonNull ColorStateList tintList) {
        if (this.mTintLists == null) {
            this.mTintLists = new WeakHashMap();
        }
        SparseArray<ColorStateList> themeTints = (SparseArray) this.mTintLists.get(context);
        if (themeTints == null) {
            themeTints = new SparseArray();
            this.mTintLists.put(context, themeTints);
        }
        themeTints.append(resId, tintList);
    }

    private ColorStateList createDefaultColorStateList(Context context) {
        int colorControlNormal = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlNormal);
        int colorControlActivated = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlActivated);
        int[][] states = new int[7][];
        colors = new int[7];
        int i = 0 + 1;
        states[i] = ThemeUtils.FOCUSED_STATE_SET;
        colors[i] = colorControlActivated;
        i++;
        states[i] = ThemeUtils.ACTIVATED_STATE_SET;
        colors[i] = colorControlActivated;
        i++;
        states[i] = ThemeUtils.PRESSED_STATE_SET;
        colors[i] = colorControlActivated;
        i++;
        states[i] = ThemeUtils.CHECKED_STATE_SET;
        colors[i] = colorControlActivated;
        i++;
        states[i] = ThemeUtils.SELECTED_STATE_SET;
        colors[i] = colorControlActivated;
        i++;
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = colorControlNormal;
        i++;
        return new ColorStateList(states, colors);
    }

    private ColorStateList createCheckableButtonColorStateList(Context context) {
        states = new int[3][];
        int[] colors = new int[3];
        states[0] = ThemeUtils.DISABLED_STATE_SET;
        colors[0] = ThemeUtils.getDisabledThemeAttrColor(context, C0140R.attr.colorControlNormal);
        int i = 0 + 1;
        states[i] = ThemeUtils.CHECKED_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlActivated);
        i++;
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlNormal);
        i++;
        return new ColorStateList(states, colors);
    }

    private ColorStateList createSwitchTrackColorStateList(Context context) {
        states = new int[3][];
        int[] colors = new int[3];
        states[0] = ThemeUtils.DISABLED_STATE_SET;
        colors[0] = ThemeUtils.getThemeAttrColor(context, 16842800, 0.1f);
        int i = 0 + 1;
        states[i] = ThemeUtils.CHECKED_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlActivated, 0.3f);
        i++;
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, 16842800, 0.3f);
        i++;
        return new ColorStateList(states, colors);
    }

    private ColorStateList createSwitchThumbColorStateList(Context context) {
        int[][] states = new int[3][];
        int[] colors = new int[3];
        ColorStateList thumbColor = ThemeUtils.getThemeAttrColorStateList(context, C0140R.attr.colorSwitchThumbNormal);
        int i;
        if (thumbColor == null || !thumbColor.isStateful()) {
            states[0] = ThemeUtils.DISABLED_STATE_SET;
            colors[0] = ThemeUtils.getDisabledThemeAttrColor(context, C0140R.attr.colorSwitchThumbNormal);
            i = 0 + 1;
            states[i] = ThemeUtils.CHECKED_STATE_SET;
            colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlActivated);
            i++;
            states[i] = ThemeUtils.EMPTY_STATE_SET;
            colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorSwitchThumbNormal);
            i++;
        } else {
            states[0] = ThemeUtils.DISABLED_STATE_SET;
            colors[0] = thumbColor.getColorForState(states[0], 0);
            i = 0 + 1;
            states[i] = ThemeUtils.CHECKED_STATE_SET;
            colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlActivated);
            i++;
            states[i] = ThemeUtils.EMPTY_STATE_SET;
            colors[i] = thumbColor.getDefaultColor();
            i++;
        }
        return new ColorStateList(states, colors);
    }

    private ColorStateList createEditTextColorStateList(Context context) {
        states = new int[3][];
        int[] colors = new int[3];
        states[0] = ThemeUtils.DISABLED_STATE_SET;
        colors[0] = ThemeUtils.getDisabledThemeAttrColor(context, C0140R.attr.colorControlNormal);
        int i = 0 + 1;
        states[i] = ThemeUtils.NOT_PRESSED_OR_FOCUSED_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlNormal);
        i++;
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlActivated);
        i++;
        return new ColorStateList(states, colors);
    }

    private ColorStateList createDefaultButtonColorStateList(Context context) {
        return createButtonColorStateList(context, C0140R.attr.colorButtonNormal);
    }

    private ColorStateList createColoredButtonColorStateList(Context context) {
        return createButtonColorStateList(context, C0140R.attr.colorAccent);
    }

    private ColorStateList createButtonColorStateList(Context context, int baseColorAttr) {
        states = new int[4][];
        colors = new int[4];
        int baseColor = ThemeUtils.getThemeAttrColor(context, baseColorAttr);
        int colorControlHighlight = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlHighlight);
        states[0] = ThemeUtils.DISABLED_STATE_SET;
        colors[0] = ThemeUtils.getDisabledThemeAttrColor(context, C0140R.attr.colorButtonNormal);
        int i = 0 + 1;
        states[i] = ThemeUtils.PRESSED_STATE_SET;
        colors[i] = ColorUtils.compositeColors(colorControlHighlight, baseColor);
        i++;
        states[i] = ThemeUtils.FOCUSED_STATE_SET;
        colors[i] = ColorUtils.compositeColors(colorControlHighlight, baseColor);
        i++;
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = baseColor;
        i++;
        return new ColorStateList(states, colors);
    }

    private ColorStateList createSpinnerColorStateList(Context context) {
        states = new int[3][];
        int[] colors = new int[3];
        states[0] = ThemeUtils.DISABLED_STATE_SET;
        colors[0] = ThemeUtils.getDisabledThemeAttrColor(context, C0140R.attr.colorControlNormal);
        int i = 0 + 1;
        states[i] = ThemeUtils.NOT_PRESSED_OR_FOCUSED_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlNormal);
        i++;
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlActivated);
        i++;
        return new ColorStateList(states, colors);
    }

    private ColorStateList createSeekbarThumbColorStateList(Context context) {
        states = new int[2][];
        int[] colors = new int[]{ThemeUtils.DISABLED_STATE_SET, ThemeUtils.getDisabledThemeAttrColor(context, C0140R.attr.colorControlActivated)};
        int i = 0 + 1;
        states[i] = ThemeUtils.EMPTY_STATE_SET;
        colors[i] = ThemeUtils.getThemeAttrColor(context, C0140R.attr.colorControlActivated);
        i++;
        return new ColorStateList(states, colors);
    }

    public static void tintDrawable(Drawable drawable, TintInfo tint, int[] state) {
        if (!shouldMutateBackground(drawable) || drawable.mutate() == drawable) {
            if (tint.mHasTintList || tint.mHasTintMode) {
                drawable.setColorFilter(createTintFilter(tint.mHasTintList ? tint.mTintList : null, tint.mHasTintMode ? tint.mTintMode : DEFAULT_MODE, state));
            } else {
                drawable.clearColorFilter();
            }
            if (VERSION.SDK_INT <= 10) {
                drawable.invalidateSelf();
                return;
            }
            return;
        }
        Log.d(TAG, "Mutated drawable is not the same instance as the input.");
    }

    private static boolean shouldMutateBackground(Drawable drawable) {
        if (VERSION.SDK_INT >= 16) {
            return true;
        }
        if (drawable instanceof LayerDrawable) {
            if (VERSION.SDK_INT < 16) {
                return false;
            }
            return true;
        } else if (drawable instanceof InsetDrawable) {
            if (VERSION.SDK_INT < 14) {
                return false;
            }
            return true;
        } else if (!(drawable instanceof DrawableContainer)) {
            return true;
        } else {
            ConstantState state = drawable.getConstantState();
            if (!(state instanceof DrawableContainerState)) {
                return true;
            }
            for (Drawable child : ((DrawableContainerState) state).getChildren()) {
                if (!shouldMutateBackground(child)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static PorterDuffColorFilter createTintFilter(ColorStateList tint, Mode tintMode, int[] state) {
        if (tint == null || tintMode == null) {
            return null;
        }
        return getPorterDuffColorFilter(tint.getColorForState(state, 0), tintMode);
    }

    private static PorterDuffColorFilter getPorterDuffColorFilter(int color, Mode mode) {
        PorterDuffColorFilter filter = COLOR_FILTER_CACHE.get(color, mode);
        if (filter != null) {
            return filter;
        }
        filter = new PorterDuffColorFilter(color, mode);
        COLOR_FILTER_CACHE.put(color, mode, filter);
        return filter;
    }

    private static void setPorterDuffColorFilter(Drawable d, int color, Mode mode) {
        if (mode == null) {
            mode = DEFAULT_MODE;
        }
        d.setColorFilter(getPorterDuffColorFilter(color, mode));
    }
}
