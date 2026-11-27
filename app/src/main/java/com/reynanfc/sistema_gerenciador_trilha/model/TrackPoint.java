package com.reynanfc.sistema_gerenciador_trilha.model;

/**
 * Uma leitura de GPS de uma trilha: velocidade em m/s, precisão em metros, timestamp em epoch milissegundos.
 * Record imutável: o id do banco e o id da trilha dona não fazem parte do valor;
 * o DAO recebe o id da trilha ao inserir os pontos.
 */
public record TrackPoint(double latitude, double longitude, float speed, float accuracy, long timestamp) {
}
