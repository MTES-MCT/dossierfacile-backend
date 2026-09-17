package fr.dossierfacile.scheduler.tasks.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("java:S2077") // Dynamic SQL safe: table and column names come from static AnalyticsTableMapping enum whitelist, salt is escaped
public class AnalyticsReplicationService {

    private final AnalyticsProperties properties;

    public void replicateAll() throws SQLException {
        String salt = properties.getSalt();
        if (salt == null || salt.isBlank()) {
            throw new IllegalArgumentException("ANALYTICS_SALT must be configured and not blank");
        }

        String sourceUrl = properties.getSourceUrl();
        String sourceUsername = properties.getSourceUsername();
        String sourcePassword = properties.getSourcePassword();

        String destUrl = properties.getDestUrl();
        String destUsername = properties.getDestUsername();
        String destPassword = properties.getDestPassword();

        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("Source database URL must be configured");
        }
        if (destUrl == null || destUrl.isBlank()) {
            throw new IllegalArgumentException("Destination database URL must be configured");
        }

        // Garde-fou 2a : Vérification d'égalité d'URL pour empêcher toute destruction accidentelle
        if (sourceUrl.equalsIgnoreCase(destUrl)) {
            throw new IllegalStateException("CRITICAL SAFETY CHECK: Destination database URL is identical to Source database URL! Aborting to prevent data loss.");
        }

