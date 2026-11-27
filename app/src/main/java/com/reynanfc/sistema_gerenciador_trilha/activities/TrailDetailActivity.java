package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.reynanfc.sistema_gerenciador_trilha.R;

public class TrailDetailActivity extends AppCompatActivity {

    /** Id of the trail to show (long). Only the id travels in the Intent; the trail is loaded from the database */
    public static final String EXTRA_TRAIL_ID = "com.reynanfc.sistema_gerenciador_trilha.EXTRA_TRAIL_ID";

    private static final long NO_ID = -1;

    private long trailId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trailId = getIntent().getLongExtra(EXTRA_TRAIL_ID, NO_ID);
        if (trailId == NO_ID) {
            closeWithError();
            return;
        }
        setContentView(R.layout.activity_trail_detail);
    }

    private void closeWithError() {
        Toast.makeText(this, R.string.detail_not_found, Toast.LENGTH_LONG).show();
        finish();
    }
}
