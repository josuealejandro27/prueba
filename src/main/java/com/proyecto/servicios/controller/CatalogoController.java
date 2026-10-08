package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.catalogo.CatalogoCacheResponseDTO;
import com.proyecto.servicios.service.ProductoCatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint del catálogo general (GestoPago).
 * Expone el snapshot del catálogo de productos sincronizado desde MongoDB.
 */
@RestController
public class CatalogoController {

    private final ProductoCatalogoService productoCatalogoService;

    public CatalogoController(ProductoCatalogoService productoCatalogoService) {
        this.productoCatalogoService = productoCatalogoService;
    }

    @Operation(summary = "Obtener catálogo general")
    @GetMapping(value = "/gestopago/catalogo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CatalogoCacheResponseDTO>> obtenerCatalogo() {
        CatalogoCacheResponseDTO catalogo = productoCatalogoService.obtenerCatalogoCacheado();
        return ResponseEntity.ok(ApiResponse.of(ApiResponseEnum.OK, catalogo));
    }
}
