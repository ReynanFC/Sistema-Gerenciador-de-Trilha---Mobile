package com.reynanfc.sistema_gerenciador_trilha.activities;

import androidx.lifecycle.ViewModel;

import com.reynanfc.sistema_gerenciador_trilha.location.TrailRecorder;

/**
 * Mantém a gravação viva nas mudanças de configuração: na rotação o Android destrói e
 * recria a Activity, mas entrega o mesmo ViewModel para a nova instância.
 */
public class RecordTrailViewModel extends ViewModel {

    final TrailRecorder recorder = new TrailRecorder();
}
