package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaFisicaRepository extends JpaRepository<PersonaFisica, Long>, JpaSpecificationExecutor<PersonaFisica> {

    Optional<PersonaFisica> findByRfcAndCurp(String rfc, String curp);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    @Query("SELECT p FROM PersonaFisica p LEFT JOIN FETCH p.domicilio LEFT JOIN FETCH p.informacionLaboral WHERE p.id = :id")
    Optional<PersonaFisica> findByIdWithDetails(@Param("id") Long id);
}
