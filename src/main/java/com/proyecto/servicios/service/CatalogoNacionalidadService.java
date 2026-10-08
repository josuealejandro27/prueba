package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogos.CatalogoNacionalidad;

import java.util.List;

public interface CatalogoNacionalidadService {

    List<CatalogoNacionalidad> obtenerTodos();

    CatalogoNacionalidad obtenerPorId(Long id);

    CatalogoNacionalidad crear(CatalogoNacionalidad catalogoNacionalidad);

    CatalogoNacionalidad actualizar(Long id, CatalogoNacionalidad catalogoNacionalidad);

    void eliminar(Long id);

    List<CatalogoNacionalidad> buscarPorNombre(String nombre);
}