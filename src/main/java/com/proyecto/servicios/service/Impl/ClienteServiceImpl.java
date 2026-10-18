package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.*;
import com.proyecto.servicios.entity.clientes.*;
import com.proyecto.servicios.exception.ClienteException;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.model.clientes.ClienteActualizaRequest;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.model.clientes.ClienteSearchRequest;
import com.proyecto.servicios.repositorys.catalogos.*;
import com.proyecto.servicios.repositorys.clientes.CuentaBancariaRepository;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.ClienteService;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";
    private static final String CODIGO_CLIENTE = "CLIENTE_ERROR";

    private static final String ESTATUS_ACTIVA = "ACTIVA";
    private static final String ESTATUS_BLOQUEADA = "BLOQUEADA";
    private static final String ESTATUS_CANCELADA = "CANCELADA";

    @Autowired
    private PersonaFisicaRepository personaFisicaRepository;

    @Autowired
    private CuentaBancariaRepository cuentaBancariaRepository;

    @Autowired
    private CatalogoGenerosRepository catalogoGenerosRepository;

    @Autowired
    private CatalogoPaisRepository catalogoPaisRepository;

    @Autowired
    private CatalogoEstadoCivilRepository catalogoEstadoCivilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        log.info("Creando nuevo cliente: {} {}", request.getNombre(), request.getApellidoPaterno());

        // Validar mayoría de edad
        validarMayorEdad(request.getFechaNacimiento());

        // Normalizar CURP y RFC a mayúsculas
        String curpNormalizada = request.getCurp().toUpperCase();
        String rfcNormalizado = request.getRfc().toUpperCase();

        // Validar que no exista otro cliente con la misma CURP
        if (personaFisicaRepository.existsByCurp(curpNormalizada)) {
            throw new ClienteException(CODIGO_CLIENTE, "Ya existe un cliente con la CURP: " + curpNormalizada, HttpStatus.CONFLICT);
        }

        // Validar que no exista otro cliente con el mismo RFC
        if (personaFisicaRepository.existsByRfc(rfcNormalizado)) {
            throw new ClienteException(CODIGO_CLIENTE, "Ya existe un cliente con el RFC: " + rfcNormalizado, HttpStatus.CONFLICT);
        }

        // Validar que no exista otro cliente con el mismo correo
        if (personaFisicaRepository.existsByCorreo(request.getCorreo())) {
            throw new ClienteException(CODIGO_CLIENTE, "Ya existe un cliente con el correo: " + request.getCorreo(), HttpStatus.CONFLICT);
        }

        // Validar que no exista otro cliente con la misma combinación RFC + CURP
        if (personaFisicaRepository.findByRfcAndCurp(rfcNormalizado, curpNormalizada).isPresent()) {
            throw new ClienteException(CODIGO_CLIENTE, "Ya existe un cliente con el RFC y CURP indicados", HttpStatus.CONFLICT);
        }

        // Crear PersonaFísica
        PersonaFisica personaFisica = new PersonaFisica();
        personaFisica.setNombre(request.getNombre());
        personaFisica.setSegundoNombre(request.getSegundoNombre());
        personaFisica.setApellidoPaterno(request.getApellidoPaterno());
        personaFisica.setApellidoMaterno(request.getApellidoMaterno());
        personaFisica.setFechaNacimiento(request.getFechaNacimiento());
        personaFisica.setCurp(curpNormalizada);
        personaFisica.setRfc(rfcNormalizado);

        // Asignar catálogos
        CatalogoGeneros genero = catalogoGenerosRepository.findById(request.getGeneroId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Género no encontrado con ID: " + request.getGeneroId()));
        personaFisica.setGenero(genero);

        CatalogoPais nacionalidad = catalogoPaisRepository.findById(request.getNacionalidadId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Nacionalidad no encontrada con ID: " + request.getNacionalidadId()));
        personaFisica.setNacionalidad(nacionalidad);

        CatalogoEstadoCivil estadoCivil = catalogoEstadoCivilRepository.findById(request.getEstadoCivilId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Estado civil no encontrado con ID: " + request.getEstadoCivilId()));
        personaFisica.setEstadoCivil(estadoCivil);

        // Datos de contacto
        personaFisica.setCorreo(request.getCorreo());
        personaFisica.setLada(request.getLada());
        personaFisica.setNumeroTelefono(request.getNumeroTelefono());
        personaFisica.setNumeroTelefono2(request.getNumeroTelefono2());

        // Domicilio
        Domicilio domicilio = new Domicilio();
        domicilio.setCalle(request.getCalle());
        domicilio.setNoExterior(request.getNoExterior());
        domicilio.setNoInterior(request.getNoInterior());
        domicilio.setColonia(request.getColonia());
        domicilio.setMunicipio(request.getMunicipio());
        domicilio.setEstado(request.getEstado());
        domicilio.setCp(request.getCp());
        domicilio.setPais(request.getPais());
        personaFisica.setDomicilio(domicilio);

        // Información laboral
        InformacionLaboral infoLaboral = new InformacionLaboral();
        infoLaboral.setOcupacion(request.getOcupacion());
        infoLaboral.setEmpresa(request.getEmpresa());
        infoLaboral.setIngresoMensual(request.getIngresoMensual());
        infoLaboral.setNumeroTelefono(request.getNumeroTelefono());
        personaFisica.setInformacionLaboral(infoLaboral);

        // Estado inicial
        personaFisica.setCuentaBloqueada(false);
        personaFisica.setLoginBloqueado(false);
        personaFisica.setActivo(true);

        personaFisica = personaFisicaRepository.save(personaFisica);

        // Crear cuenta bancaria automáticamente
        CuentaBancaria cuentaBancaria = new CuentaBancaria();
        cuentaBancaria.setNumeroCuenta(generarNumeroCuenta());
        cuentaBancaria.setSaldo(request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO);
        cuentaBancaria.setFechaApertura(LocalDateTime.now());
        cuentaBancaria.setPersonaFisica(personaFisica);
        cuentaBancaria.setEstatus(ESTATUS_ACTIVA);
        cuentaBancariaRepository.save(cuentaBancaria);

        // Crear usuario automáticamente
        Usuario usuario = new Usuario();
        usuario.setCliente(personaFisica);
        usuario.setCorreo(request.getCorreo());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        log.info("Cliente creado exitosamente con ID: {}", personaFisica.getId());
        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(ClienteActualizaRequest request) {
        Long id = request.getId();
        log.info("Actualizando cliente con ID: {}", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id, HttpStatus.NOT_FOUND));

        // Actualizar datos personales (no CURP ni RFC)
        if (request.getNombre() != null) {
            personaFisica.setNombre(request.getNombre());
        }
        if (request.getSegundoNombre() != null) {
            personaFisica.setSegundoNombre(request.getSegundoNombre());
        }
        if (request.getApellidoPaterno() != null) {
            personaFisica.setApellidoPaterno(request.getApellidoPaterno());
        }
        if (request.getApellidoMaterno() != null) {
            personaFisica.setApellidoMaterno(request.getApellidoMaterno());
        }
        if (request.getFechaNacimiento() != null) {
            validarMayorEdad(request.getFechaNacimiento());
            personaFisica.setFechaNacimiento(request.getFechaNacimiento());
        }
        if (request.getGeneroId() != null) {
            CatalogoGeneros genero = catalogoGenerosRepository.findById(request.getGeneroId())
                    .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Género no encontrado"));
            personaFisica.setGenero(genero);
        }
        if (request.getNacionalidadId() != null) {
            CatalogoPais nacionalidad = catalogoPaisRepository.findById(request.getNacionalidadId())
                    .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Nacionalidad no encontrada"));
            personaFisica.setNacionalidad(nacionalidad);
        }
        if (request.getEstadoCivilId() != null) {
            CatalogoEstadoCivil estadoCivil = catalogoEstadoCivilRepository.findById(request.getEstadoCivilId())
                    .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Estado civil no encontrado"));
            personaFisica.setEstadoCivil(estadoCivil);
        }

        // Actualizar datos de contacto
        if (request.getCorreo() != null && !request.getCorreo().equals(personaFisica.getCorreo())) {
            if (personaFisicaRepository.existsByCorreo(request.getCorreo())) {
                throw new ClienteException(CODIGO_CLIENTE, "Ya existe un cliente con el correo: " + request.getCorreo(), HttpStatus.CONFLICT);
            }
            personaFisica.setCorreo(request.getCorreo());
            // El correo del cliente es el nombre de usuario: se mantienen sincronizados
            usuarioRepository.findByClienteId(id).ifPresent(usuario -> {
                usuario.setCorreo(request.getCorreo());
                usuarioRepository.save(usuario);
            });
        }
        if (request.getLada() != null) {
            personaFisica.setLada(request.getLada());
        }
        if (request.getNumeroTelefono() != null) {
            personaFisica.setNumeroTelefono(request.getNumeroTelefono());
        }
        if (request.getNumeroTelefono2() != null) {
            personaFisica.setNumeroTelefono2(request.getNumeroTelefono2());
        }

        // Actualizar domicilio
        Domicilio domicilio = personaFisica.getDomicilio();
        if (domicilio == null) {
            domicilio = new Domicilio();
        }
        if (request.getCalle() != null) {
            domicilio.setCalle(request.getCalle());
        }
        if (request.getNoExterior() != null) {
            domicilio.setNoExterior(request.getNoExterior());
        }
        if (request.getNoInterior() != null) {
            domicilio.setNoInterior(request.getNoInterior());
        }
        if (request.getColonia() != null) {
            domicilio.setColonia(request.getColonia());
        }
        if (request.getMunicipio() != null) {
            domicilio.setMunicipio(request.getMunicipio());
        }
        if (request.getEstado() != null) {
            domicilio.setEstado(request.getEstado());
        }
        if (request.getCp() != null) {
            domicilio.setCp(request.getCp());
        }
        if (request.getPais() != null) {
            domicilio.setPais(request.getPais());
        }
        personaFisica.setDomicilio(domicilio);

        // Actualizar información laboral
        InformacionLaboral infoLaboral = personaFisica.getInformacionLaboral();
        if (infoLaboral == null) {
            infoLaboral = new InformacionLaboral();
        }
        if (request.getOcupacion() != null) {
            infoLaboral.setOcupacion(request.getOcupacion());
        }
        if (request.getEmpresa() != null) {
            infoLaboral.setEmpresa(request.getEmpresa());
        }
        if (request.getIngresoMensual() != null) {
            infoLaboral.setIngresoMensual(request.getIngresoMensual());
        }
        infoLaboral.setNumeroTelefono(personaFisica.getNumeroTelefono());
        personaFisica.setInformacionLaboral(infoLaboral);

        personaFisica = personaFisicaRepository.save(personaFisica);

        log.info("Cliente actualizado exitosamente");
        return mapToResponse(personaFisica, obtenerCuentaPrincipal(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerCliente(Long id) {
        log.info("Consultando cliente con ID: {}", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id, HttpStatus.NOT_FOUND));

        return mapToResponse(personaFisica, obtenerCuentaPrincipal(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponse> buscarClientes(ClienteSearchRequest filtros) {
        log.info("Buscando clientes con filtros combinables");

        Specification<PersonaFisica> spec = Specification.where(null);

        if (filtros.getCurp() != null) {
            String curp = filtros.getCurp().trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("curp"), curp));
        }
        if (filtros.getRfc() != null) {
            String rfc = filtros.getRfc().trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("rfc"), rfc));
        }
        if (filtros.getCorreo() != null) {
            String correo = "%" + filtros.getCorreo().toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("correo")), correo));
        }
        if (filtros.getNombre() != null) {
            String nombre = "%" + filtros.getNombre().toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), nombre));
        }
        if (filtros.getApellidoPaterno() != null) {
            String apellidoPaterno = "%" + filtros.getApellidoPaterno().toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("apellidoPaterno")), apellidoPaterno));
        }
        if (filtros.getApellidoMaterno() != null) {
            String apellidoMaterno = "%" + filtros.getApellidoMaterno().toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("apellidoMaterno")), apellidoMaterno));
        }
        if (Boolean.TRUE.equals(filtros.getActivo())) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("activo")));
        } else if (Boolean.FALSE.equals(filtros.getActivo())) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("activo")));
        }
        if (Boolean.TRUE.equals(filtros.getCuentaBloqueada())) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("cuentaBloqueada")));
        } else if (Boolean.FALSE.equals(filtros.getCuentaBloqueada())) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("cuentaBloqueada")));
        }
        if (Boolean.TRUE.equals(filtros.getLoginBloqueado())) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("loginBloqueado")));
        } else if (Boolean.FALSE.equals(filtros.getLoginBloqueado())) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("loginBloqueado")));
        }
        if (filtros.getFechaInicio() != null) {
            Date fechaInicio = filtros.getFechaInicio();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("fechaCreacion"), fechaInicio));
        }
        if (filtros.getFechaFin() != null) {
            Date fechaFin = filtros.getFechaFin();
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("fechaCreacion"), fechaFin));
        }
        if (filtros.getNumeroCuenta() != null) {
            String numeroCuenta = filtros.getNumeroCuenta().trim();
            spec = spec.and((root, query, cb) -> {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<CuentaBancaria> cuenta = sub.from(CuentaBancaria.class);
                sub.select(cuenta.get("personaFisica").get("id"));
                sub.where(cb.equal(cuenta.get("numeroCuenta"), numeroCuenta));
                return root.get("id").in(sub);
            });
        }

        Pageable pageable = filtros.aPageable();
        Page<PersonaFisica> page = personaFisicaRepository.findAll(spec, pageable);
        List<ClienteResponse> contenido = mapList(page.getContent());

        return new PageImpl<>(contenido, pageable, page.getTotalElements());
    }

    @Override
    @Transactional
    public ClienteResponse bloquearCuenta(Long id, boolean bloqueado) {
        log.info("{} cuenta del cliente con ID: {}", bloqueado ? "Bloqueando" : "Desbloqueando", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id, HttpStatus.NOT_FOUND));

        personaFisica.setCuentaBloqueada(bloqueado);
        personaFisica = personaFisicaRepository.save(personaFisica);

        // Actualizar estatus de la cuenta
        CuentaBancaria cuentaBancaria = obtenerCuentaPrincipal(id);
        cuentaBancaria.setEstatus(bloqueado ? ESTATUS_BLOQUEADA : ESTATUS_ACTIVA);
        cuentaBancariaRepository.save(cuentaBancaria);

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    @Transactional
    public ClienteResponse bloquearLogin(Long id, boolean bloqueado) {
        log.info("{} login del cliente con ID: {}", bloqueado ? "Bloqueando" : "Desbloqueando", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id, HttpStatus.NOT_FOUND));

        personaFisica.setLoginBloqueado(bloqueado);
        personaFisica = personaFisicaRepository.save(personaFisica);

        return mapToResponse(personaFisica, obtenerCuentaPrincipal(id));
    }

    @Override
    @Transactional
    public ClienteResponse desactivarCliente(Long id) {
        log.info("Desactivando cliente con ID: {}", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id, HttpStatus.NOT_FOUND));

        personaFisica.setActivo(false);
        personaFisica = personaFisicaRepository.save(personaFisica);

        // Cancelar todas las cuentas asociadas (relación 1:N)
        List<CuentaBancaria> cuentas = cuentaBancariaRepository.findByPersonaFisicaId(id);
        for (CuentaBancaria cuenta : cuentas) {
            cuenta.setEstatus(ESTATUS_CANCELADA);
            cuentaBancariaRepository.save(cuenta);
        }

        // Desactivar el usuario asociado
        usuarioRepository.findByClienteId(id).ifPresent(usuario -> {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
        });

        log.info("Cliente desactivado exitosamente");
        return mapToResponse(personaFisica, cuentas.isEmpty() ? null : cuentas.get(0));
    }

    @Override
    @Transactional
    public ClienteResponse activarCliente(Long id) {
        log.info("Activando cliente con ID: {}", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id, HttpStatus.NOT_FOUND));

        personaFisica.setActivo(true);
        personaFisica = personaFisicaRepository.save(personaFisica);

        // Reactivar la cuenta principal asociada
        CuentaBancaria cuentaBancaria = obtenerCuentaPrincipal(id);
        cuentaBancaria.setEstatus(ESTATUS_ACTIVA);
        cuentaBancariaRepository.save(cuentaBancaria);

        // Reactivar el usuario asociado (coherente con la baja lógica)
        usuarioRepository.findByClienteId(id).ifPresent(usuario -> {
            usuario.setActivo(true);
            usuarioRepository.save(usuario);
        });

        log.info("Cliente activado exitosamente");
        return mapToResponse(personaFisica, cuentaBancaria);
    }

    private void validarMayorEdad(Date fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ValidationException(CODIGO_VALIDACION, "La fecha de nacimiento es obligatoria");
        }

        Date hoy = new Date();
        if (fechaNacimiento.after(hoy)) {
            throw new ValidationException(CODIGO_VALIDACION, "La fecha de nacimiento no puede ser una fecha futura");
        }

        int edad = Period.between(
                new Date(fechaNacimiento.getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                hoy.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        ).getYears();

        if (edad < 18) {
            throw new ValidationException(CODIGO_VALIDACION, "El cliente debe ser mayor de edad (18 años o más)");
        }
    }

    private String generarNumeroCuenta() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Obtiene la cuenta principal (preferentemente ACTIVA) de un cliente.
     */
    private CuentaBancaria obtenerCuentaPrincipal(Long personaFisicaId) {
        List<CuentaBancaria> cuentas = cuentaBancariaRepository.findByPersonaFisicaId(personaFisicaId);
        if (cuentas.isEmpty()) {
            throw new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada");
        }
        return elegirCuentaPrincipal(cuentas);
    }

    private static CuentaBancaria elegirCuentaPrincipal(List<CuentaBancaria> cuentas) {
        return cuentas.stream()
                .filter(c -> ESTATUS_ACTIVA.equals(c.getEstatus()))
                .findFirst()
                .orElse(cuentas.get(0));
    }

    /**
     * Mapea una lista de clientes resolviendo sus cuentas en una sola consulta (evita N+1).
     */
    private List<ClienteResponse> mapList(List<PersonaFisica> personas) {
        if (personas.isEmpty()) {
            return List.of();
        }
        List<Long> ids = personas.stream().map(PersonaFisica::getId).collect(Collectors.toList());
        Map<Long, CuentaBancaria> cuentasPorCliente = cuentaBancariaRepository.findByPersonaFisicaIdIn(ids).stream()
                .collect(Collectors.groupingBy(
                        c -> c.getPersonaFisica().getId(),
                        Collectors.collectingAndThen(Collectors.toList(), ClienteServiceImpl::elegirCuentaPrincipal)));

        return personas.stream()
                .map(persona -> mapToResponse(persona, cuentasPorCliente.get(persona.getId())))
                .collect(Collectors.toList());
    }

    private ClienteResponse mapToResponse(PersonaFisica persona, CuentaBancaria cuenta) {
        ClienteResponse response = new ClienteResponse();
        response.setId(persona.getId());
        response.setNombre(persona.getNombre());
        response.setSegundoNombre(persona.getSegundoNombre());
        response.setApellidoPaterno(persona.getApellidoPaterno());
        response.setApellidoMaterno(persona.getApellidoMaterno());
        response.setFechaNacimiento(persona.getFechaNacimiento());
        response.setCurp(persona.getCurp());
        response.setRfc(persona.getRfc());
        response.setGenero(persona.getGenero() != null ? persona.getGenero().getTipo() : null);
        response.setNacionalidad(persona.getNacionalidad() != null ? persona.getNacionalidad().getNombre() : null);
        response.setEstadoCivil(persona.getEstadoCivil() != null ? persona.getEstadoCivil().getNombre() : null);
        response.setCorreo(persona.getCorreo());
        response.setNumeroTelefono(persona.getNumeroTelefono());
        response.setCalle(persona.getDomicilio() != null ? persona.getDomicilio().getCalle() : null);
        response.setColonia(persona.getDomicilio() != null ? persona.getDomicilio().getColonia() : null);
        response.setMunicipio(persona.getDomicilio() != null ? persona.getDomicilio().getMunicipio() : null);
        response.setEstado(persona.getDomicilio() != null ? persona.getDomicilio().getEstado() : null);
        response.setCp(persona.getDomicilio() != null ? String.valueOf(persona.getDomicilio().getCp()) : null);
        response.setPais(persona.getDomicilio() != null ? persona.getDomicilio().getPais() : null);
        response.setOcupacion(persona.getInformacionLaboral() != null ? persona.getInformacionLaboral().getOcupacion() : null);
        response.setEmpresa(persona.getInformacionLaboral() != null ? persona.getInformacionLaboral().getEmpresa() : null);
        response.setIngresoMensual(persona.getInformacionLaboral() != null ? persona.getInformacionLaboral().getIngresoMensual() : null);
        response.setNumeroCuenta(cuenta != null ? cuenta.getNumeroCuenta() : null);
        response.setSaldo(cuenta != null ? cuenta.getSaldo() : null);
        response.setCuentaBloqueada(persona.isCuentaBloqueada());
        response.setLoginBloqueado(persona.isLoginBloqueado());
        response.setActivo(persona.isActivo());
        return response;
    }
}
