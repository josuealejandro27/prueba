package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CatalogoNacionalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogoNacionalidadRepository extends JpaRepository<CatalogoNacionalidad, Long>,
        JpaSpecificationExecutor<CatalogoNacionalidad> {
}
