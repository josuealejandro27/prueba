package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CatalogoColonia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoColoniaRepository extends JpaRepository<CatalogoColonia, Long> {

    List<CatalogoColonia> findByNombreContainingIgnoreCase(String nombre);

    List<CatalogoColonia> findByMunicipioId(Long municipioId);
}