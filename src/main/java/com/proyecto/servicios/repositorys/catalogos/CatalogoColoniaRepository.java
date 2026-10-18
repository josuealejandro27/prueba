package com.proyecto.servicios.repositorys.catalogos;

import com.proyecto.servicios.entity.catalogos.CatalogoColonia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogoColoniaRepository extends JpaRepository<CatalogoColonia, Long>,
        JpaSpecificationExecutor<CatalogoColonia> {
}
