package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.catalogos.CatalogoPais;

import java.util.List;

public interface CatalogoPaisService {

    List<CatalogoPais> obtenerTodos();

    CatalogoPais obtenerPorId(Long id);

    CatalogoPais crear(CatalogoPais catalogoPais);

    CatalogoPais actualizar(Long id, CatalogoPais catalogoPais);

    void eliminar(Long id);

    List<CatalogoPais> buscarPorNombre(String nombre);
}