package com.reynanfc.sistema_gerenciador_trilha.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.reynanfc.sistema_gerenciador_trilha.R;
import com.reynanfc.sistema_gerenciador_trilha.database.TrailDao;
import com.reynanfc.sistema_gerenciador_trilha.databinding.ActivityRecordTrailBinding;
import com.reynanfc.sistema_gerenciador_trilha.databinding.BottomSheetFinishTrailBinding;
import com.reynanfc.sistema_gerenciador_trilha.location.LocationTracker;
import com.reynanfc.sistema_gerenciador_trilha.location.TrailRecorder;
import com.reynanfc.sistema_gerenciador_trilha.model.Trail;
import com.reynanfc.sistema_gerenciador_trilha.util.Async;
import com.reynanfc.sistema_gerenciador_trilha.util.Formatters;
import com.reynanfc.sistema_gerenciador_trilha.util.MapIcons;
import com.reynanfc.sistema_gerenciador_trilha.util.SettingsPrefs;

import java.util.ArrayList;

/**
 * Tela Registrar Trilha: mostra o mapa, pede a permissão de localização, acompanha o GPS e
 * repassa cada posição para o {@link TrailRecorder} (tempo, velocidades, distância, pontos). Ao finalizar,
 * abre o resumo e salva a trilha com seus pontos pelo {@link TrailDao}.
 * Fluxo e diagramas: docs/registrar-trilha.md
 */
public class RecordTrailActivity extends AppCompatActivity {

    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final float INITIAL_ZOOM = 17f;
    private static final long TIMER_TICK_MILLIS = 500;
    /** m/s (~1,8 km/h): abaixo disso o rumo do GPS é ruído */
    private static final float MIN_SPEED_FOR_BEARING = 0.5f;

    private ActivityRecordTrailBinding binding;
    private SettingsPrefs settingsPrefs;
    @Nullable
    private GoogleMap googleMap;
    private LocationTracker locationTracker;
    private boolean hasLocationPermission;
    private boolean cameraCentered;
    @Nullable
    private Marker positionMarker;
    @Nullable
    private Circle accuracyCircle;
    private TrailRecorder recorder;

