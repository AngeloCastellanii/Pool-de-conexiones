package com.simulator.metrics;

import com.simulator.model.SimulationReport;

import java.util.Set;
import java.util.TreeSet;

/**
 * PARTE 7 — Recolector de Métricas
 *
 * Compara los reportes de RAW y POOLED e imprime la tabla comparativa final.
 */
public class MetricsCollector {

    private static final String SEP = "═".repeat(60);
    private static final String SEP2 = "─".repeat(60);

    public static void imprimirComparativa(SimulationReport raw,
            SimulationReport pooled,
            com.simulator.logging.SimulationLogger logger) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n").append(SEP).append("\n");
        sb.append("          COMPARATIVA FINAL DE SIMULACIONES\n");
        sb.append(SEP).append("\n");

        sb.append(String.format("%-28s %-14s %-14s%n", "Métrica", "RAW", "POOLED"));
        sb.append(SEP2).append("\n");

        // Tiempo total
        sb.append(metrica("Tiempo total (ms)",
                raw.getTiempoTotalMs() + "ms",
                pooled.getTiempoTotalMs() + "ms",
                pooled.getTiempoTotalMs() < raw.getTiempoTotalMs()));

        // Muestras totales
        sb.append(metrica("Total muestras",
                String.valueOf(raw.getTotalMuestras()),
                String.valueOf(pooled.getTotalMuestras()), false));

        // Exitosas
        sb.append(metrica("Exitosas",
                raw.getExitosas() + " (" + f1(raw.getPorcentajeExito()) + "%)",
                pooled.getExitosas() + " (" + f1(pooled.getPorcentajeExito()) + "%)",
                pooled.getPorcentajeExito() >= raw.getPorcentajeExito()));

        // Fallidas
        sb.append(metrica("Fallidas",
                raw.getFallidas() + " (" + f1(100 - raw.getPorcentajeExito()) + "%)",
                pooled.getFallidas() + " (" + f1(100 - pooled.getPorcentajeExito()) + "%)",
                pooled.getFallidas() <= raw.getFallidas()));

        // Promedio reintentos
        sb.append(metrica("Prom. reintentos",
                f2(raw.getPromedioReintentos()),
                f2(pooled.getPromedioReintentos()),
                pooled.getPromedioReintentos() <= raw.getPromedioReintentos()));

        sb.append(SEP).append("\n");

        // Distribución de queries ejecutadas
        sb.append("Distribución de queries (muestras ejecutadas)\n");
        sb.append(SEP2).append("\n");
        sb.append(String.format("%-28s %-14s %-14s%n", "Query", "RAW", "POOLED"));
        sb.append(SEP2).append("\n");

        Set<String> todas = new TreeSet<>();
        todas.addAll(raw.getConteoPorQuery().keySet());
        todas.addAll(pooled.getConteoPorQuery().keySet());

        for (String query : todas) {
            long rawCount = raw.getConteoPorQuery().getOrDefault(query, 0L);
            long pooledCount = pooled.getConteoPorQuery().getOrDefault(query, 0L);
            sb.append(String.format("%-28s %-14s %-14s%n",
                resumirQuery(query),
                rawCount,
                pooledCount));
        }

        sb.append(SEP).append("\n");

        // Conclusión
        int puntosRaw = calcularPuntos(raw, pooled);
        int puntosPooled = calcularPuntos(pooled, raw);

        if (puntosPooled > puntosRaw) {
            sb.append("  GANADOR: POOLED tuvo mejor desempeño general.\n");
            sb.append("  RAZON:   El pool reutiliza conexiones evitando el overhead\n");
            sb.append("           de abrir/cerrar una conexión por cada hilo (RAW).\n");
        } else if (puntosRaw > puntosPooled) {
            sb.append("  GANADOR: RAW tuvo mejor desempeño general.\n");
            sb.append("  RAZON:   Con pocas muestras, el overhead del pool puede\n");
            sb.append("           superar el costo de conexiones directas.\n");
        } else {
            sb.append("  RESULTADO: Empate técnico entre RAW y POOLED.\n");
        }

        sb.append(SEP).append("\n");

        String resultado = sb.toString();
        System.out.println(resultado);
        logger.logInfo(resultado);
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private static String metrica(String nombre, String valRaw, String valPooled,
            boolean pooledGana) {
        String ganador = pooledGana ? "  <- POOLED" : "";
        return String.format("%-28s %-14s %-14s%s%n", nombre, valRaw, valPooled, ganador);
    }

    private static int calcularPuntos(SimulationReport a, SimulationReport b) {
        int pts = 0;
        if (a.getTiempoTotalMs() <= b.getTiempoTotalMs())
            pts++;
        if (a.getPorcentajeExito() >= b.getPorcentajeExito())
            pts++;
        if (a.getPromedioReintentos() <= b.getPromedioReintentos())
            pts++;
        return pts;
    }

    private static String f1(double v) {
        return String.format("%.1f", v);
    }

    private static String f2(double v) {
        return String.format("%.2f", v);
    }

    private static String resumirQuery(String query) {
        if (query == null || query.isBlank()) {
            return "(vacía)";
        }
        String limpia = query.replaceAll("\\s+", " ").trim();
        int max = 26;
        return limpia.length() <= max ? limpia : limpia.substring(0, max) + "...";
    }
}
