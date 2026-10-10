package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, Long>, JpaSpecificationExecutor<CuentaBancaria> {

    Optional<CuentaBancaria> findByNumeroCuenta(String numeroCuenta);

    boolean existsByNumeroCuenta(String numeroCuenta);

    List<CuentaBancaria> findByPersonaFisicaId(Long personaFisicaId);

    List<CuentaBancaria> findByPersonaFisicaIdIn(List<Long> personaFisicaIds);
}
