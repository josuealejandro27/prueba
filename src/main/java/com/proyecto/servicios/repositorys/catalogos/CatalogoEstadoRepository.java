package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CatalogoEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoEstadoRepository extends JpaRepository<CatalogoEstado, Long> {

    List<CatalogoEstado> findByNombreContainingIgnoreCase(String nombre);

    List<CatalogoEstado> findByPaisId(Long paisId);
}