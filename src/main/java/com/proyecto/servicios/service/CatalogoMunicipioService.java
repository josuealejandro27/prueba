package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogos.CatalogoMunicipio;

import java.util.List;

public interface CatalogoMunicipioService {

    List<CatalogoMunicipio> obtenerTodos();

    CatalogoMunicipio obtenerPorId(Long id);

    CatalogoMunicipio crear(CatalogoMunicipio catalogoMunicipio);

    CatalogoMunicipio actualizar(Long id, CatalogoMunicipio catalogoMunicipio);

    void eliminar(Long id);

    List<CatalogoMunicipio> buscarPorNombre(String nombre);

    List<CatalogoMunicipio> obtenerPorEstadoId(Long estadoId);
}