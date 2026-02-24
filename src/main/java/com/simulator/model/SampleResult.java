package com.simulator.model;

import java.time.LocalDateTime;

/**
 * Representa el resultado de una muestra individual (un hilo/conexión).
 */
public class SampleResult {

    private final int     id;
    private final boolean exitosa;
    private final long    tiempoMs;
    private final int     reintentos;
    private final LocalDateTime timestamp;
    private final String  error;
    private final String  queryEjecutada;

    public SampleResult(int id, boolean exitosa, long tiempoMs,
                        int reintentos, String queryEjecutada, String error) {
        this.id             = id;
        this.exitosa        = exitosa;
        this.tiempoMs       = tiempoMs;
        this.reintentos     = reintentos;
        this.queryEjecutada = queryEjecutada;
        this.error          = error;
        this.timestamp      = LocalDateTime.now();
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public int     getId()             { return id; }
    public boolean isExitosa()         { return exitosa; }
    public long    getTiempoMs()       { return tiempoMs; }
    public int     getReintentos()     { return reintentos; }
    public LocalDateTime getTimestamp(){ return timestamp; }
    public String  getError()          { return error != null ? error : ""; }
    public String  getQueryEjecutada() { return queryEjecutada; }

    @Override
    public String toString() {
        String estado = exitosa ? "EXITOSA" : "FALLIDA";
        return String.format("[ID-%03d] [%s] Tiempo: %dms | Reintentos: %d%s",
            id, estado, tiempoMs, reintentos,
            exitosa ? "" : " | Error: " + error);
    }
}
