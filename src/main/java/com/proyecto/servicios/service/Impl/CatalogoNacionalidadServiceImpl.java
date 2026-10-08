package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoNacionalidad;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.repositorys.catalogos.CatalogoNacionalidadRepository;
import com.proyecto.servicios.service.CatalogoNacionalidadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class CatalogoNacionalidadServiceImpl implements CatalogoNacionalidadService {

    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";

    @Autowired
    private CatalogoNacionalidadRepository catalogoNacionalidadRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoNacionalidad> obtenerTodos() {
        log.info("Consultando todas las nacionalidades del catálogo");
        return catalogoNacionalidadRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoNacionalidad obtenerPorId(Long id) {
        log.info("Consultando nacionalidad con id: {}", id);
        return buscarPorId(id);
    }

    @Override
    @Transactional
    public CatalogoNacionalidad crear(CatalogoNacionalidad catalogoNacionalidad) {
        log.info("Creando nueva nacionalidad: {}", catalogoNacionalidad.getNombre());
        return catalogoNacionalidadRepository.save(catalogoNacionalidad);
    }

    @Override
    @Transactional
    public CatalogoNacionalidad actualizar(Long id, CatalogoNacionalidad catalogoNacionalidad) {
        log.info("Actualizando nacionalidad con id: {}", id);
        CatalogoNacionalidad existente = buscarPorId(id);
        if (catalogoNacionalidad.getNombre() != null) {
            existente.setNombre(catalogoNacionalidad.getNombre());
        }
        return catalogoNacionalidadRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando nacionalidad con id: {}", id);
        catalogoNacionalidadRepository.delete(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoNacionalidad> buscarPorNombre(String nombre) {
        log.info("Buscando nacionalidades por nombre: {}", nombre);
        return catalogoNacionalidadRepository.findByNombreContainingIgnoreCase(nombre);
    }

    private CatalogoNacionalidad buscarPorId(Long id) {
        return catalogoNacionalidadRepository.findById(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Nacionalidad no encontrada con id: " + id));
    }
}