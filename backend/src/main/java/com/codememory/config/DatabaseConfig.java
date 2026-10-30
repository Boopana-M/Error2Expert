package com.codememory.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Universal Database Configuration.
 * Automatically parses cloud connection strings (Render, Railway, Supabase, Neon, Fly.io)
 * from standard `DATABASE_URL` (postgres:// or postgresql://) into JDBC compliant format,
 * while seamlessly supporting local development and Docker Compose.
 */
@Configuration
@Slf4j
public class DatabaseConfig {

    @Value("${spring.datasource.url:}")
    private String springUrl;

    @Value("${DB_URL:}")
    private String dbUrl;

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${spring.datasource.username:postgres}")
    private String defaultUsername;

    @Value("${spring.datasource.password:postgres}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        // Check environment variable priority
        String envDbUrl = System.getenv("DATABASE_URL");
        if (envDbUrl == null || envDbUrl.isBlank()) {
            envDbUrl = System.getenv("DB_URL");
        }

        String rawUrl = (envDbUrl != null && !envDbUrl.isBlank()) ? envDbUrl :
                (!databaseUrl.isBlank() ? databaseUrl : (!dbUrl.isBlank() ? dbUrl : springUrl));

        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = "jdbc:postgresql://localhost:5432/codememory";
        }

        String username = defaultUsername;
        String password = defaultPassword;
        String jdbcUrl = rawUrl;

        // Automatically convert cloud URI format (postgres://user:pass@host:port/dbname)
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            boolean parsed = false;
            try {
                String uriStr = rawUrl.replace("postgres://", "http://").replace("postgresql://", "http://");
                URI uri = new URI(uriStr);

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath(); // /dbname
                String query = uri.getQuery();

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    username = java.net.URLDecoder.decode(userInfo[0], java.nio.charset.StandardCharsets.UTF_8);
                    if (userInfo.length > 1) {
                        password = java.net.URLDecoder.decode(userInfo[1], java.nio.charset.StandardCharsets.UTF_8);
                    }
                }

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (query != null && !query.isBlank()) {
                    jdbcUrl += "?" + query;
                } else if (!jdbcUrl.contains("sslmode")) {
                    jdbcUrl += "?sslmode=prefer";
                }
                parsed = true;
                log.info("Successfully converted cloud DATABASE_URL to JDBC: jdbc:postgresql://{}:{}{}", host, port, path);
            } catch (Exception e) {
                log.warn("Standard URI parse failed for DATABASE_URL: {}. Attempting direct JDBC formatting.", e.getMessage());
            }

            if (!parsed) {
                String clean = rawUrl.startsWith("postgres://")
                        ? rawUrl.replaceFirst("^postgres://", "jdbc:postgresql://")
                        : rawUrl.replaceFirst("^postgresql://", "jdbc:postgresql://");
                jdbcUrl = clean;
                if (!jdbcUrl.contains("sslmode")) {
                    jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=prefer";
                }
                log.info("Applied direct JDBC format for cloud database URL.");
            }
        }

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            config.setDriverClassName("org.h2.Driver");
        } else {
            config.setDriverClassName("org.postgresql.Driver");
        }
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}
