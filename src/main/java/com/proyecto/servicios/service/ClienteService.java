package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;

import java.util.List;

public interface ClienteService {

    ClienteResponse crearCliente(ClienteRequest request);

    ClienteResponse actualizarCliente(Long id, ClienteRequest request);

    ClienteResponse obtenerCliente(Long id);

    List<ClienteResponse> obtenerTodos();

    ClienteResponse bloquearCuenta(Long id, boolean bloqueado);

    ClienteResponse bloquearLogin(Long id, boolean bloqueado);
}
