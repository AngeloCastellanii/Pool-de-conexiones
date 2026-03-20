package com.simulator.db;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Carga queries predefinidas desde un archivo .properties en resources.
 */
final class QueryCatalog {

    private QueryCatalog() {
    }

    static Map<String, String> load(String resourcePath) {
        Properties props = new Properties();
        try (InputStream is = QueryCatalog.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("No se encontró el catálogo de queries: " + resourcePath);
            }
            props.load(is);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar el catálogo de queries: " + resourcePath, e);
        }

        Map<String, String> queries = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            if (value != null && !value.isBlank()) {
                queries.put(key.trim(), value.trim());
            }
        }

        if (queries.isEmpty()) {
            throw new IllegalStateException("El catálogo de queries está vacío: " + resourcePath);
        }

        return Map.copyOf(queries);
    }
}
