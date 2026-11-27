package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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

        View card = findViewById(R.id.current_settings);
        Chip chipMapType = card.findViewById(R.id.chip_map_type);
        Chip chipNavigation = card.findViewById(R.id.chip_navigation);

        chipMapType.setText(prefs.getMapType().labelResId);
        chipNavigation.setText(prefs.getNavigationMode().labelResId);
    }

    private void setupWindowInsets() {
        View rootLayout = findViewById(R.id.main);
        View header = findViewById(R.id.header);

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
                R.id.card_record,
                R.drawable.ic_record,
                R.string.menu_record_title,
                R.string.menu_record_subtitle,
                RecordTrailActivity.class
        );

        setupCard(
                R.id.card_trails,
                R.drawable.ic_list,
                R.string.menu_trails_title,
                R.string.menu_trails_subtitle,
                TrailListActivity.class
        );

        setupCard(
                R.id.card_settings,
                R.drawable.ic_settings,
                R.string.menu_settings_title,
                R.string.menu_settings_subtitle,
                SettingsActivity.class
        );
    }

    private void setupCard(@IdRes int cardId,
                           @DrawableRes int iconResId,
                           @StringRes int titleResId,
                           @StringRes int subtitleResId,
                           Class<? extends Activity> destinationActivity) {
        View cardView = findViewById(cardId);

        ImageView ivIcon = cardView.findViewById(R.id.menu_card_icon);
        TextView tvTitle = cardView.findViewById(R.id.menu_card_title);
        TextView tvSubtitle = cardView.findViewById(R.id.menu_card_subtitle);

        ivIcon.setImageResource(iconResId);
        tvTitle.setText(titleResId);
        tvSubtitle.setText(subtitleResId);

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(this, destinationActivity);
            startActivity(intent);
        });
    }
}