package com.proyecto.servicios.service;

import com.proyecto.servicios.model.catalogo.CatalogoGeneroActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroRequest;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroResponse;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroSearchRequest;
import org.springframework.data.domain.Page;

public interface CatalogoGeneroService {

    CatalogoGeneroResponse obtenerPorId(Long id);

    Page<CatalogoGeneroResponse> buscar(CatalogoGeneroSearchRequest filtros);

    CatalogoGeneroResponse crear(CatalogoGeneroRequest request);

    CatalogoGeneroResponse actualizar(CatalogoGeneroActualizaRequest request);

    void eliminar(Long id);
}
