package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.CuentaSearchRequest;
import com.proyecto.servicios.model.clientes.CuentaUpdateRequest;
import org.springframework.data.domain.Page;

public interface CuentaService {

    // Consultas
    CuentaResponse obtenerCuentaPorNumero(String numeroCuenta);

    /**
     * Consulta cuentas con filtros opcionales (combinables) y paginación.
     *
     * @param filtros criterios de búsqueda; todos opcionales y combinables
     *                ({@code clienteId}, {@code activas}) más paginación
     * @return página de cuentas que cumplen los criterios
     */
    Page<CuentaResponse> buscarCuentas(CuentaSearchRequest filtros);

    // Actualización
    CuentaResponse actualizarCuenta(String numeroCuenta, CuentaUpdateRequest request);

    // Creación
    CuentaResponse crearCuenta(Long clienteId, String numeroCuenta);
}
