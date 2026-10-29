package com.proyecto.servicios.repositorys.transacciones;

import com.proyecto.servicios.entity.transacciones.TransaccionFinanciera;
import com.proyecto.servicios.enums.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionFinancieraRepository extends JpaRepository<TransaccionFinanciera, Long>, JpaSpecificationExecutor<TransaccionFinanciera> {

    Optional<TransaccionFinanciera> findByIdempotencyKey(String idempotencyKey);

    Optional<TransaccionFinanciera> findByFolio(String folio);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<TransaccionFinanciera> findByCuentaIdOrderByFechaCreacionDesc(Long cuentaId);

    List<TransaccionFinanciera> findByEstado(EstadoTransaccion estado);
}

