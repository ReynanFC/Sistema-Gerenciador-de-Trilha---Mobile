package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.databinding.ActivitySettingsBinding;
import com.reynanfc.sistema_gerenciador_trilha.databinding.ComponentOptionCardBinding;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs.MapType;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs.NavigationMode;

public class SettingsActivity extends AppCompatActivity {

    private static final String STATE_MAP_TYPE = "state_map_type";
    private static final String STATE_NAVIGATION_MODE = "state_navigation_mode";

    private SettingsPrefs settingsPrefs;

    private MapType selectedMapType;
    private NavigationMode selectedNavigationMode;

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settingsPrefs = new SettingsPrefs(this);
        selectedMapType = settingsPrefs.getMapType();
        selectedNavigationMode = settingsPrefs.getNavigationMode();
        // Depois de uma rotação, mantém o que foi escolhido mas ainda não salvo
        if (savedInstanceState != null) {
            selectedMapType = MapType.valueOf(
                    savedInstanceState.getString(STATE_MAP_TYPE, selectedMapType.name()));
            selectedNavigationMode = NavigationMode.valueOf(
                    savedInstanceState.getString(STATE_NAVIGATION_MODE, selectedNavigationMode.name()));
        }

        setupToolbar();
        setupOptionCards();
        setupSaveButton();
        renderSelection();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_MAP_TYPE, selectedMapType.name());
        outState.putString(STATE_NAVIGATION_MODE, selectedNavigationMode.name());
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupOptionCards() {
        bindOption(
                binding.optionMapVector,
                R.drawable.thumb_map_vector,
                R.string.settings_map_vector_title,
                R.string.settings_map_vector_description,
                true
        );
        bindOption(
                binding.optionMapSatellite,
                R.drawable.thumb_map_satellite,
                R.string.settings_map_satellite_title,
                R.string.settings_map_satellite_description,
                false
        );
        bindOption(
                binding.optionNorthUp,
                R.drawable.thumb_north_up,
                R.string.settings_north_up,
                R.string.settings_north_up_description,
                true
        );
        bindOption(
                binding.optionCourseUp,
                R.drawable.thumb_course_up,
                R.string.settings_course_up,
                R.string.settings_course_up_description,
                false
        );

        binding.optionMapVector.getRoot().setOnClickListener(v -> selectMapType(MapType.VECTOR));
        binding.optionMapSatellite.getRoot().setOnClickListener(v -> selectMapType(MapType.SATELLITE));
        binding.optionNorthUp.getRoot().setOnClickListener(v -> selectNavigationMode(NavigationMode.NORTH_UP));
        binding.optionCourseUp.getRoot().setOnClickListener(v -> selectNavigationMode(NavigationMode.COURSE_UP));
    }

    private void bindOption(ComponentOptionCardBinding option,
                            @DrawableRes int thumbnailResId,
                            @StringRes int titleResId,
                            @StringRes int descriptionResId,
                            boolean isDefault) {
        option.optionThumbnail.setImageResource(thumbnailResId);
        option.optionTitle.setText(titleResId);
        option.optionDescription.setText(descriptionResId);
        option.optionBadge.setVisibility(isDefault ? View.VISIBLE : View.GONE);
    }

    private void selectMapType(MapType mapType) {
        selectedMapType = mapType;
        renderSelection();
    }

    private void selectNavigationMode(NavigationMode navigationMode) {
        selectedNavigationMode = navigationMode;
        renderSelection();
    }

    private void renderSelection() {
        setOptionChecked(binding.optionMapVector, selectedMapType == MapType.VECTOR);
        setOptionChecked(binding.optionMapSatellite, selectedMapType == MapType.SATELLITE);
        setOptionChecked(binding.optionNorthUp, selectedNavigationMode == NavigationMode.NORTH_UP);
        setOptionChecked(binding.optionCourseUp, selectedNavigationMode == NavigationMode.COURSE_UP);
    }

    private void setOptionChecked(ComponentOptionCardBinding option, boolean checked) {
        option.getRoot().setChecked(checked);
        option.optionRadio.setChecked(checked);
    }

    private void setupSaveButton() {
        binding.buttonSave.setOnClickListener(v -> {
            settingsPrefs.setMapType(selectedMapType);
            settingsPrefs.setNavigationMode(selectedNavigationMode);
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
