package com.proyecto.servicios.service;

import com.proyecto.servicios.model.catalogo.CatalogoMunicipioActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioRequest;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioResponse;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioSearchRequest;
import org.springframework.data.domain.Page;

public interface CatalogoMunicipioService {

    CatalogoMunicipioResponse obtenerPorId(Long id);

    Page<CatalogoMunicipioResponse> buscar(CatalogoMunicipioSearchRequest filtros);

    CatalogoMunicipioResponse crear(CatalogoMunicipioRequest request);

    CatalogoMunicipioResponse actualizar(CatalogoMunicipioActualizaRequest request);

    void eliminar(Long id);
}
