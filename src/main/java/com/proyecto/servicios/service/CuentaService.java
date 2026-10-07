package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.CuentaUpdateRequest;

import java.util.List;

public interface CuentaService {

    // Consultas
    CuentaResponse obtenerCuentaPorNumero(String numeroCuenta);

    List<CuentaResponse> obtenerCuentasPorCliente(Long clienteId);

    List<CuentaResponse> obtenerCuentasPorEstatus(String estatus);

    List<CuentaResponse> obtenerCuentasActivas();

    // Actualización
    CuentaResponse actualizarCuenta(String numeroCuenta, CuentaUpdateRequest request);

    // Creación
    CuentaResponse crearCuenta(Long clienteId, String numeroCuenta);
}
