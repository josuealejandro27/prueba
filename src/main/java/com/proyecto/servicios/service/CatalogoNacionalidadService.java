package com.proyecto.servicios.service;

import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadRequest;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadResponse;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadSearchRequest;
import org.springframework.data.domain.Page;

public interface CatalogoNacionalidadService {

    CatalogoNacionalidadResponse obtenerPorId(Long id);

    Page<CatalogoNacionalidadResponse> buscar(CatalogoNacionalidadSearchRequest filtros);

    CatalogoNacionalidadResponse crear(CatalogoNacionalidadRequest request);

    CatalogoNacionalidadResponse actualizar(CatalogoNacionalidadActualizaRequest request);

    void eliminar(Long id);
}