    // Sair depois que a gravação começou perderia a trilha, então pergunta antes
    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (recorder.isStarted()) {
                confirmDiscard();
            } else {
                finish();
            }
        }
    };

    // Atualiza o texto do cronômetro enquanto a tela está visível (iniciado no onResume, removido no onPause)
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            binding.textTimer.setText(Formatters.duration(recorder.getElapsedMillis()));
            timerHandler.postDelayed(this, TIMER_TICK_MILLIS);
        }
    };

    // Precisa ser registrado antes de a Activity iniciar, por isso é um campo e não é criado na hora
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> onLocationPermissionResult());

    private final ActivityResultLauncher<IntentSenderRequest> locationSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    onDeviceLocationOff();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRecordTrailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settingsPrefs = new SettingsPrefs(this);
        recorder = new ViewModelProvider(this).get(RecordTrailViewModel.class).recorder;
        locationTracker = new LocationTracker(this, this::onLocation);

        setupWindowInsets();
        binding.buttonBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getOnBackPressedDispatcher().addCallback(this, backCallback);
        binding.buttonFinish.setOnClickListener(v -> onFinishClicked());
        showCurrentSettings();
        binding.buttonPause.setOnClickListener(v -> togglePause());
        binding.buttonCompass.setOnClickListener(v -> resetBearing());
        renderRecordingState();
        renderStats();
        setupMap();
        checkLocationPermission();
    }

    // O GPS só roda com a tela visível; o resultado da permissão chega antes do onResume
    @Override
    protected void onResume() {
        super.onResume();
        if (hasLocationPermission) {
            locationTracker.start();
        }
        timerHandler.post(timerTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationTracker.stop();
        timerHandler.removeCallbacks(timerTick);
    }

    private void onFinishClicked() {
        if (recorder.getPoints().isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.finish_no_points_title)
                    .setMessage(R.string.finish_no_points_message)
                    .setNegativeButton(R.string.finish_discard, (d, which) -> finish())
                    .setPositiveButton(R.string.finish_keep_recording, null)
                    .show();
            return;
        }

        // Congela os números enquanto o usuário digita o nome; fechar o sheet retoma a gravação
        boolean wasRunning = recorder.isRunning();
        recorder.pause();
        renderRecordingState();
        showFinishSheet(wasRunning, System.currentTimeMillis());
    }

    private void showFinishSheet(boolean wasRunning, long endTime) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        BottomSheetFinishTrailBinding sheetBinding = BottomSheetFinishTrailBinding.inflate(getLayoutInflater());
        sheet.setContentView(sheetBinding.getRoot());
        sheet.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        sheet.getBehavior().setSkipCollapsed(true);

        long elapsed = recorder.getElapsedMillis();
        double averageSpeed = averageSpeed(recorder.getDistance(), elapsed);

        sheetBinding.textFinishStart.setText(Formatters.dateTime(recorder.getStartTime()));
        sheetBinding.textFinishEnd.setText(Formatters.dateTime(endTime));
        // Duração = fim - início (o que a lista e a visualização mostram); o cronômetro mostra só o tempo em movimento
        sheetBinding.textFinishDuration.setText(Formatters.duration(endTime - recorder.getStartTime()));
        sheetBinding.textFinishDistance.setText(
                getString(R.string.value_km, Formatters.distanceKm(recorder.getDistance())));
        sheetBinding.textFinishAverageSpeed.setText(
                getString(R.string.value_kmh, Formatters.speedKmh(averageSpeed)));
        sheetBinding.textFinishMaxSpeed.setText(
                getString(R.string.value_kmh, Formatters.speedKmh(recorder.getMaxSpeed())));

        sheet.setOnCancelListener(d -> {
            if (wasRunning) {
                recorder.resume();
                renderRecordingState();
            }
        });
        sheetBinding.buttonDiscard.setOnClickListener(v -> confirmDiscard());
        sheetBinding.buttonSaveTrail.setOnClickListener(v -> {
            Editable text = sheetBinding.editTrailName.getText();
            String name = text == null ? "" : text.toString().trim();
            if (name.isEmpty()) {
                sheetBinding.layoutTrailName.setError(getString(R.string.finish_name_required));
                return;
            }
            sheetBinding.layoutTrailName.setError(null);
            sheetBinding.buttonSaveTrail.setEnabled(false);
            saveTrail(buildTrail(name, endTime, averageSpeed), sheet, sheetBinding.buttonSaveTrail);
        });

        sheet.show();
    }

    /** Distância dividida pelo tempo em movimento (sem as pausas), em m/s */
    private static double averageSpeed(double distanceMeters, long elapsedMillis) {
        return elapsedMillis > 0 ? distanceMeters / (elapsedMillis / 1000.0) : 0;
    }

    private Trail buildTrail(String name, long endTime, double averageSpeed) {
        Trail trail = new Trail(name, recorder.getStartTime(), endTime,
                averageSpeed, recorder.getMaxSpeed(), recorder.getDistance());
        trail.setPoints(new ArrayList<>(recorder.getPoints()));
        return trail;
    }

    // A inserção de centenas de pontos roda numa thread de fundo para a interface nunca travar
    private void saveTrail(Trail trail, BottomSheetDialog sheet, MaterialButton buttonSave) {
        TrailDao trailDao = new TrailDao(this);
        Async.run(() -> trailDao.insert(trail),
                trailId -> {
                    Toast.makeText(this, R.string.finish_saved, Toast.LENGTH_SHORT).show();
                    sheet.dismiss();
                    finish();
                },
                error -> {
                    buttonSave.setEnabled(true);
                    Toast.makeText(this, R.string.finish_save_error, Toast.LENGTH_LONG).show();
                });
    }

    private void confirmDiscard() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.finish_discard_title)
                .setMessage(R.string.finish_discard_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.finish_discard, (d, which) -> finish())
                .show();
    }

    private void togglePause() {
        if (recorder.isRunning()) {
            recorder.pause();
        } else {
            recorder.resume();
        }
        renderRecordingState();
    }

    // Selo de status e botão Pausar/Retomar; antes da primeira posição do GPS não há o que pausar
    private void renderRecordingState() {
        if (!recorder.isStarted()) {
            setRecordingStatus(R.string.record_waiting_gps, R.drawable.dot_waiting);
        } else if (recorder.isRunning()) {
            setRecordingStatus(R.string.record_recording, R.drawable.dot_recording);
        } else {
            setRecordingStatus(R.string.record_paused, R.drawable.dot_waiting);
        }

        binding.buttonPause.setEnabled(recorder.isStarted());
        boolean paused = recorder.isStarted() && !recorder.isRunning();
        binding.buttonPause.setText(paused ? R.string.record_resume : R.string.record_pause);
        binding.buttonPause.setIconResource(paused ? R.drawable.ic_play : R.drawable.ic_pause);
    }

    private void renderStats() {
        binding.textSpeed.setText(Formatters.speedKmh(recorder.getCurrentSpeed()));
        binding.textMaxSpeed.setText(Formatters.speedKmh(recorder.getMaxSpeed()));
        binding.textDistance.setText(Formatters.distanceKm(recorder.getDistance()));
    }

    private void setRecordingStatus(@StringRes int textResId, @DrawableRes int dotResId) {
        binding.textRecordingStatus.setText(textResId);
        binding.textRecordingStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(dotResId, 0, 0, 0);
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkLocationPermission() {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            onLocationPermissionGranted();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionRationale();
        } else {
            locationPermissionLauncher.launch(LOCATION_PERMISSIONS);
        }
    }

    // FINE e COARSE são pedidas juntas: desde o Android 12 o usuário pode conceder só a aproximada
    private void onLocationPermissionResult() {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            onLocationPermissionGranted();
        } else if (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Toast.makeText(this, R.string.permission_location_approximate, Toast.LENGTH_LONG).show();
            onLocationPermissionGranted();
        } else {
            onLocationPermissionDenied();
        }
    }

    private void onLocationPermissionGranted() {
        hasLocationPermission = true;
        binding.chipAccuracy.setText(R.string.record_accuracy_waiting);
        checkDeviceLocation();
    }

    // Se a localização do aparelho estiver desligada, o Play Services mostra um diálogo que liga com um toque
    private void checkDeviceLocation() {
        locationTracker.checkLocationSettings().addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                PendingIntent resolution = ((ResolvableApiException) e).getResolution();
                locationSettingsLauncher.launch(new IntentSenderRequest.Builder(resolution).build());
            } else {
                onDeviceLocationOff();
            }
        });
    }

    private void onDeviceLocationOff() {
        binding.chipAccuracy.setText(R.string.record_accuracy_gps_off);
        Toast.makeText(this, R.string.location_disabled_message, Toast.LENGTH_LONG).show();
    }

    private void onLocation(Location location) {
        // A gravação começa na primeira posição, para a hora de início e o primeiro ponto coincidirem
        if (!recorder.isStarted()) {
            recorder.start();
            renderRecordingState();
        }
        recorder.addLocation(location);
        renderStats();

        binding.chipAccuracy.setText(getString(R.string.record_accuracy, Math.round(location.getAccuracy())));

        if (googleMap == null) {
            return;
        }
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        updatePositionMarker(position, location);
        updateAccuracyCircle(position, location.getAccuracy());

        if (cameraCentered) {
            followCamera(position, location);
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, INITIAL_ZOOM));
            cameraCentered = true;
        }
    }

    /**
     * Mantém o usuário no centro. North Up mantém o rumo 0; Course Up gira o mapa para que a
     * direção da caminhada fique para cima. O rumo só é usado em movimento, porque parado
     * o rumo do GPS fica pulando.
     */
    private void followCamera(LatLng position, Location location) {
        CameraPosition current = googleMap.getCameraPosition();
        float bearing = 0f;
        if (isCourseUp()) {
            boolean reliableBearing = location.hasBearing() && recorder.getCurrentSpeed() >= MIN_SPEED_FOR_BEARING;
            bearing = reliableBearing ? location.getBearing() : current.bearing;
        }
        CameraPosition target = new CameraPosition.Builder(current)
                .target(position)
                .bearing(bearing)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(target));
    }

    private boolean isCourseUp() {
        return settingsPrefs.getNavigationMode() == SettingsPrefs.NavigationMode.COURSE_UP;
    }

    // Bússola: volta para o norte (no Course Up o mapa gira de novo na próxima posição em movimento)
    private void resetBearing() {
        if (googleMap == null) {
            return;
        }
        CameraPosition north = new CameraPosition.Builder(googleMap.getCameraPosition()).bearing(0f).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(north));
    }

    // Raio = precisão do GPS em metros: a posição real está dentro do círculo com ~68% de probabilidade
    private void updateAccuracyCircle(LatLng center, float accuracyMeters) {
        if (accuracyCircle == null) {
            accuracyCircle = googleMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(accuracyMeters)
                    .fillColor(ContextCompat.getColor(this, R.color.accuracy_fill))
                    .strokeColor(ContextCompat.getColor(this, R.color.accuracy_stroke))
                    .strokeWidth(getResources().getDimension(R.dimen.accuracy_stroke_width)));
        } else {
            accuracyCircle.setCenter(center);
            accuracyCircle.setRadius(accuracyMeters);
        }
    }

    // Um único marcador para a tela toda: criado na primeira posição e depois só movido
    private void updatePositionMarker(LatLng position, Location location) {
        if (positionMarker == null) {
            positionMarker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .icon(MapIcons.fromVector(this, R.drawable.ic_position_marker))
                    .anchor(0.5f, 0.5f)
                    .flat(true));
        } else {
            positionMarker.setPosition(position);
        }

        // Marcador flat: a rotação é relativa ao norte, então a seta continua apontando o rumo certo
        // mesmo quando o mapa gira (Course Up). Parado não há rumo, então mantém o último
        if (positionMarker != null && location.hasBearing()) {
            positionMarker.setRotation(location.getBearing());
        }
    }

    private void onLocationPermissionDenied() {
        binding.chipAccuracy.setText(R.string.record_accuracy_no_permission);

        // Depois de duas recusas (ou "não perguntar novamente") o Android para de mostrar o diálogo dele,
        // então o único caminho é a tela de configurações do app
        boolean permanentlyDenied =
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_location_title)
                .setMessage(R.string.permission_location_denied_message)
                .setNegativeButton(R.string.action_back, (d, which) -> finish());
        if (permanentlyDenied) {
            dialog.setPositiveButton(R.string.permission_open_settings, (d, which) -> openAppSettings());
        } else {
            dialog.setPositiveButton(R.string.permission_try_again,
                    (d, which) -> locationPermissionLauncher.launch(LOCATION_PERMISSIONS));
        }
        dialog.setCancelable(false).show();
    }

    private void showPermissionRationale() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_location_title)
                .setMessage(R.string.permission_location_rationale)
                .setNegativeButton(R.string.action_back, (d, which) -> finish())
                .setPositiveButton(R.string.permission_continue,
                        (d, which) -> locationPermissionLauncher.launch(LOCATION_PERMISSIONS))
                .setCancelable(false)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
        finish();
    }

    private void setupMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this::onMapReady);
        }
    }

    private void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.setMapType(toGoogleMapType(settingsPrefs.getMapType()));

        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setMapToolbarEnabled(false);
        // A orientação vem da configuração North Up / Course Up, não da rotação com dois dedos
        uiSettings.setRotateGesturesEnabled(false);

        // A agulha da bússola aponta o norte real: gira o ícone no sentido contrário ao rumo do mapa
        googleMap.setOnCameraMoveListener(() ->
                binding.buttonCompass.setRotation(-googleMap.getCameraPosition().bearing));
    }

    private int toGoogleMapType(SettingsPrefs.MapType mapType) {
        switch (mapType) {
            case SATELLITE:
                return GoogleMap.MAP_TYPE_SATELLITE;
            case VECTOR:
            default:
                return GoogleMap.MAP_TYPE_NORMAL;
        }
    }

    private void showCurrentSettings() {
        binding.chipSettings.setText(getString(R.string.record_settings_summary,
                getString(settingsPrefs.getMapType().labelResId),
                getString(settingsPrefs.getNavigationMode().labelResId)));
    }

    private void setupWindowInsets() {
        View overlay = binding.recordOverlay;

        int initialLeft = overlay.getPaddingLeft();
        int initialTop = overlay.getPaddingTop();
        int initialRight = overlay.getPaddingRight();
        int initialBottom = overlay.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(overlay, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialLeft + systemBars.left,
                    initialTop + systemBars.top,
                    initialRight + systemBars.right,
                    initialBottom + systemBars.bottom
            );
            return insets;
        });
    }
}
