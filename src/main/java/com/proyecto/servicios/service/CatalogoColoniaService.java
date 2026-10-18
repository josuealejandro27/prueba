package com.proyecto.servicios.service;

import com.proyecto.servicios.model.catalogo.CatalogoColoniaActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaResponse;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaSearchRequest;
import org.springframework.data.domain.Page;

public interface CatalogoColoniaService {

    CatalogoColoniaResponse obtenerPorId(Long id);

    Page<CatalogoColoniaResponse> buscar(CatalogoColoniaSearchRequest filtros);

    CatalogoColoniaResponse crear(CatalogoColoniaRequest request);

    CatalogoColoniaResponse actualizar(CatalogoColoniaActualizaRequest request);

    void eliminar(Long id);
}
