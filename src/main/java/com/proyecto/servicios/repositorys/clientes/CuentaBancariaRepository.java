package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaBancariaRepository extends JpaRepository<CuentaBancaria, Long> {

    Optional<CuentaBancaria> findByNumeroCuenta(String numeroCuenta);

    Optional<CuentaBancaria> findByPersonaFisicaId(Long personaFisicaId);
}
