package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogos.CatalogoEstado;

import java.util.List;

public interface CatalogoEstadoService {

    List<CatalogoEstado> obtenerTodos();

    CatalogoEstado obtenerPorId(Long id);

    CatalogoEstado crear(CatalogoEstado catalogoEstado);

    CatalogoEstado actualizar(Long id, CatalogoEstado catalogoEstado);

    void eliminar(Long id);

    List<CatalogoEstado> buscarPorNombre(String nombre);

    List<CatalogoEstado> obtenerPorPaisId(Long paisId);
}