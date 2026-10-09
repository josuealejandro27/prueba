package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface GestoPagoTokenRepository extends JpaRepository<GestoPagoToken, Integer> {

    Optional<GestoPagoToken> findByIdDistribuidorAndCodigoDispositivo(Integer idDistribuidor, String codigoDispositivo);

    // Una sola sentencia evita la carrera entre el @Scheduled y la sincronización inicial del catálogo.
    @Modifying
    @Transactional("sfTransactionManager")
    @Query(value = """
            INSERT INTO gestopago_tokens
                (id_distribuidor, codigo_dispositivo, token, token_type, expires_in,
                 fecha_creacion, fecha_actualizacion, activo)
            VALUES (:idDistribuidor, :codigoDispositivo, :token, :tokenType, :expiresIn,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, TRUE)
            ON CONFLICT (id_distribuidor, codigo_dispositivo) DO UPDATE SET
                token = EXCLUDED.token,
                token_type = COALESCE(EXCLUDED.token_type, gestopago_tokens.token_type),
                expires_in = COALESCE(EXCLUDED.expires_in, gestopago_tokens.expires_in),
                fecha_actualizacion = CURRENT_TIMESTAMP,
                activo = TRUE
            """, nativeQuery = true)
    int guardarOActualizar(@Param("idDistribuidor") Integer idDistribuidor,
                           @Param("codigoDispositivo") String codigoDispositivo,
                           @Param("token") String token,
                           @Param("tokenType") String tokenType,
                           @Param("expiresIn") Long expiresIn);
}
