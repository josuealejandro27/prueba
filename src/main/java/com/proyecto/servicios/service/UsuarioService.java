package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.UsuarioResponse;

import java.util.List;

public interface UsuarioService {

    UsuarioResponse obtenerUsuario(Long id);

    /**
     * Busca usuarios con filtros opcionales (combinables).
     *
     * @param correo    filtro por correo (no distingue mayúsculas); {@code null} = sin filtro
     * @param clienteId filtro por cliente; {@code null} = sin filtro
     * @param activos   {@code true} = solo activos, {@code false} = solo inactivos, {@code null} = sin filtro
     */
    List<UsuarioResponse> buscarUsuarios(String correo, Long clienteId, Boolean activos);

    List<UsuarioResponse> buscarPorCorreo(String correo);

    UsuarioResponse crearUsuario(Long clienteId, String correo, String password);

    UsuarioResponse actualizarUsuario(Long id, String correo, String password);

    UsuarioResponse desactivarUsuario(Long id);

    UsuarioResponse activarUsuario(Long id);
}
