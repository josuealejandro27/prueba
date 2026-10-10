package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.clientes.CuentaBancaria;
import com.proyecto.servicios.entity.clientes.PersonaFisica;
import com.proyecto.servicios.exception.CuentaException;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.model.clientes.CuentaResponse;
import com.proyecto.servicios.model.clientes.CuentaUpdateRequest;
import com.proyecto.servicios.repositorys.clientes.CuentaBancariaRepository;
import com.proyecto.servicios.repositorys.clientes.PersonaFisicaRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";
    private static final String CODIGO_CUENTA = "CUENTA_ERROR";
    private static final String ESTATUS_ACTIVA = "ACTIVA";

    @Autowired
    private CuentaBancariaRepository cuentaBancariaRepository;

    @Autowired
    private PersonaFisicaRepository personaFisicaRepository;

    @Override
    @Transactional(readOnly = true)
    public CuentaResponse obtenerCuentaPorNumero(String numeroCuenta) {
        log.info("Consultando cuenta con número: {}", numeroCuenta);

        CuentaBancaria cuenta = cuentaBancariaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaException(CODIGO_CUENTA, "Cuenta no encontrada con número: " + numeroCuenta, HttpStatus.NOT_FOUND));

        return mapToResponse(cuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> buscarCuentas(Long clienteId, Boolean activas) {
        log.info("Consultando cuentas con filtros - clienteId: {}, activas: {}", clienteId, activas);

        Specification<CuentaBancaria> spec = Specification.where(null);

        if (clienteId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("personaFisica").get("id"), clienteId));
        }

        if (activas != null) {
            spec = spec.and((root, query, cb) -> activas
                    ? cb.equal(cb.upper(root.get("estatus")), ESTATUS_ACTIVA)
                    : cb.notEqual(cb.upper(root.get("estatus")), ESTATUS_ACTIVA));
        }

        return cuentaBancariaRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CuentaResponse actualizarCuenta(String numeroCuenta, CuentaUpdateRequest request) {
        log.info("Actualizando cuenta con número: {}", numeroCuenta);

        CuentaBancaria cuenta = cuentaBancariaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaException(CODIGO_CUENTA, "Cuenta no encontrada con número: " + numeroCuenta, HttpStatus.NOT_FOUND));

        if (request.getSaldo() != null) {
            cuenta.setSaldo(request.getSaldo());
        }

        if (request.getEstatus() != null) {
            cuenta.setEstatus(request.getEstatus());
        }

        cuenta = cuentaBancariaRepository.save(cuenta);

        log.info("Cuenta actualizada exitosamente");
        return mapToResponse(cuenta);
    }

    @Override
    @Transactional
    public CuentaResponse crearCuenta(Long clienteId, String numeroCuenta) {
        log.info("Creando cuenta para cliente con ID: {}", clienteId);

        PersonaFisica persona = personaFisicaRepository.findById(clienteId)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Cliente no encontrado con ID: " + clienteId));

        if (!persona.isActivo()) {
            throw new ValidationException(CODIGO_VALIDACION, "No se puede crear cuenta para un cliente inactivo");
        }

        if (numeroCuenta != null && cuentaBancariaRepository.existsByNumeroCuenta(numeroCuenta)) {
            throw new CuentaException(CODIGO_CUENTA, "Ya existe una cuenta con el número: " + numeroCuenta, HttpStatus.CONFLICT);
        }

        CuentaBancaria cuenta = new CuentaBancaria();
        cuenta.setNumeroCuenta(numeroCuenta != null ? numeroCuenta : generarNumeroCuenta());
        cuenta.setSaldo(BigDecimal.ZERO);
        cuenta.setFechaApertura(LocalDateTime.now());
        cuenta.setPersonaFisica(persona);
        cuenta.setEstatus(ESTATUS_ACTIVA);

        cuenta = cuentaBancariaRepository.save(cuenta);

        log.info("Cuenta creada exitosamente con número: {}", cuenta.getNumeroCuenta());
        return mapToResponse(cuenta);
    }

    private String generarNumeroCuenta() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private CuentaResponse mapToResponse(CuentaBancaria cuenta) {
        CuentaResponse response = new CuentaResponse();
        response.setId(cuenta.getId());
        response.setNumeroCuenta(cuenta.getNumeroCuenta());
        response.setSaldo(cuenta.getSaldo());
        response.setFechaApertura(cuenta.getFechaApertura());
        response.setEstatus(cuenta.getEstatus());
        response.setClienteId(cuenta.getPersonaFisica().getId());
        response.setNombreCliente(cuenta.getPersonaFisica().getNombre() + " " + cuenta.getPersonaFisica().getApellidoPaterno());
        return response;
    }
}
