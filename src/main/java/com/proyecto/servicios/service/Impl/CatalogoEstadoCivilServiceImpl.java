package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.repositorys.catalogos.CatalogoEstadoCivilRepository;
import com.proyecto.servicios.service.CatalogoEstadoCivilService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class CatalogoEstadoCivilServiceImpl implements CatalogoEstadoCivilService {

    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";

    @Autowired
    private CatalogoEstadoCivilRepository catalogoEstadoCivilRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoEstadoCivil> obtenerTodos() {
        log.info("Consultando todos los estados civiles del catálogo");
        return catalogoEstadoCivilRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoEstadoCivil obtenerPorId(Long id) {
        log.info("Consultando estado civil con id: {}", id);
        return buscarPorId(id);
    }

    @Override
    @Transactional
    public CatalogoEstadoCivil crear(CatalogoEstadoCivil catalogoEstadoCivil) {
        log.info("Creando nuevo estado civil: {}", catalogoEstadoCivil.getNombre());
        return catalogoEstadoCivilRepository.save(catalogoEstadoCivil);
    }

    @Override
    @Transactional
    public CatalogoEstadoCivil actualizar(Long id, CatalogoEstadoCivil catalogoEstadoCivil) {
        log.info("Actualizando estado civil con id: {}", id);
        CatalogoEstadoCivil existente = buscarPorId(id);
        if (catalogoEstadoCivil.getNombre() != null) {
            existente.setNombre(catalogoEstadoCivil.getNombre());
        }
        return catalogoEstadoCivilRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando estado civil con id: {}", id);
        catalogoEstadoCivilRepository.delete(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoEstadoCivil> buscarPorNombre(String nombre) {
        log.info("Buscando estados civiles por nombre: {}", nombre);
        return catalogoEstadoCivilRepository.findByNombreContainingIgnoreCase(nombre);
    }

    private CatalogoEstadoCivil buscarPorId(Long id) {
        return catalogoEstadoCivilRepository.findById(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Estado civil no encontrado con id: " + id));
    }
}