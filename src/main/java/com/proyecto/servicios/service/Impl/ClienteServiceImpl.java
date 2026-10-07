package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.*;
import com.proyecto.servicios.entity.clientes.*;
import com.proyecto.servicios.exception.ClienteException;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.model.clientes.ClienteUpdateRequest;
import com.proyecto.servicios.repositorys.catalogos.*;
import com.proyecto.servicios.repositorys.clientes.CuentaBancariaRepository;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.repositorys.clientes.UsuarioRepository;
import com.proyecto.servicios.service.ClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";
    private static final String CODIGO_CLIENTE = "CLIENTE_ERROR";

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
            throw new ClienteException(CODIGO_CLIENTE, "Ya existe un cliente con la CURP: " + curpNormalizada);
        }

        // Validar que no exista otro cliente con el mismo RFC
        if (personaFisicaRepository.existsByRfc(rfcNormalizado)) {
            throw new ClienteException(CODIGO_CLIENTE, "Ya existe un cliente con el RFC: " + rfcNormalizado);
        }

        // Validar que no exista otro cliente con el mismo correo
        if (personaFisicaRepository.existsByCorreo(request.getCorreo())) {
            throw new ClienteException(CODIGO_CLIENTE, "Ya existe un cliente con el correo: " + request.getCorreo());
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
        cuentaBancaria.setEstatus("ACTIVA");
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
    public ClienteResponse actualizarCliente(Long id, ClienteUpdateRequest request) {
        log.info("Actualizando cliente con ID: {}", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id));

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
                throw new ClienteException(CODIGO_CLIENTE, "Ya existe un cliente con el correo: " + request.getCorreo());
            }
            personaFisica.setCorreo(request.getCorreo());
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
        personaFisica.setInformacionLaboral(infoLaboral);

        personaFisica = personaFisicaRepository.save(personaFisica);

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada"));

        log.info("Cliente actualizado exitosamente");
        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    public ClienteResponse obtenerCliente(Long id) {
        log.info("Consultando cliente con ID: {}", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id));

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada"));

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    public ClienteResponse obtenerClientePorCurp(String curp) {
        log.info("Consultando cliente con CURP: {}", curp);

        PersonaFisica personaFisica = personaFisicaRepository.findByCurp(curp)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con CURP: " + curp));

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(personaFisica.getId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada"));

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    public ClienteResponse obtenerClientePorRfc(String rfc) {
        log.info("Consultando cliente con RFC: {}", rfc);

        PersonaFisica personaFisica = personaFisicaRepository.findByRfc(rfc)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con RFC: " + rfc));

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(personaFisica.getId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada"));

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    public ClienteResponse obtenerClientePorCorreo(String correo) {
        log.info("Consultando cliente con correo: {}", correo);

        PersonaFisica personaFisica = personaFisicaRepository.findByCorreo(correo)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con correo: " + correo));

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(personaFisica.getId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada"));

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    public ClienteResponse obtenerClientePorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cliente con número de cuenta: {}", numeroCuenta);

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada con número: " + numeroCuenta));

        PersonaFisica personaFisica = personaFisicaRepository.findById(cuentaBancaria.getPersonaFisica().getId())
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado para la cuenta"));

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    public List<ClienteResponse> obtenerTodos() {
        log.info("Consultando todos los clientes");

        List<PersonaFisica> personas = personaFisicaRepository.findAll();

        return personas.stream()
                .map(persona -> {
                    CuentaBancaria cuenta = cuentaBancariaRepository.findByPersonaFisicaId(persona.getId())
                            .orElse(null);
                    return mapToResponse(persona, cuenta);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> obtenerClientesActivos() {
        log.info("Consultando clientes activos");

        List<PersonaFisica> personas = personaFisicaRepository.findByActivoTrue();

        return personas.stream()
                .map(persona -> {
                    CuentaBancaria cuenta = cuentaBancariaRepository.findByPersonaFisicaId(persona.getId())
                            .orElse(null);
                    return mapToResponse(persona, cuenta);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> buscarPorNombre(String nombre) {
        log.info("Buscando clientes por nombre: {}", nombre);

        List<PersonaFisica> personas = personaFisicaRepository.findByNombreContainingIgnoreCase(nombre);

        return personas.stream()
                .map(persona -> {
                    CuentaBancaria cuenta = cuentaBancariaRepository.findByPersonaFisicaId(persona.getId())
                            .orElse(null);
                    return mapToResponse(persona, cuenta);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> buscarPorApellidoPaterno(String apellidoPaterno) {
        log.info("Buscando clientes por apellido paterno: {}", apellidoPaterno);

        List<PersonaFisica> personas = personaFisicaRepository.findByApellidoPaternoContainingIgnoreCase(apellidoPaterno);

        return personas.stream()
                .map(persona -> {
                    CuentaBancaria cuenta = cuentaBancariaRepository.findByPersonaFisicaId(persona.getId())
                            .orElse(null);
                    return mapToResponse(persona, cuenta);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> buscarPorApellidoMaterno(String apellidoMaterno) {
        log.info("Buscando clientes por apellido materno: {}", apellidoMaterno);

        List<PersonaFisica> personas = personaFisicaRepository.findByApellidoMaternoContainingIgnoreCase(apellidoMaterno);

        return personas.stream()
                .map(persona -> {
                    CuentaBancaria cuenta = cuentaBancariaRepository.findByPersonaFisicaId(persona.getId())
                            .orElse(null);
                    return mapToResponse(persona, cuenta);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClienteResponse> obtenerClientesPorRangoFechas(Date fechaInicio, Date fechaFin) {
        log.info("Consultando clientes entre {} y {}", fechaInicio, fechaFin);

        List<PersonaFisica> personas = personaFisicaRepository.findByFechaCreacionBetween(fechaInicio, fechaFin);

        return personas.stream()
                .map(persona -> {
                    CuentaBancaria cuenta = cuentaBancariaRepository.findByPersonaFisicaId(persona.getId())
                            .orElse(null);
                    return mapToResponse(persona, cuenta);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClienteResponse bloquearCuenta(Long id, boolean bloqueado) {
        log.info("{} cuenta del cliente con ID: {}", bloqueado ? "Bloqueando" : "Desbloqueando", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id));

        personaFisica.setCuentaBloqueada(bloqueado);
        personaFisica = personaFisicaRepository.save(personaFisica);

        // Actualizar estatus de la cuenta
        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada"));

        cuentaBancaria.setEstatus(bloqueado ? "BLOQUEADA" : "ACTIVA");
        cuentaBancariaRepository.save(cuentaBancaria);

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    @Transactional
    public ClienteResponse bloquearLogin(Long id, boolean bloqueado) {
        log.info("{} login del cliente con ID: {}", bloqueado ? "Bloqueando" : "Desbloqueando", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id));

        personaFisica.setLoginBloqueado(bloqueado);
        personaFisica = personaFisicaRepository.save(personaFisica);

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada"));

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    @Transactional
    public ClienteResponse desactivarCliente(Long id) {
        log.info("Desactivando cliente con ID: {}", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id));

        personaFisica.setActivo(false);
        personaFisica = personaFisicaRepository.save(personaFisica);

        // Cancelar todas las cuentas asociadas
        List<CuentaBancaria> cuentas = cuentaBancariaRepository.findByPersonaFisicaId(id)
                .map(List::of)
                .orElse(List.of());
        
        for (CuentaBancaria cuenta : cuentas) {
            cuenta.setEstatus("CANCELADA");
            cuentaBancariaRepository.save(cuenta);
        }

        // Desactivar el usuario asociado
        usuarioRepository.findByClienteId(id).ifPresent(usuario -> {
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
        });

        CuentaBancaria cuentaBancaria = cuentas.isEmpty() ? null : cuentas.get(0);

        log.info("Cliente desactivado exitosamente");
        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    @Transactional
    public ClienteResponse activarCliente(Long id) {
        log.info("Activando cliente con ID: {}", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ClienteException(CODIGO_CLIENTE, "Cliente no encontrado con ID: " + id));

        personaFisica.setActivo(true);
        personaFisica = personaFisicaRepository.save(personaFisica);

        // Activar la cuenta asociada
        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada"));

        cuentaBancaria.setEstatus("ACTIVA");
        cuentaBancariaRepository.save(cuentaBancaria);

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
                fechaNacimiento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                hoy.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        ).getYears();

        if (edad < 18) {
            throw new ValidationException(CODIGO_VALIDACION, "El cliente debe ser mayor de edad (18 años o más)");
        }
    }

    private String generarNumeroCuenta() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
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
