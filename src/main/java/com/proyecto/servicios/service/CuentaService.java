package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.CuentaUpdateRequest;

import java.util.List;

public interface CuentaService {

    // Consultas
    CuentaResponse obtenerCuentaPorNumero(String numeroCuenta);

    /**
     * Consulta cuentas con filtros opcionales (combinables).
     *
     * @param clienteId filtro por cliente; {@code null} = sin filtro
     * @param activas   {@code true} = solo cuentas ACTIVAS, {@code false} = solo no activas,
     *                  {@code null} = sin filtro
     */
    List<CuentaResponse> buscarCuentas(Long clienteId, Boolean activas);

    // Actualización
    CuentaResponse actualizarCuenta(String numeroCuenta, CuentaUpdateRequest request);

    // Creación
    CuentaResponse crearCuenta(Long clienteId, String numeroCuenta);
}
