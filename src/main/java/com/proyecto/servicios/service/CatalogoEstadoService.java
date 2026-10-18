package com.proyecto.servicios.service;

import com.proyecto.servicios.model.catalogo.CatalogoEstadoActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoResponse;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoSearchRequest;
import org.springframework.data.domain.Page;

public interface CatalogoEstadoService {

    CatalogoEstadoResponse obtenerPorId(Long id);

    Page<CatalogoEstadoResponse> buscar(CatalogoEstadoSearchRequest filtros);

    CatalogoEstadoResponse crear(CatalogoEstadoRequest request);

    CatalogoEstadoResponse actualizar(CatalogoEstadoActualizaRequest request);

    void eliminar(Long id);
}
