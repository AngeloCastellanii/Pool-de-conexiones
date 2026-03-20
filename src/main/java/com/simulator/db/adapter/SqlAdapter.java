package com.simulator.db.adapter;

/**
 * Adapter para desacoplar la construcción del JDBC URL y el driver.
 */
public interface SqlAdapter {

    String dialectName();

    String driverClassName();

    String buildJdbcUrl(String host, int port, String database);
}
