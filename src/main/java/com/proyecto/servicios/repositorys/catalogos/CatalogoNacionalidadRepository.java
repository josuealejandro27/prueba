package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CatalogoNacionalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoNacionalidadRepository extends JpaRepository<CatalogoNacionalidad, Long> {

    List<CatalogoNacionalidad> findByNombreContainingIgnoreCase(String nombre);
}