package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoPaisRepository extends JpaRepository<CatalogoPais, Long> {

    List<CatalogoPais> findByNombreContainingIgnoreCase(String nombre);
}