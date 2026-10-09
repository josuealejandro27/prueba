package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogos.CatalogoColonia;

import java.util.List;

public interface CatalogoColoniaService {

    List<CatalogoColonia> obtenerTodos();

    CatalogoColonia obtenerPorId(Long id);

    CatalogoColonia crear(CatalogoColonia catalogoColonia);

    CatalogoColonia actualizar(Long id, CatalogoColonia catalogoColonia);

    void eliminar(Long id);

    List<CatalogoColonia> buscarPorNombre(String nombre);

    List<CatalogoColonia> obtenerPorMunicipioId(Long municipioId);
}