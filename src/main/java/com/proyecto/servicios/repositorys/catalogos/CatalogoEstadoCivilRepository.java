package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoEstadoCivilRepository extends JpaRepository<CatalogoEstadoCivil, Long> {

    List<CatalogoEstadoCivil> findByNombreContainingIgnoreCase(String nombre);
}