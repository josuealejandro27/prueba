package com.proyecto.servicios.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.proyecto.servicios.repositorys.sf",
                "com.proyecto.servicios.repositorys.gestopago",
                "com.proyecto.servicios.repositorys.catalogos",
                "com.proyecto.servicios.repositorys.clientes"
        },
        transactionManagerRef = "sfTransactionManager",
        entityManagerFactoryRef = "sfEntityManagerFactory"
)
public class ConfigDB {
    @Autowired
    private Environment env;

    @Bean(name="sfDatasource")
    public DataSource sfDatasource(){
        HikariConfig config=new HikariConfig();
        try{
            config.setJdbcUrl(env.getProperty("spring.datasource.url"));
            config.setPassword(env.getProperty("spring.datasource.password"));
            config.setUsername(env.getProperty("spring.datasource.username"));
            config.setMaximumPoolSize(intProp("spring.datasource.hikari.maximum-pool-size", 10));
            config.setMinimumIdle(intProp("spring.datasource.hikari.minimum-idle", 2));
            config.setMaxLifetime(longProp("spring.datasource.hikari.max-lifetime", 60000));
            config.setConnectionTimeout(longProp("spring.datasource.hikari.connection-timeout", 5000));
            config.setValidationTimeout(longProp("spring.datasource.hikari.validation-timeout", 5000));
            config.setIdleTimeout(longProp("spring.datasource.hikari.idle-timeout", 600000));
            config.setKeepaliveTime(longProp("spring.datasource.hikari.keepalive-time", 20000));
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("sfDatasource");
            // Supabase usa pgbouncer en modo transaction: los prepared statements
            // servidor (S_1, S_2...) entran en conflicto entre clientes (SQL 42P05).
            // Se desactivan para evitar el error al validar/ejecutar consultas.
            config.addDataSourceProperty("prepareThreshold", "0");
            config.addDataSourceProperty("preparedStatementCacheQueries", "0");

        }catch (Exception e){
            log.error("Ha ocurrido un error en la conexcion a base de datos, a causa de:",e);
            return null;
        }
        return new HikariDataSource(config);
    }

    private int intProp(String key, int fallback) {
        String v = env.getProperty(key);
        return v != null ? Integer.parseInt(v) : fallback;
    }

    private long longProp(String key, long fallback) {
        String v = env.getProperty(key);
        return v != null ? Long.parseLong(v) : fallback;
    }

    @Bean(name="sfEntityManagerFactory")
    @DependsOn("flyway")
    public LocalContainerEntityManagerFactoryBean sfEntityManagerFactory(){
        LocalContainerEntityManagerFactoryBean em= new LocalContainerEntityManagerFactoryBean();
        try{
          em.setDataSource(sfDatasource());
          em.setPackagesToScan(
                  "com.proyecto.servicios.entity.sf",
                  "com.proyecto.servicios.entity.gestopago",
                  "com.proyecto.servicios.entity.catalogos",
                  "com.proyecto.servicios.entity.clientes"
          );
          em.setPersistenceUnitName("sfDatasource");
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);
          Map<String, Object> properties=new HashMap<>();
          properties.put("hibernate.hbm2ddl.auto", "none");
            properties.put("hibernate.show-sql", false);
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            properties.put("jakarta.persistence.query.timeout", 600000);


        } catch (Exception e) {
            log.error("Ha ocurrido un error en la conexion a base de datos, a causa de:",e);
            return null;

        }
        return em;
    }
 @Bean(name="sfTransactionManager")
 public PlatformTransactionManager sfTransactionManager(@Qualifier("sfEntityManagerFactory") EntityManagerFactory sfEntityManagerFactory){
        return new JpaTransactionManager(sfEntityManagerFactory);

 }

}
