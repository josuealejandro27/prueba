package com.proyecto.servicios.repositorys.clientes;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonaFisicaRepository extends JpaRepository<PersonaFisica, Long> {

    Optional<PersonaFisica> findByCurp(String curp);

    Optional<PersonaFisica> findByRfc(String rfc);

    Optional<PersonaFisica> findByCorreo(String correo);

    List<PersonaFisica> findByNombreContainingIgnoreCase(String nombre);

    List<PersonaFisica> findByApellidoPaternoContainingIgnoreCase(String apellidoPaterno);

    List<PersonaFisica> findByApellidoMaternoContainingIgnoreCase(String apellidoMaterno);

    List<PersonaFisica> findByActivoTrue();

    @Query("SELECT p FROM PersonaFisica p WHERE p.fechaCreacion BETWEEN :fechaInicio AND :fechaFin")
    List<PersonaFisica> findByFechaCreacionBetween(@Param("fechaInicio") Date fechaInicio, @Param("fechaFin") Date fechaFin);

    @Query("SELECT p FROM PersonaFisica p LEFT JOIN FETCH p.domicilio LEFT JOIN FETCH p.informacionLaboral WHERE p.id = :id")
    Optional<PersonaFisica> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT p FROM PersonaFisica p LEFT JOIN FETCH p.domicilio LEFT JOIN FETCH p.informacionLaboral")
    List<PersonaFisica> findAllWithDetails();

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);
}
