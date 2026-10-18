package com.proyecto.servicios.service;

import com.proyecto.servicios.model.catalogo.CatalogoPaisActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoPaisRequest;
import com.proyecto.servicios.model.catalogo.CatalogoPaisResponse;
import com.proyecto.servicios.model.catalogo.CatalogoPaisSearchRequest;
import org.springframework.data.domain.Page;

public interface CatalogoPaisService {

    CatalogoPaisResponse obtenerPorId(Long id);

    Page<CatalogoPaisResponse> buscar(CatalogoPaisSearchRequest filtros);

    CatalogoPaisResponse crear(CatalogoPaisRequest request);

    CatalogoPaisResponse actualizar(CatalogoPaisActualizaRequest request);

    void eliminar(Long id);
}
