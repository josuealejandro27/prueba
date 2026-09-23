package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.catalogo.CatalogoCacheResponseDTO;
import com.proyecto.servicios.service.ProductoCatalogoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de consulta del catálogo de productos
 * desde la caché de MongoDB.
 */
@RestController
public class CatalogoProductoController {

    private final ProductoCatalogoService productoCatalogoService;

    public CatalogoProductoController(ProductoCatalogoService productoCatalogoService) {
        this.productoCatalogoService = productoCatalogoService;
    }

    @GetMapping(value = "/catalogo/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoCacheResponseDTO>> obtenerCatalogo() {
        CatalogoCacheResponseDTO catalogo = productoCatalogoService.obtenerCatalogoCacheado();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, catalogo));
    }
}
