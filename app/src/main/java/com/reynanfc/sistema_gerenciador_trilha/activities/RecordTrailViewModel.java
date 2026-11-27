package com.reynanfc.sistema_gerenciador_trilha.activities;

import androidx.lifecycle.ViewModel;

import com.reynanfc.sistema_gerenciador_trilha.location.TrailRecorder;

/**
 * Keeps the recording alive across configuration changes: on rotation Android destroys and
 * recreates the Activity, but hands the same ViewModel to the new instance.
 */
public class RecordTrailViewModel extends ViewModel {

    final TrailRecorder recorder = new TrailRecorder();
}
