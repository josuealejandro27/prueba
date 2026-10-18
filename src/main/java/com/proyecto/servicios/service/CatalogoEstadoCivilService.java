package com.proyecto.servicios.service;

import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilResponse;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilSearchRequest;
import org.springframework.data.domain.Page;

public interface CatalogoEstadoCivilService {

    CatalogoEstadoCivilResponse obtenerPorId(Long id);

    Page<CatalogoEstadoCivilResponse> buscar(CatalogoEstadoCivilSearchRequest filtros);

    CatalogoEstadoCivilResponse crear(CatalogoEstadoCivilRequest request);

    CatalogoEstadoCivilResponse actualizar(CatalogoEstadoCivilActualizaRequest request);

    void eliminar(Long id);
}
