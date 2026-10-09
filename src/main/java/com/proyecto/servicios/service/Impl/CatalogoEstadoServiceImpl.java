package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoEstado;
import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.repositorys.catalogos.CatalogoEstadoRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoPaisRepository;
import com.proyecto.servicios.service.CatalogoEstadoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class CatalogoEstadoServiceImpl implements CatalogoEstadoService {

    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";

    @Autowired
    private CatalogoEstadoRepository catalogoEstadoRepository;

    @Autowired
    private CatalogoPaisRepository catalogoPaisRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoEstado> obtenerTodos() {
        log.info("Consultando todos los estados del catálogo");
        return catalogoEstadoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoEstado obtenerPorId(Long id) {
        log.info("Consultando estado con id: {}", id);
        return buscarPorId(id);
    }

    @Override
    @Transactional
    public CatalogoEstado crear(CatalogoEstado catalogoEstado) {
        log.info("Creando nuevo estado: {}", catalogoEstado.getNombre());
        CatalogoEstado nuevo = new CatalogoEstado();
        nuevo.setNombre(catalogoEstado.getNombre());
        nuevo.setPais(resolverPais(catalogoEstado.getPais()));
        return catalogoEstadoRepository.save(nuevo);
    }

    @Override
    @Transactional
    public CatalogoEstado actualizar(Long id, CatalogoEstado catalogoEstado) {
        log.info("Actualizando estado con id: {}", id);
        CatalogoEstado existente = buscarPorId(id);
        if (catalogoEstado.getNombre() != null) {
            existente.setNombre(catalogoEstado.getNombre());
        }
        if (catalogoEstado.getPais() != null && catalogoEstado.getPais().getId() != null) {
            existente.setPais(resolverPais(catalogoEstado.getPais()));
        }
        return catalogoEstadoRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando estado con id: {}", id);
        catalogoEstadoRepository.delete(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoEstado> buscarPorNombre(String nombre) {
        log.info("Buscando estados por nombre: {}", nombre);
        return catalogoEstadoRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoEstado> obtenerPorPaisId(Long paisId) {
        log.info("Consultando estados del país con id: {}", paisId);
        if (!catalogoPaisRepository.existsById(paisId)) {
            throw new ValidationException(CODIGO_VALIDACION, "País no encontrado con id: " + paisId);
        }
        return catalogoEstadoRepository.findByPaisId(paisId);
    }

    private CatalogoEstado buscarPorId(Long id) {
        return catalogoEstadoRepository.findById(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Estado no encontrado con id: " + id));
    }

    private CatalogoPais resolverPais(CatalogoPais pais) {
        if (pais == null || pais.getId() == null) {
            throw new ValidationException(CODIGO_VALIDACION, "El país del estado es obligatorio (enviar su id)");
        }
        return catalogoPaisRepository.findById(pais.getId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "País no encontrado con id: " + pais.getId()));
    }
}