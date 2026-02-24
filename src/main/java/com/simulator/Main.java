package com.simulator;

import com.simulator.config.SimulationConfig;
import com.simulator.logging.SimulationLogger;

/**
 * Punto de entrada del simulador.
 * Próximas partes agregarán RawSimulation, PooledSimulation y MetricsCollector.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        SimulationConfig config = new SimulationConfig("config.properties");

        try (SimulationLogger logger = new SimulationLogger()) {
            logger.logInfo("Configuración cargada correctamente.");
            logger.logInfo(config.toString());
            logger.logInfo("Proyecto estructurado. Partes 4-8 pendientes.");
        }
    }
}
