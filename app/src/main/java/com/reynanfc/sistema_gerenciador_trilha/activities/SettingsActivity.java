package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs.MapType;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs.NavigationMode;

public class SettingsActivity extends AppCompatActivity {

    private SettingsPrefs settingsPrefs;

    private MapType selectedMapType;
    private NavigationMode selectedNavigationMode;

    private MaterialCardView cardMapVector;
    private MaterialCardView cardMapSatellite;
    private MaterialCardView cardNorthUp;
    private MaterialCardView cardCourseUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settingsPrefs = new SettingsPrefs(this);
        selectedMapType = settingsPrefs.getMapType();
        selectedNavigationMode = settingsPrefs.getNavigationMode();

        setupToolbar();
        setupOptionCards();
        setupSaveButton();
        renderSelection();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupOptionCards() {
        cardMapVector = bindOption(
                R.id.option_map_vector,
                R.drawable.thumb_map_vector,
                R.string.settings_map_vector_title,
                R.string.settings_map_vector_description,
                true
        );
        cardMapSatellite = bindOption(
                R.id.option_map_satellite,
                R.drawable.thumb_map_satellite,
                R.string.settings_map_satellite_title,
                R.string.settings_map_satellite_description,
                false
        );
        cardNorthUp = bindOption(
                R.id.option_north_up,
                R.drawable.thumb_north_up,
                R.string.settings_north_up,
                R.string.settings_north_up_description,
                true
        );
        cardCourseUp = bindOption(
                R.id.option_course_up,
                R.drawable.thumb_course_up,
                R.string.settings_course_up,
                R.string.settings_course_up_description,
                false
        );

        cardMapVector.setOnClickListener(v -> selectMapType(MapType.VECTOR));
        cardMapSatellite.setOnClickListener(v -> selectMapType(MapType.SATELLITE));
        cardNorthUp.setOnClickListener(v -> selectNavigationMode(NavigationMode.NORTH_UP));
        cardCourseUp.setOnClickListener(v -> selectNavigationMode(NavigationMode.COURSE_UP));
    }

    private MaterialCardView bindOption(@IdRes int cardId,
                                        @DrawableRes int thumbnailResId,
                                        @StringRes int titleResId,
                                        @StringRes int descriptionResId,
                                        boolean isDefault) {
        MaterialCardView card = findViewById(cardId);

        ImageView ivThumbnail = card.findViewById(R.id.option_thumbnail);
        TextView tvTitle = card.findViewById(R.id.option_title);
        TextView tvDescription = card.findViewById(R.id.option_description);
        TextView tvBadge = card.findViewById(R.id.option_badge);

        ivThumbnail.setImageResource(thumbnailResId);
        tvTitle.setText(titleResId);
        tvDescription.setText(descriptionResId);
        tvBadge.setVisibility(isDefault ? View.VISIBLE : View.GONE);

        return card;
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
        setOptionChecked(cardMapVector, selectedMapType == MapType.VECTOR);
        setOptionChecked(cardMapSatellite, selectedMapType == MapType.SATELLITE);
        setOptionChecked(cardNorthUp, selectedNavigationMode == NavigationMode.NORTH_UP);
        setOptionChecked(cardCourseUp, selectedNavigationMode == NavigationMode.COURSE_UP);
    }

    private void setOptionChecked(MaterialCardView card, boolean checked) {
        card.setChecked(checked);
        MaterialRadioButton radio = card.findViewById(R.id.option_radio);
        radio.setChecked(checked);
    }

    private void setupSaveButton() {
        findViewById(R.id.button_save).setOnClickListener(v -> {
            settingsPrefs.setMapType(selectedMapType);
            settingsPrefs.setNavigationMode(selectedNavigationMode);
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
