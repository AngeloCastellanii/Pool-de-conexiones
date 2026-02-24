package com.simulator;

import com.simulator.config.SimulationConfig;
import com.simulator.core.IterativeRunner;
import com.simulator.core.PooledSimulation;
import com.simulator.core.RawSimulation;
import com.simulator.logging.SimulationLogger;
import com.simulator.metrics.MetricsCollector;
import com.simulator.model.SimulationReport;

/**
 * PARTE 8 — Punto de entrada del Simulador de Pool de Conexiones
 *
 * Flujo:
 * - Si simulation.iterative=true -> IterativeRunner (tabla de pasos N)
 * - Si simulation.iterative=false -> flujo normal RAW + POOLED + comparativa
 *
 * Para la GUI JavaFX: mvn javafx:run
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("    SIMULADOR DE POOL DE CONEXIONES - PostgreSQL + JDBC");
        System.out.println("=".repeat(60));

        SimulationConfig config;
        try {
            config = new SimulationConfig("config.properties");
        } catch (Exception e) {
            System.err.println("ERROR: No se pudo cargar config.properties: " + e.getMessage());
            return;
        }

        System.out.println(config);

        try (SimulationLogger logger = new SimulationLogger()) {

            logger.logInfo("Configuracion cargada: " + config);

            // Modo ITERATIVO
            if (config.isIterative()) {
                logger.logInfo("Modo ITERATIVO activado. Pasos: " + config.getIterativeSteps());
                IterativeRunner runner = new IterativeRunner(config, logger);
                runner.run();
                System.out.println("Archivo de log generado en: " + logger.getLogFilePath());
                return;
            }

            // Modo NORMAL: RAW -> POOLED -> Comparativa

            // Simulacion 1: RAW
            SimulationReport rawReport;
            try {
                RawSimulation raw = new RawSimulation(config, logger);
                rawReport = raw.run();
                logger.logInfo(String.format(
                        "RAW completada: %d/%d exitosas (%.1f%%) | Tiempo: %dms",
                        rawReport.getExitosas(), rawReport.getTotalMuestras(),
                        rawReport.getPorcentajeExito(), rawReport.getTiempoTotalMs()));
            } catch (Exception e) {
                logger.logError("Error en simulacion RAW: " + e.getMessage());
                return;
            }

            // Pausa entre simulaciones
            logger.logInfo("Pausa de 2 segundos entre simulaciones...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }

            // Simulacion 2: POOLED
            SimulationReport pooledReport;
            try {
                PooledSimulation pooled = new PooledSimulation(config, logger);
                pooledReport = pooled.run();
                logger.logInfo(String.format(
                        "POOLED completada: %d/%d exitosas (%.1f%%) | Tiempo: %dms",
                        pooledReport.getExitosas(), pooledReport.getTotalMuestras(),
                        pooledReport.getPorcentajeExito(), pooledReport.getTiempoTotalMs()));
            } catch (Exception e) {
                logger.logError("Error en simulacion POOLED: " + e.getMessage());
                return;
            }

            // Reporte final comparativo
            MetricsCollector.imprimirComparativa(rawReport, pooledReport, logger);

            System.out.println("Archivo de log generado en: " + logger.getLogFilePath());

        } catch (Exception e) {
            System.err.println("ERROR inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
