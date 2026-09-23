package com.proyecto.servicios.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.concurrent.TimeUnit;

/**
 * Configuración de MongoDB, usado únicamente como caché del catálogo.
 * <p>
 * Se limita el {@code serverSelectionTimeout} para que, si MongoDB no está
 * disponible, la aplicación pueda arrancar y responder errores rápido en vez
 * de bloquearse 30 segundos (valor por defecto del driver).
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.proyecto.servicios.repositorys.mongo")
public class MongoCacheConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.connection-connect-timeout:3000}")
    private long connectTimeoutMs;

    @Value("${spring.data.mongodb.connection-socket-timeout:3000}")
    private long socketTimeoutMs;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public MongoClientSettings mongoClientSettings() {
        return MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .applyToClusterSettings(builder -> builder.serverSelectionTimeout(3, TimeUnit.SECONDS))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout((int) connectTimeoutMs, TimeUnit.MILLISECONDS)
                        .readTimeout((int) socketTimeoutMs, TimeUnit.MILLISECONDS))
                .build();
    }
}
