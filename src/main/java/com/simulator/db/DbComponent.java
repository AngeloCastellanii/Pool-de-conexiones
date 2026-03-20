package com.simulator.db;

import com.simulator.db.adapter.SqlAdapter;
import com.simulator.db.model.DbQueryResult;
import com.simulator.pool.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Componente desacoplado a través de adapters y con pool interno.
 * No expone ejecución de SQL crudo: solo permite query por id.
 */
public class DbComponent {

    private final ConnectionPool pool;
    private final Map<String, String> queries;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    public DbComponent(
            SqlAdapter adapter,
            String host,
            int port,
            String database,
            String user,
            String password,
            int poolMinSize,
            int poolMaxSize,
            long poolScaleUpThresholdMs,
            long poolScaleDownThresholdMs,
            long poolAcquireTimeoutMs,
            String queryCatalogResource) {
        try {
            Class.forName(adapter.driverClassName());
            String jdbcUrl = adapter.buildJdbcUrl(host, port, database);
            this.pool = new ConnectionPool(
                    jdbcUrl,
                    user,
                    password,
                    poolMinSize,
                    poolMaxSize,
                    poolScaleUpThresholdMs,
                    poolScaleDownThresholdMs,
                    poolAcquireTimeoutMs,
                    null);
            this.queries = QueryCatalog.load(queryCatalogResource);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo inicializar el pool para " + adapter.dialectName(), e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No se encontró el driver JDBC: " + adapter.driverClassName(), e);
        }
    }

    public DbQueryResult query(String queryId) throws SQLException, InterruptedException {
        String sql = resolveSql(queryId);

        Connection txConn = transactionConnection.get();
        if (txConn != null) {
            return executeSql(txConn, sql);
        }

        Connection conn = null;
        try {
            conn = pool.acquire();
            return executeSql(conn, sql);
        } finally {
            if (conn != null) {
                pool.release(conn);
            }
        }
    }

    public DbTransaction transaction() throws SQLException, InterruptedException {
        if (transactionConnection.get() != null) {
            throw new IllegalStateException("Ya existe una transacción activa en este hilo");
        }

        Connection conn = pool.acquire();
        conn.setAutoCommit(false);
        transactionConnection.set(conn);
        return new DbTransaction(conn);
    }

    public void shutdown() {
        pool.shutdown();
    }

    public final class DbTransaction implements AutoCloseable {

        private final Connection conn;
        private boolean active = true;

        private DbTransaction(Connection conn) {
            this.conn = conn;
        }

        public DbQueryResult query(String queryId) throws SQLException {
            ensureActive();
            return executeSql(conn, resolveSql(queryId));
        }

        public void commit() throws SQLException {
            ensureActive();
            conn.commit();
            close();
        }

        public void rollback() throws SQLException {
            ensureActive();
            conn.rollback();
            close();
        }

        @Override
        public void close() throws SQLException {
            if (!active) {
                return;
            }
            active = false;
            try {
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }
            } finally {
                transactionConnection.remove();
                pool.release(conn);
            }
        }

        private void ensureActive() {
            if (!active) {
                throw new IllegalStateException("La transacción ya fue cerrada");
            }
        }
    }

    private String resolveSql(String queryId) {
        String sql = queries.get(queryId);
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("Query no encontrada en catálogo: " + queryId);
        }
        return sql;
    }

    private DbQueryResult executeSql(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            boolean hasResultSet = stmt.execute();
            if (!hasResultSet) {
                return new DbQueryResult(false, stmt.getUpdateCount(), List.of());
            }

            try (ResultSet rs = stmt.getResultSet()) {
                List<Map<String, Object>> rows = toRows(rs);
                return new DbQueryResult(true, -1, rows);
            }
        }
    }

    private List<Map<String, Object>> toRows(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int colCount = md.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(md.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }

        return rows;
    }
}
