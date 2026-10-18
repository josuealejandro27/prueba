package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.model.clientes.ClienteSearchRequest;
import org.springframework.data.domain.Page;

public interface ClienteService {

    // Creación y actualización
    ClienteResponse crearCliente(ClienteRequest request);

    ClienteResponse actualizarCliente(ClienteActualizaRequest request);

    // Consultas
    ClienteResponse obtenerCliente(Long id);

    Page<ClienteResponse> buscarClientes(ClienteSearchRequest filtros);

    // Bloqueo y desbloqueo
    ClienteResponse bloquearCuenta(Long id, boolean bloqueado);

    ClienteResponse bloquearLogin(Long id, boolean bloqueado);

    // Baja lógica
    ClienteResponse desactivarCliente(Long id);

    ClienteResponse activarCliente(Long id);
}
