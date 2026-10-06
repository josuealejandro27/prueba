package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonaFisicaRepository extends JpaRepository<PersonaFisica, Long> {

    Optional<PersonaFisica> findByCurp(String curp);

    Optional<PersonaFisica> findByRfc(String rfc);

    Optional<PersonaFisica> findByCorreo(String correo);
}
