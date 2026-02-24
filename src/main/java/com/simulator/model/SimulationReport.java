package com.simulator.model;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Agrega todos los resultados de una simulación y calcula métricas globales.
 */
public class SimulationReport {

    private final String        tipoSimulacion;
    private final List<SampleResult> resultados;
    private final long          tiempoTotalMs;

    public SimulationReport(String tipoSimulacion,
                            List<SampleResult> resultados,
                            long tiempoTotalMs) {
        this.tipoSimulacion = tipoSimulacion;
        this.resultados     = resultados;
        this.tiempoTotalMs  = tiempoTotalMs;
    }

    public long getTotalMuestras()   { return resultados.size(); }
    public String getTipo()          { return tipoSimulacion; }
    public long getTiempoTotalMs()   { return tiempoTotalMs; }
    public List<SampleResult> getResultados() { return resultados; }

    public long getExitosas() {
        return resultados.stream().filter(SampleResult::isExitosa).count();
    }

    public long getFallidas() {
        return resultados.stream().filter(r -> !r.isExitosa()).count();
    }

    public double getPorcentajeExito() {
        if (resultados.isEmpty()) return 0;
        return (getExitosas() * 100.0) / resultados.size();
    }

    public double getPromedioReintentos() {
        if (resultados.isEmpty()) return 0;
        return resultados.stream()
                         .mapToInt(SampleResult::getReintentos)
                         .average()
                         .orElse(0);
    }

    public Map<String, Long> getConteoPorQuery() {
        return resultados.stream()
                .collect(Collectors.groupingBy(SampleResult::getQueryEjecutada,
                        TreeMap::new,
                        Collectors.counting()));
    }
}
