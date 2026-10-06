package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;

import java.util.List;

public interface CatalogoGeneroService {

    List<CatalogoGeneros> obtenerTodos();

    CatalogoGeneros obtenerPorId(Long id);

    CatalogoGeneros crear(CatalogoGeneros catalogoGeneros);

    CatalogoGeneros actualizar(Long id, CatalogoGeneros catalogoGeneros);

    void eliminar(Long id);
}
