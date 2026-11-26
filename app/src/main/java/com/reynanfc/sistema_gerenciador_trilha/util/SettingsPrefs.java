package com.reynanfc.sistema_gerenciador_trilha.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StringRes;

import com.reynanfc.sistema_gerenciador_trilha.R;

public class SettingsPrefs {

    public enum MapType {
        VECTOR(R.string.settings_map_vector),
        SATELLITE(R.string.settings_map_satellite);

        @StringRes
        public final int labelResId;

        MapType(@StringRes int labelResId) {
            this.labelResId = labelResId;
        }
    }

    public enum NavigationMode {
        NORTH_UP(R.string.settings_north_up),
        COURSE_UP(R.string.settings_course_up);

        @StringRes
        public final int labelResId;

        NavigationMode(@StringRes int labelResId) {
            this.labelResId = labelResId;
        }
    }

    private static final String PREFS_NAME = "settings";
    private static final String KEY_MAP_TYPE = "map_type";
    private static final String KEY_NAVIGATION_MODE = "navigation_mode";

    private static final MapType DEFAULT_MAP_TYPE = MapType.VECTOR;
    private static final NavigationMode DEFAULT_NAVIGATION_MODE = NavigationMode.NORTH_UP;

    private final SharedPreferences prefs;

    public SettingsPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public MapType getMapType() {
        return readEnum(KEY_MAP_TYPE, MapType.class, DEFAULT_MAP_TYPE);
    }

    public void setMapType(MapType mapType) {
        prefs.edit().putString(KEY_MAP_TYPE, mapType.name()).apply();
    }

    public NavigationMode getNavigationMode() {
        return readEnum(KEY_NAVIGATION_MODE, NavigationMode.class, DEFAULT_NAVIGATION_MODE);
    }

    public void setNavigationMode(NavigationMode navigationMode) {
        prefs.edit().putString(KEY_NAVIGATION_MODE, navigationMode.name()).apply();
    }

    private <E extends Enum<E>> E readEnum(String key, Class<E> type, E defaultValue) {
        String stored = prefs.getString(key, null);
        if (stored == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(type, stored);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}
