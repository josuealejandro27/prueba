package com.proyecto.servicios.repositorys.transacciones;

import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionFinancieraRepository extends JpaRepository<TransaccionFinanciera, Long>, JpaSpecificationExecutor<TransaccionFinanciera> {

    Optional<TransaccionFinanciera> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TransaccionFinanciera t WHERE t.idempotencyKey = :idempotencyKey")
    Optional<TransaccionFinanciera> findByIdempotencyKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TransaccionFinanciera t WHERE t.id = :id")
    Optional<TransaccionFinanciera> findByIdForUpdate(@Param("id") Long id);

    Optional<TransaccionFinanciera> findByFolio(String folio);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<TransaccionFinanciera> findByCuentaIdOrderByFechaCreacionDesc(Long cuentaId);

    List<TransaccionFinanciera> findByEstado(EstadoTransaccion estado);

    @Query("SELECT t FROM TransaccionFinanciera t WHERE t.estado = :estado AND t.fechaActualizacion <= :umbral ORDER BY t.fechaCreacion ASC")
    List<TransaccionFinanciera> findPendientesPorEstadoYUmbral(@Param("estado") EstadoTransaccion estado,
                                                               @Param("umbral") LocalDateTime umbral);
}


