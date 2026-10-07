package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.*;
import com.proyecto.servicios.entity.clientes.*;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.model.clientes.ClienteRequest;
import com.proyecto.servicios.model.clientes.ClienteResponse;
import com.proyecto.servicios.repositorys.catalogos.*;
import com.proyecto.servicios.repositorys.clientes.CuentaBancariaRepository;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.service.ClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";

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

    @Override
    @Transactional
    public ClienteResponse crearCliente(ClienteRequest request) {
        log.info("Creando nuevo cliente: {} {}", request.getNombre(), request.getApellidoPaterno());

        // Validar que no exista otro cliente con la misma CURP
        if (personaFisicaRepository.findByCurp(request.getCurp()).isPresent()) {
            throw new ValidationException(CODIGO_VALIDACION, "Ya existe un cliente con la CURP: " + request.getCurp());
        }

        // Validar que no exista otro cliente con el mismo RFC
        if (personaFisicaRepository.findByRfc(request.getRfc()).isPresent()) {
            throw new ValidationException(CODIGO_VALIDACION, "Ya existe un cliente con el RFC: " + request.getRfc());
        }

        // Crear PersonaFísica
        PersonaFisica personaFisica = new PersonaFisica();
        personaFisica.setNombre(request.getNombre());
        personaFisica.setSegundoNombre(request.getSegundoNombre());
        personaFisica.setApellidoPaterno(request.getApellidoPaterno());
        personaFisica.setApellidoMaterno(request.getApellidoMaterno());
        personaFisica.setFechaNacimiento(request.getFechaNacimiento());
        personaFisica.setCurp(request.getCurp());
        personaFisica.setRfc(request.getRfc());

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
        DatosContacto datosContacto = new DatosContacto();
        datosContacto.setCorreo(request.getCorreo());
        datosContacto.setLada(request.getLada());
        datosContacto.setNumeroTelefono(request.getNumeroTelefono());
        datosContacto.setNumeroTelefono2(request.getNumeroTelefono2());
        personaFisica.setDatosContacto(datosContacto);

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

        personaFisica = personaFisicaRepository.save(personaFisica);

        // Crear cuenta bancaria
        CuentaBancaria cuentaBancaria = new CuentaBancaria();
        cuentaBancaria.setNumeroCuenta(generarNumeroCuenta());
        cuentaBancaria.setSaldo(request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO);
        cuentaBancaria.setFechaApertura(LocalDateTime.now());
        cuentaBancaria.setPersonaFisica(personaFisica);
        cuentaBancariaRepository.save(cuentaBancaria);

        log.info("Cliente creado exitosamente con ID: {}", personaFisica.getId());
        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(Long id, ClienteRequest request) {
        log.info("Actualizando cliente con ID: {}", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cliente no encontrado con ID: " + id));

        // Actualizar datos personales
        personaFisica.setNombre(request.getNombre());
        personaFisica.setSegundoNombre(request.getSegundoNombre());
        personaFisica.setApellidoPaterno(request.getApellidoPaterno());
        personaFisica.setApellidoMaterno(request.getApellidoMaterno());
        personaFisica.setFechaNacimiento(request.getFechaNacimiento());
        personaFisica.setCurp(request.getCurp());
        personaFisica.setRfc(request.getRfc());

        // Actualizar catálogos
        CatalogoGeneros genero = catalogoGenerosRepository.findById(request.getGeneroId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Género no encontrado con ID: " + request.getGeneroId()));
        personaFisica.setGenero(genero);

        CatalogoPais nacionalidad = catalogoPaisRepository.findById(request.getNacionalidadId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Nacionalidad no encontrada con ID: " + request.getNacionalidadId()));
        personaFisica.setNacionalidad(nacionalidad);

        CatalogoEstadoCivil estadoCivil = catalogoEstadoCivilRepository.findById(request.getEstadoCivilId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Estado civil no encontrado con ID: " + request.getEstadoCivilId()));
        personaFisica.setEstadoCivil(estadoCivil);

        // Actualizar datos de contacto
        DatosContacto datosContacto = personaFisica.getDatosContacto();
        if (datosContacto == null) {
            datosContacto = new DatosContacto();
        }
        datosContacto.setCorreo(request.getCorreo());
        datosContacto.setLada(request.getLada());
        datosContacto.setNumeroTelefono(request.getNumeroTelefono());
        datosContacto.setNumeroTelefono2(request.getNumeroTelefono2());
        personaFisica.setDatosContacto(datosContacto);

        // Actualizar domicilio
        Domicilio domicilio = personaFisica.getDomicilio();
        if (domicilio == null) {
            domicilio = new Domicilio();
        }
        domicilio.setCalle(request.getCalle());
        domicilio.setNoExterior(request.getNoExterior());
        domicilio.setNoInterior(request.getNoInterior());
        domicilio.setColonia(request.getColonia());
        domicilio.setMunicipio(request.getMunicipio());
        domicilio.setEstado(request.getEstado());
        domicilio.setCp(request.getCp());
        domicilio.setPais(request.getPais());
        personaFisica.setDomicilio(domicilio);

        // Actualizar información laboral
        InformacionLaboral infoLaboral = personaFisica.getInformacionLaboral();
        if (infoLaboral == null) {
            infoLaboral = new InformacionLaboral();
        }
        infoLaboral.setOcupacion(request.getOcupacion());
        infoLaboral.setEmpresa(request.getEmpresa());
        infoLaboral.setIngresoMensual(request.getIngresoMensual());
        personaFisica.setInformacionLaboral(infoLaboral);

        personaFisica = personaFisicaRepository.save(personaFisica);

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada para el cliente con ID: " + id));

        log.info("Cliente actualizado exitosamente");
        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    public ClienteResponse obtenerCliente(Long id) {
        log.info("Consultando cliente con ID: {}", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cliente no encontrado con ID: " + id));

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada para el cliente con ID: " + id));

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    public ClienteResponse obtenerClientePorCurp(String curp) {
        log.info("Consultando cliente con CURP: {}", curp);

        PersonaFisica personaFisica = personaFisicaRepository.findByCurp(curp)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cliente no encontrado con CURP: " + curp));

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(personaFisica.getId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada para el cliente con CURP: " + curp));

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    public ClienteResponse obtenerClientePorRfc(String rfc) {
        log.info("Consultando cliente con RFC: {}", rfc);

        PersonaFisica personaFisica = personaFisicaRepository.findByRfc(rfc)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cliente no encontrado con RFC: " + rfc));

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(personaFisica.getId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada para el cliente con RFC: " + rfc));

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    public ClienteResponse obtenerClientePorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cliente con número de cuenta: {}", numeroCuenta);

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada con número: " + numeroCuenta));

        PersonaFisica personaFisica = personaFisicaRepository.findById(cuentaBancaria.getPersonaFisica().getId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cliente no encontrado para la cuenta con número: " + numeroCuenta));

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
    @Transactional
    public ClienteResponse bloquearCuenta(Long id, boolean bloqueado) {
        log.info("{} cuenta del cliente con ID: {}", bloqueado ? "Bloqueando" : "Desbloqueando", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cliente no encontrado con ID: " + id));

        personaFisica.setCuentaBloqueada(bloqueado);
        personaFisica = personaFisicaRepository.save(personaFisica);

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada para el cliente con ID: " + id));

        return mapToResponse(personaFisica, cuentaBancaria);
    }

    @Override
    @Transactional
    public ClienteResponse bloquearLogin(Long id, boolean bloqueado) {
        log.info("{} login del cliente con ID: {}", bloqueado ? "Bloqueando" : "Desbloqueando", id);

        PersonaFisica personaFisica = personaFisicaRepository.findById(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cliente no encontrado con ID: " + id));

        personaFisica.setLoginBloqueado(bloqueado);
        personaFisica = personaFisicaRepository.save(personaFisica);

        CuentaBancaria cuentaBancaria = cuentaBancariaRepository.findByPersonaFisicaId(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cuenta bancaria no encontrada para el cliente con ID: " + id));

        return mapToResponse(personaFisica, cuentaBancaria);
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
        response.setCorreo(persona.getDatosContacto() != null ? persona.getDatosContacto().getCorreo() : null);
        response.setNumeroTelefono(persona.getDatosContacto() != null ? persona.getDatosContacto().getNumeroTelefono() : null);
        response.setCalle(persona.getDomicilio() != null ? persona.getDomicilio().getCalle() : null);
        response.setColonia(persona.getDomicilio() != null ? String.valueOf(persona.getDomicilio().getColonia()) : null);
        response.setMunicipio(persona.getDomicilio() != null ? String.valueOf(persona.getDomicilio().getMunicipio()) : null);
        response.setEstado(persona.getDomicilio() != null ? String.valueOf(persona.getDomicilio().getEstado()) : null);
        response.setCp(persona.getDomicilio() != null ? String.valueOf(persona.getDomicilio().getCp()) : null);
        response.setPais(persona.getDomicilio() != null ? String.valueOf(persona.getDomicilio().getPais()) : null);
        response.setOcupacion(persona.getInformacionLaboral() != null ? persona.getInformacionLaboral().getOcupacion() : null);
        response.setEmpresa(persona.getInformacionLaboral() != null ? persona.getInformacionLaboral().getEmpresa() : null);
        response.setIngresoMensual(persona.getInformacionLaboral() != null ? persona.getInformacionLaboral().getIngresoMensual() : null);
        response.setNumeroCuenta(cuenta != null ? cuenta.getNumeroCuenta() : null);
        response.setSaldo(cuenta != null ? cuenta.getSaldo() : null);
        response.setCuentaBloqueada(persona.isCuentaBloqueada());
        response.setLoginBloqueado(persona.isLoginBloqueado());
        return response;
    }
}
