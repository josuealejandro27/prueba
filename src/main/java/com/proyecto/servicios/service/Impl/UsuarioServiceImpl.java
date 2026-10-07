package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.UsuarioException;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.model.clientes.UsuarioResponse;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private static final String CODIGO_USUARIO = "USUARIO_ERROR";
    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonaFisicaRepository personaFisicaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UsuarioResponse obtenerUsuario(Long id) {
        log.info("Consultando usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioException(CODIGO_USUARIO, "Usuario no encontrado con ID: " + id));

        return mapToResponse(usuario);
    }

    @Override
    public UsuarioResponse obtenerUsuarioPorCorreo(String correo) {
        log.info("Consultando usuario con correo: {}", correo);

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsuarioException(CODIGO_USUARIO, "Usuario no encontrado con correo: " + correo));

        return mapToResponse(usuario);
    }

    @Override
    public UsuarioResponse obtenerUsuarioPorClienteId(Long clienteId) {
        log.info("Consultando usuario del cliente con ID: {}", clienteId);

        Usuario usuario = usuarioRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new UsuarioException(CODIGO_USUARIO, "Usuario no encontrado para el cliente con ID: " + clienteId));

        return mapToResponse(usuario);
    }

    @Override
    public List<UsuarioResponse> obtenerTodos() {
        log.info("Consultando todos los usuarios");

        return usuarioRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UsuarioResponse> obtenerUsuariosActivos() {
        log.info("Consultando usuarios activos");

        return usuarioRepository.findAll().stream()
                .filter(Usuario::isActivo)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UsuarioResponse> buscarPorCorreo(String correo) {
        log.info("Buscando usuarios por correo: {}", correo);

        return usuarioRepository.findAll().stream()
                .filter(u -> u.getCorreo().toLowerCase().contains(correo.toLowerCase()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UsuarioResponse crearUsuario(Long clienteId, String correo, String password) {
        log.info("Creando usuario para cliente con ID: {}", clienteId);

        // Validar que el cliente exista
        PersonaFisica cliente = personaFisicaRepository.findById(clienteId)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cliente no encontrado con ID: " + clienteId));

        // Validar que no exista otro usuario con el mismo correo
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new UsuarioException(CODIGO_USUARIO, "Ya existe un usuario con el correo: " + correo);
        }

        // Validar que el cliente no tenga ya un usuario
        if (usuarioRepository.existsByClienteId(clienteId)) {
            throw new UsuarioException(CODIGO_USUARIO, "El cliente ya tiene un usuario asociado");
        }

        // Validar formato de contraseña
        validarPassword(password);

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setActivo(true);

        usuario = usuarioRepository.save(usuario);

        log.info("Usuario creado exitosamente con ID: {}", usuario.getId());
        return mapToResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse actualizarUsuario(Long id, String correo, String password) {
        log.info("Actualizando usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioException(CODIGO_USUARIO, "Usuario no encontrado con ID: " + id));

        if (correo != null && !correo.equals(usuario.getCorreo())) {
            if (usuarioRepository.existsByCorreo(correo)) {
                throw new UsuarioException(CODIGO_USUARIO, "Ya existe un usuario con el correo: " + correo);
            }
            usuario.setCorreo(correo);
        }

        if (password != null) {
            validarPassword(password);
            usuario.setPassword(passwordEncoder.encode(password));
        }

        usuario = usuarioRepository.save(usuario);

        log.info("Usuario actualizado exitosamente");
        return mapToResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse desactivarUsuario(Long id) {
        log.info("Desactivando usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioException(CODIGO_USUARIO, "Usuario no encontrado con ID: " + id));

        usuario.setActivo(false);
        usuario = usuarioRepository.save(usuario);

        log.info("Usuario desactivado exitosamente");
        return mapToResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse activarUsuario(Long id) {
        log.info("Activando usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioException(CODIGO_USUARIO, "Usuario no encontrado con ID: " + id));

        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);

        log.info("Usuario activado exitosamente");
        return mapToResponse(usuario);
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidationException(CODIGO_VALIDACION, "La contraseña debe tener al menos 8 caracteres");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ValidationException(CODIGO_VALIDACION, "La contraseña debe contener al menos una letra mayúscula, una letra minúscula, un número y un carácter especial");
        }
    }

    private UsuarioResponse mapToResponse(Usuario usuario) {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(usuario.getId());
        response.setClienteId(usuario.getCliente().getId());
        response.setCorreo(usuario.getCorreo());
        response.setActivo(usuario.isActivo());
        response.setFechaCreacion(usuario.getFechaCreacion());
        response.setFechaActualizacion(usuario.getFechaActualizacion());
        return response;
    }
}
