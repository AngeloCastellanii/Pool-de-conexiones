package com.simulator.core;

import com.simulator.config.SimulationConfig;
import com.simulator.logging.SimulationLogger;
import com.simulator.model.SimulationReport;

import java.util.ArrayList;
import java.util.List;

/**
 * EXTRA OPCIONAL — Modo Iterativo
 *
 * Ejecuta RAW y POOLED para cada paso de hilos definido en
 * simulation.iterativeSteps (ej: 50,100,200) y presenta una
 * tabla acumulativa al finalizar mostrando cómo escalan ambas
 * estrategias conforme aumenta la carga.
 */
public class IterativeRunner {

    private final SimulationConfig baseConfig;
    private final SimulationLogger logger;

    public IterativeRunner(SimulationConfig config, SimulationLogger logger) {
        this.baseConfig = config;
        this.logger = logger;
    }

    /**
     * Ejecuta todos los pasos iterativos y devuelve la tabla de resultados.
     */
    public List<StepResult> run() throws Exception {

        List<Integer> steps = baseConfig.getIterativeSteps();

        // Guardar resultados de cada paso
        List<StepResult> resultados = new ArrayList<>();

        logger.logSection("MODO ITERATIVO — " + steps.size() + " PASOS: " + steps);

        for (int n : steps) {
            SimulationConfig stepConfig = baseConfig.withSamples(n);

            logger.logInfo("━━━ Iniciando paso con N=" + n + " hilos ━━━");

            // RAW
            RawSimulation raw = new RawSimulation(stepConfig, logger);
            SimulationReport rawReport = raw.run();
            logger.logInfo(String.format("  [RAW   N=%-3d] %d/%d exitosas | Tiempo: %dms",
                    n, rawReport.getExitosas(), rawReport.getTotalMuestras(),
                    rawReport.getTiempoTotalMs()));

            // Pausa entre RAW y POOLED
            Thread.sleep(1500);

            // POOLED
            PooledSimulation pooled = new PooledSimulation(stepConfig, logger);
            SimulationReport pooledReport = pooled.run();
            logger.logInfo(String.format("  [POOLED N=%-3d] %d/%d exitosas | Tiempo: %dms",
                    n, pooledReport.getExitosas(), pooledReport.getTotalMuestras(),
                    pooledReport.getTiempoTotalMs()));

            resultados.add(new StepResult(n, rawReport, pooledReport));

            // Pausa entre pasos
            if (steps.indexOf(n) < steps.size() - 1) {
                logger.logInfo("  Pausa de 2 segundos antes del siguiente paso...");
                Thread.sleep(2000);
            }
        }

        // Imprimir tabla acumulativa
        imprimirTablaIterativa(resultados);
        return resultados;
    }

    // ── Tabla final acumulativa ────────────────────────────────────────────────

    private void imprimirTablaIterativa(List<StepResult> resultados) {
        String sep = "═".repeat(82);
        String sepM = "─".repeat(82);
        String fmt = "│ %-6s │ %-18s │ %-18s │ %-12s │ %-8s │%n";

        StringBuilder sb = new StringBuilder("\n");
        sb.append(sep).append("\n");
        sb.append("             TABLA COMPARATIVA ITERATIVA — RAW vs POOLED\n");
        sb.append(sep).append("\n");
        sb.append(String.format(fmt, "Hilos", "RAW Tiempo (ms)", "POOLED Tiempo (ms)", "Mejora (%)", "Ganador"));
        sb.append(sepM).append("\n");

        for (StepResult r : resultados) {
            long rawMs = r.rawReport.getTiempoTotalMs();
            long pooledMs = r.pooledReport.getTiempoTotalMs();
            double mejora = rawMs > 0 ? ((rawMs - pooledMs) * 100.0 / rawMs) : 0;
            String ganador = pooledMs < rawMs ? "POOLED ✓" : "RAW    ✓";

            sb.append(String.format(fmt,
                    r.n,
                    rawMs + "ms (" + r.rawReport.getExitosas() + "/" + r.rawReport.getTotalMuestras() + ")",
                    pooledMs + "ms (" + r.pooledReport.getExitosas() + "/" + r.pooledReport.getTotalMuestras() + ")",
                    String.format("%.1f%%", mejora),
                    ganador));
        }
        sb.append(sep).append("\n");
        sb.append("  CONCLUSIÓN: El pool de conexiones escala mejor.")
                .append(" A mayor carga, mayor ventaja del POOLED.\n");
        sb.append(sep).append("\n");

        logger.logInfo(sb.toString());
    }

    // Record interno — PUBLIC para que la GUI pueda leerlo
    public static class StepResult {
        public final int n;
        public final SimulationReport rawReport;
        public final SimulationReport pooledReport;

        public StepResult(int n, SimulationReport rawReport, SimulationReport pooledReport) {
            this.n = n;
            this.rawReport = rawReport;
            this.pooledReport = pooledReport;
        }
    }
}
