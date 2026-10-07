package com.proyecto.servicios.service;

import com.proyecto.servicios.model.clientes.UsuarioResponse;

import java.util.List;

public interface UsuarioService {

    UsuarioResponse obtenerUsuario(Long id);

    UsuarioResponse obtenerUsuarioPorCorreo(String correo);

    UsuarioResponse obtenerUsuarioPorClienteId(Long clienteId);

    List<UsuarioResponse> obtenerTodos();

    List<UsuarioResponse> obtenerUsuariosActivos();

    List<UsuarioResponse> buscarPorCorreo(String correo);

    UsuarioResponse crearUsuario(Long clienteId, String correo, String password);

    UsuarioResponse actualizarUsuario(Long id, String correo, String password);

    UsuarioResponse desactivarUsuario(Long id);

    UsuarioResponse activarUsuario(Long id);
}
