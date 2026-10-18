package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.UsuarioCreateRequest;
import com.proyecto.servicios.model.clientes.UsuarioResponse;
import com.proyecto.servicios.model.clientes.UsuarioSearchRequest;
import com.proyecto.servicios.model.clientes.UsuarioUpdateRequest;
import org.springframework.data.domain.Page;

public interface UsuarioService {

    UsuarioResponse obtenerUsuario(Long id);

    /**
     * Busca usuarios con criterios opcionales (combinables) y paginación configurable.
     * Sin criterios se devuelven todos los usuarios.
     *
     * @param filtros criterios de búsqueda; nunca {@code null}
     */
    Page<UsuarioResponse> buscarUsuarios(UsuarioSearchRequest filtros);

    UsuarioResponse crearUsuario(UsuarioCreateRequest request);

    UsuarioResponse actualizarUsuario(UsuarioUpdateRequest request);

    UsuarioResponse desactivarUsuario(Long id);

    UsuarioResponse activarUsuario(Long id);
}
