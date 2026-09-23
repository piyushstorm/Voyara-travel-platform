package com.travelplatform;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that Flyway migrations execute successfully on a completely empty PostgreSQL database,
 * and that Spring Boot starts up cleanly with Hibernate ddl-auto=validate.
 *
 * Requirements:
 * 8. Keep production: ddl-auto=validate
 * 10. Test Flyway against a fresh PostgreSQL database/container, not only H2.
 * 11. Verify the application reaches: "Started TravelPlatformApplication"
 */
public class FreshPostgresFlywayMigrationIntegrationTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5432/voyara_flyway_test";
    private static final String PG_USER = "postgres";
    private static final String PG_PASS = "postgrespassword";

    @org.junit.jupiter.api.BeforeEach
    void checkPostgresAvailableAndReset() {
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/velozity_db", PG_USER, PG_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP DATABASE IF EXISTS voyara_flyway_test;");
            stmt.execute("CREATE DATABASE voyara_flyway_test;");
        } catch (Exception e) {
            Assumptions.abort("PostgreSQL container on localhost:5432 is not reachable: " + e.getMessage());
        }
    }

    @Test
    void testFlywayAllMigrationsOnEmptyDatabase() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(PG_URL, PG_USER, PG_PASS)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();

        MigrateResult result = flyway.migrate();

        assertTrue(result.success, "Flyway migrations must succeed on a fresh database");
        assertEquals(14, result.migrationsExecuted, "All 14 migrations must be executed");

        // Verify key tables exist
        try (Connection conn = DriverManager.getConnection(PG_URL, PG_USER, PG_PASS);
             Statement stmt = conn.createStatement()) {
            String[] expectedTables = {
                    "users", "bookings", "flights", "hotels", "rooms", "seats",
                    "payments", "refunds", "refresh_tokens", "notifications",
                    "guardian_alerts", "connection_risks", "price_history",
                    "price_freezes", "cancellation_policies", "room_holds",
                    "review_reports", "destinations", "recommendations", "trip_invitations"
            };

            for (String tbl : expectedTables) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '" + tbl + "');")) {
                    assertTrue(rs.next() && rs.getBoolean(1), "Table '" + tbl + "' must exist after migrations");
                }
            }

            // Verify trip_invitations has sent_at column
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'trip_invitations' AND column_name = 'sent_at');")) {
                assertTrue(rs.next() && rs.getBoolean(1), "Column 'sent_at' must exist in 'trip_invitations'");
            }
        }
    }

    @Test
    void testApplicationStartsSuccessfullyWithHibernateDdlAutoValidate() {
        // First ensure migrations are applied
        Flyway flyway = Flyway.configure()
                .dataSource(PG_URL, PG_USER, PG_PASS)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        flyway.migrate();

        // Boot Spring Boot with ddl-auto=validate against the PostgreSQL database
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TravelPlatformApplication.class)
                .properties(
                        "spring.datasource.url=" + PG_URL,
                        "spring.datasource.username=" + PG_USER,
                        "spring.datasource.password=" + PG_PASS,
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.open-in-view=false",
                        "spring.flyway.enabled=true",
                        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.web.socket.servlet.WebSocketServletAutoConfiguration",
                        "app.jwt.secret=testSecretKeyForUnitTestsOnly1234567890ThisKeyIsAtLeast256BitsLongForHmac!",
                        "seed.data.enabled=false",
                        "server.port=-1"
                )
                .run()) {

            assertTrue(context.isRunning(), "TravelPlatformApplication must be running");
        }
    }
}
