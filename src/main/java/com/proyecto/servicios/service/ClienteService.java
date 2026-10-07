package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.model.clientes.ClienteUpdateRequest;

import java.util.Date;
import java.util.List;

public interface ClienteService {

    // Creación y actualización
    ClienteResponse crearCliente(ClienteRequest request);

    ClienteResponse actualizarCliente(Long id, ClienteUpdateRequest request);

    // Consultas
    ClienteResponse obtenerCliente(Long id);

    ClienteResponse obtenerClientePorCurp(String curp);

    ClienteResponse obtenerClientePorRfc(String rfc);

    ClienteResponse obtenerClientePorCorreo(String correo);

    ClienteResponse obtenerClientePorNumeroCuenta(String numeroCuenta);

    List<ClienteResponse> obtenerTodos();

    List<ClienteResponse> obtenerClientesActivos();

    List<ClienteResponse> buscarPorNombre(String nombre);

    List<ClienteResponse> buscarPorApellidoPaterno(String apellidoPaterno);

    List<ClienteResponse> buscarPorApellidoMaterno(String apellidoMaterno);

    List<ClienteResponse> obtenerClientesPorRangoFechas(Date fechaInicio, Date fechaFin);

    // Bloqueo y desbloqueo
    ClienteResponse bloquearCuenta(Long id, boolean bloqueado);

    ClienteResponse bloquearLogin(Long id, boolean bloqueado);

    // Baja lógica
    ClienteResponse desactivarCliente(Long id);

    ClienteResponse activarCliente(Long id);
}
