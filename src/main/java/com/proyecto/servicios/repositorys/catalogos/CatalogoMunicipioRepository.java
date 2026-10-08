package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CatalogoMunicipio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoMunicipioRepository extends JpaRepository<CatalogoMunicipio, Long> {

    List<CatalogoMunicipio> findByNombreContainingIgnoreCase(String nombre);

    List<CatalogoMunicipio> findByEstadoId(Long estadoId);
}