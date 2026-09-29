package com.proyecto.servicios.service;

import com.proyecto.servicios.model.catalogo.CatalogoCacheResponseDTO;

/**
 * Servicio del catálogo de productos.
 * Responsable de sincronizar el catálogo externo hacia la caché de MongoDB
 * y de exponer la información cacheada al resto del sistema.
 */
public interface ProductoCatalogoService {

    /**
     * Tarea programada (cron 06:00 AM) que consume el servicio externo,
     * aplica la política de reintentos y guarda el resultado en MongoDB.
     */
    void sincronizarCatalogo();

    /**
     * Sincronización inicial ejecutada automáticamente al arrancar la
     * aplicación: mismo flujo que la tarea programada.
     */
    void sincronizarCatalogoAlArranque();

    /**
     * Consulta el catálogo almacenado en la caché de MongoDB.
     *
     * @return snapshot vigente del catálogo.
     * @throws com.proyecto.servicios.exception.CatalogoException si no hay caché disponible.
     */
    CatalogoCacheResponseDTO obtenerCatalogoCacheado();
}
