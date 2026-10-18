package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CatalogoMunicipio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogoMunicipioRepository extends JpaRepository<CatalogoMunicipio, Long>,
        JpaSpecificationExecutor<CatalogoMunicipio> {
}
