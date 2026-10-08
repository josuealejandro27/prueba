package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoGenerosRepository extends JpaRepository<CatalogoGeneros, Long> {

    List<CatalogoGeneros> findByTipoContainingIgnoreCase(String tipo);
}