package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;

import java.util.List;

public interface CatalogoEstadoCivilService {

    List<CatalogoEstadoCivil> obtenerTodos();

    CatalogoEstadoCivil obtenerPorId(Long id);

    CatalogoEstadoCivil crear(CatalogoEstadoCivil catalogoEstadoCivil);

    CatalogoEstadoCivil actualizar(Long id, CatalogoEstadoCivil catalogoEstadoCivil);

    void eliminar(Long id);

    List<CatalogoEstadoCivil> buscarPorNombre(String nombre);
}