        log.info("Connexion à la base source ({}) et destination ({})...", sourceUrl, destUrl);
        try (Connection sourceConn = DriverManager.getConnection(sourceUrl, sourceUsername, sourcePassword);
             Connection destConn = DriverManager.getConnection(destUrl, destUsername, destPassword)) {

            // Garde-fou 1 : Verrouillage strict de la connexion source en lecture seule au niveau de la session PostgreSQL.
            // Interdit physiquement tout ordre DDL (DROP, ALTER, TRUNCATE) ou DML (INSERT, UPDATE, DELETE) sur la base source.
            sourceConn.setReadOnly(true);

            // Garde-fou 2b : Vérification d'identité physique de la base de données (hôte, port, nom de base).
            // Empêche toute bascule destructrice si des alias DNS différents pointent vers la même base.
            assertDifferentDatabases(sourceConn, destConn);

            configureConnectionTimeouts(sourceConn, "source");
            configureConnectionTimeouts(destConn, "destination");

            log.info("Démarrage de la copie de {} tables vers les tables temporaires...", AnalyticsTableMapping.values().length);
            for (AnalyticsTableMapping mapping : AnalyticsTableMapping.values()) {
                replicateTable(sourceConn, destConn, mapping, salt);
            }

            log.info("Toutes les tables ont été copiées avec succès. Démarrage de la bascule atomique...");
            atomicSwap(destConn);
        }
    }

    /**
     * Garde-fou de sécurité critique : s'assure que la connexion destination ne pointe pas physiquement
     * sur le même cluster PostgreSQL et la même base de données que la source.
     */
    private void assertDifferentDatabases(Connection sourceConn, Connection destConn) throws SQLException {
        try (Statement sStmt = sourceConn.createStatement();
             Statement dStmt = destConn.createStatement();
             ResultSet sRs = sStmt.executeQuery("SELECT inet_server_addr(), inet_server_port(), current_database();");
             ResultSet dRs = dStmt.executeQuery("SELECT inet_server_addr(), inet_server_port(), current_database();")) {

            if (sRs.next() && dRs.next()) {
                String sAddr = sRs.getString(1);
                String dAddr = dRs.getString(1);
                String sPort = sRs.getString(2);
                String dPort = dRs.getString(2);
                String sDb = sRs.getString(3);
                String dDb = dRs.getString(3);

                boolean sameHost = Objects.equals(sAddr, dAddr);
                boolean samePort = Objects.equals(sPort, dPort);
                boolean sameDb = Objects.equals(sDb, dDb);

                if (sameHost && samePort && sameDb) {
                    throw new IllegalStateException(
                            "CRITICAL SAFETY CHECK: Source and Destination resolve to the EXACT SAME physical database (" +
                                    sDb + " on " + sAddr + ":" + sPort + ")! Aborting to prevent data loss."
                    );
                }
            }
        }
    }

    private void configureConnectionTimeouts(Connection conn, String connectionName) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET statement_timeout = 0;");
            stmt.execute("SET lock_timeout = '60000';");
            log.debug("Timeouts configurés (statement_timeout=0, lock_timeout=60000) pour la connexion {}", connectionName);
        }
    }

    private void replicateTable(Connection sourceConn, Connection destConn, AnalyticsTableMapping mapping, String salt) throws SQLException {
        String table = mapping.getDestTableName();
        log.info("Début réplication table {}", table);

        try (Statement dropStmt = destConn.createStatement()) {
            dropStmt.execute("DROP TABLE IF EXISTS tmp_" + table + " CASCADE;");
        }

        createTableFromSourceMetadata(sourceConn, destConn, mapping, salt);

        PGConnection sourcePg = sourceConn.unwrap(PGConnection.class);
        PGConnection destPg = destConn.unwrap(PGConnection.class);

        String selectSql = mapping.buildSelectQuery(salt);
        String copyOutSql = "COPY (" + selectSql + ") TO STDOUT WITH (FORMAT CSV, HEADER FALSE)";
        String copyInSql = "COPY tmp_" + table + " FROM STDIN WITH (FORMAT CSV)";

        streamCopy(sourcePg, destPg, copyOutSql, copyInSql, table);

        log.info("Table {} copiée avec succès dans tmp_{}", table, table);
    }

    private void streamCopy(PGConnection sourcePg, PGConnection destPg, String copyOutSql, String copyInSql, String table) throws SQLException {
        AtomicReference<Exception> producerError = new AtomicReference<>();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        try (PipedInputStream in = new PipedInputStream(64 * 1024);
             PipedOutputStream out = new PipedOutputStream(in)) {

            Thread producer = Thread.ofPlatform()
                    .name("analytics-producer-" + table)
                    .start(() -> runCopyOut(sourcePg, copyOutSql, out, table, mdcContext, producerError));

            runCopyIn(destPg, copyInSql, in, producer, table);

            if (producerError.get() != null) {
                Exception ex = producerError.get();
                if (ex instanceof SQLException sqlException) {
                    throw sqlException;
                }
                throw new RuntimeException("Erreur lors du copyOut pour " + table, ex);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interruption pendant la copie de la table " + table, e);
        } catch (IOException e) {
            throw new RuntimeException("Erreur d'E/S sur le stream de copie pour " + table, e);
        }
    }

    private void runCopyOut(PGConnection sourcePg, String copyOutSql, PipedOutputStream out, String table, Map<String, String> mdcContext, AtomicReference<Exception> producerError) {
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        try {
            sourcePg.getCopyAPI().copyOut(copyOutSql, out);
            out.flush();
        } catch (Exception e) {
            log.error("Erreur copyOut pour {}", table, e);
            producerError.set(e);
        } finally {
            try {
                out.close();
            } catch (IOException ignored) {
                // Ignore failure on stream close
            }
            MDC.clear();
        }
    }

    private void runCopyIn(PGConnection destPg, String copyInSql, PipedInputStream in, Thread producer, String table) throws SQLException, IOException, InterruptedException {
        try {
            destPg.getCopyAPI().copyIn(copyInSql, in);
        } catch (Exception e) {
            log.error("Erreur copyIn pour {}", table, e);
            try {
                in.close(); // Close pipe early to unblock producer thread waiting on buffer space
            } catch (IOException ignored) {
                // Ignore failure on stream close
            }
            producer.interrupt();
            throw e;
        } finally {
            producer.join();
        }
    }

    private void createTableFromSourceMetadata(Connection sourceConn, Connection destConn, AnalyticsTableMapping mapping, String salt) throws SQLException {
        String query = mapping.buildSelectQuery(salt) + " LIMIT 0";
        try (Statement stmt = sourceConn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData md = rs.getMetaData();
            StringBuilder sb = new StringBuilder("CREATE TABLE tmp_").append(mapping.getDestTableName()).append(" (");
            for (int i = 1; i <= md.getColumnCount(); i++) {
                if (i > 1) {
                    sb.append(", ");
                }
                sb.append("\"").append(md.getColumnLabel(i)).append("\" ").append(md.getColumnTypeName(i));
            }
            sb.append(");");
            try (Statement destStmt = destConn.createStatement()) {
                destStmt.execute(sb.toString());
            }
        }
    }

    private void atomicSwap(Connection destConn) throws SQLException {
        destConn.setAutoCommit(false);
        try (Statement swapStmt = destConn.createStatement()) {
            for (AnalyticsTableMapping mapping : AnalyticsTableMapping.values()) {
                String table = mapping.getDestTableName();
                swapStmt.addBatch("DROP TABLE IF EXISTS " + table + " CASCADE;");
                swapStmt.addBatch("ALTER TABLE tmp_" + table + " RENAME TO " + table + ";");
            }
            swapStmt.executeBatch();
            destConn.commit();
            log.info("Bascule atomique globale (Zero-Downtime Swap) effectuée avec succès.");
        } catch (Exception e) {
            destConn.rollback();
            log.error("Erreur lors de la bascule atomique, rollback effectué sur la destination", e);
            throw e;
        } finally {
            destConn.setAutoCommit(true);
        }
    }
}
