package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.databinding.ActivityMainBinding;
import com.reynanfc.sistema_gerenciador_trilha.databinding.ComponentMenuCardBinding;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowInsets();
        setupMenuCards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCurrentSettings();
    }

    private void updateCurrentSettings() {
        SettingsPrefs prefs = new SettingsPrefs(this);

        binding.currentSettings.chipMapType.setText(prefs.getMapType().labelResId);
        binding.currentSettings.chipNavigation.setText(prefs.getNavigationMode().labelResId);
    }

    private void setupWindowInsets() {
        View rootLayout = binding.getRoot();
        View header = binding.header.getRoot();

        int initialHeaderLeft = header.getPaddingLeft();
        int initialHeaderTop = header.getPaddingTop();
        int initialHeaderRight = header.getPaddingRight();
        int initialHeaderBottom = header.getPaddingBottom();

        int initialRootLeft = rootLayout.getPaddingLeft();
        int initialRootTop = rootLayout.getPaddingTop();
        int initialRootRight = rootLayout.getPaddingRight();
        int initialRootBottom = rootLayout.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            header.setPadding(
                    initialHeaderLeft,
                    initialHeaderTop + systemBars.top,
                    initialHeaderRight,
                    initialHeaderBottom
            );

            rootLayout.setPadding(
                    initialRootLeft + systemBars.left,
                    initialRootTop,
                    initialRootRight + systemBars.right,
                    initialRootBottom + systemBars.bottom
            );

            return insets;
        });
    }

    private void setupMenuCards() {
        setupCard(
                binding.cardRecord,
                R.drawable.ic_record,
                R.string.menu_record_title,
                R.string.menu_record_subtitle,
                RecordTrailActivity.class
        );

        setupCard(
                binding.cardTrails,
                R.drawable.ic_list,
                R.string.menu_trails_title,
                R.string.menu_trails_subtitle,
                TrailListActivity.class
        );

        setupCard(
                binding.cardSettings,
                R.drawable.ic_settings,
                R.string.menu_settings_title,
                R.string.menu_settings_subtitle,
                SettingsActivity.class
        );
    }

    private void setupCard(ComponentMenuCardBinding card,
                           @DrawableRes int iconResId,
                           @StringRes int titleResId,
                           @StringRes int subtitleResId,
                           Class<? extends Activity> destinationActivity) {
        card.menuCardIcon.setImageResource(iconResId);
        card.menuCardTitle.setText(titleResId);
        card.menuCardSubtitle.setText(subtitleResId);

        card.getRoot().setOnClickListener(v -> {
            Intent intent = new Intent(this, destinationActivity);
            startActivity(intent);
        });
    }
}