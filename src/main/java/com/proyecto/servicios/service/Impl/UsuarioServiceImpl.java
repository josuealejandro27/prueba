package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.entity.clientes.Usuario;
import com.proyecto.servicios.exception.UsuarioException;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.model.clientes.UsuarioCreateRequest;
import com.proyecto.servicios.model.clientes.UsuarioResponse;
import com.proyecto.servicios.model.clientes.UsuarioSearchRequest;
import com.proyecto.servicios.model.clientes.UsuarioUpdateRequest;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private static final String CODIGO_USUARIO = "USUARIO_ERROR";
    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonaFisicaRepository personaFisicaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerUsuario(Long id) {
        log.info("Consultando usuario con ID: {}", id);

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioException(CODIGO_USUARIO, "Usuario no encontrado con ID: " + id, HttpStatus.NOT_FOUND));

        return mapToResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> buscarUsuarios(UsuarioSearchRequest filtros) {
        String correoLimpio = (filtros.getCorreo() != null && !filtros.getCorreo().isBlank())
                ? filtros.getCorreo().trim() : null;
        log.info("Consultando usuarios con filtros - correo: {}, clienteId: {}, activos: {}",
                correoLimpio, filtros.getClienteId(), filtros.getActivos());

        Specification<Usuario> spec = Specification.where(null);

        if (correoLimpio != null) {
            // Coincidencia parcial, sin distinguir mayúsculas/minúsculas.
            String patron = "%" + correoLimpio.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("correo")), patron));
        }

        if (filtros.getClienteId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("cliente").get("id"), filtros.getClienteId()));
        }

        if (filtros.getActivos() != null) {
            spec = spec.and((root, query, cb) -> Boolean.TRUE.equals(filtros.getActivos())
                    ? cb.isTrue(root.get("activo"))
                    : cb.isFalse(root.get("activo")));
        }

        return usuarioRepository.findAll(spec, filtros.aPageable())
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public UsuarioResponse crearUsuario(UsuarioCreateRequest request) {
        log.info("Creando usuario para cliente con ID: {}", request.getClienteId());

        // Validar que el cliente exista
        PersonaFisica cliente = personaFisicaRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION,
                        "Cliente no encontrado con ID: " + request.getClienteId()));

        String correo = request.getCorreo().trim();

        // Validar que no exista otro usuario con el mismo correo
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new UsuarioException(CODIGO_USUARIO, "Ya existe un usuario con el correo: " + correo, HttpStatus.CONFLICT);
        }

        // Validar que el cliente no tenga ya un usuario
        if (usuarioRepository.existsByClienteId(request.getClienteId())) {
            throw new UsuarioException(CODIGO_USUARIO, "El cliente ya tiene un usuario asociado", HttpStatus.CONFLICT);
        }

        // Crear usuario (activo y fechas los controla el servidor)
        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);

        usuario = usuarioRepository.save(usuario);

        log.info("Usuario creado exitosamente con ID: {}", usuario.getId());
        return mapToResponse(usuario);
    }

    @Override
    @Transactional
    public UsuarioResponse actualizarUsuario(UsuarioUpdateRequest request) {
        log.info("Actualizando usuario con ID: {}", request.getId());

        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioException(CODIGO_USUARIO,
                        "Usuario no encontrado con ID: " + request.getId(), HttpStatus.NOT_FOUND));

        String correo = (request.getCorreo() != null && !request.getCorreo().isBlank())
                ? request.getCorreo().trim() : null;

        if (correo != null && !correo.equals(usuario.getCorreo())) {
            if (usuarioRepository.existsByCorreo(correo)) {
                throw new UsuarioException(CODIGO_USUARIO, "Ya existe un usuario con el correo: " + correo, HttpStatus.CONFLICT);
            }
            usuario.setCorreo(correo);
        }

        if (request.getPassword() != null) {
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
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
                .orElseThrow(() -> new UsuarioException(CODIGO_USUARIO, "Usuario no encontrado con ID: " + id, HttpStatus.NOT_FOUND));

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
                .orElseThrow(() -> new UsuarioException(CODIGO_USUARIO, "Usuario no encontrado con ID: " + id, HttpStatus.NOT_FOUND));

        usuario.setActivo(true);
        usuario = usuarioRepository.save(usuario);

        log.info("Usuario activado exitosamente");
        return mapToResponse(usuario);
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
