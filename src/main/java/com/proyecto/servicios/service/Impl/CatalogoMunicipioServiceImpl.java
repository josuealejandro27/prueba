package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoEstado;
import com.proyecto.servicios.entity.catalogos.CatalogoMunicipio;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.repositorys.catalogos.CatalogoEstadoRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoMunicipioRepository;
import com.proyecto.servicios.service.CatalogoMunicipioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class CatalogoMunicipioServiceImpl implements CatalogoMunicipioService {

    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";

    @Autowired
    private CatalogoMunicipioRepository catalogoMunicipioRepository;

    @Autowired
    private CatalogoEstadoRepository catalogoEstadoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoMunicipio> obtenerTodos() {
        log.info("Consultando todos los municipios del catálogo");
        return catalogoMunicipioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoMunicipio obtenerPorId(Long id) {
        log.info("Consultando municipio con id: {}", id);
        return buscarPorId(id);
    }

    @Override
    @Transactional
    public CatalogoMunicipio crear(CatalogoMunicipio catalogoMunicipio) {
        log.info("Creando nuevo municipio: {}", catalogoMunicipio.getNombre());
        CatalogoMunicipio nuevo = new CatalogoMunicipio();
        nuevo.setNombre(catalogoMunicipio.getNombre());
        nuevo.setEstado(resolverEstado(catalogoMunicipio.getEstado()));
        return catalogoMunicipioRepository.save(nuevo);
    }

    @Override
    @Transactional
    public CatalogoMunicipio actualizar(Long id, CatalogoMunicipio catalogoMunicipio) {
        log.info("Actualizando municipio con id: {}", id);
        CatalogoMunicipio existente = buscarPorId(id);
        if (catalogoMunicipio.getNombre() != null) {
            existente.setNombre(catalogoMunicipio.getNombre());
        }
        if (catalogoMunicipio.getEstado() != null && catalogoMunicipio.getEstado().getId() != null) {
            existente.setEstado(resolverEstado(catalogoMunicipio.getEstado()));
        }
        return catalogoMunicipioRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando municipio con id: {}", id);
        catalogoMunicipioRepository.delete(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoMunicipio> buscarPorNombre(String nombre) {
        log.info("Buscando municipios por nombre: {}", nombre);
        return catalogoMunicipioRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoMunicipio> obtenerPorEstadoId(Long estadoId) {
        log.info("Consultando municipios del estado con id: {}", estadoId);
        if (!catalogoEstadoRepository.existsById(estadoId)) {
            throw new ValidationException(CODIGO_VALIDACION, "Estado no encontrado con id: " + estadoId);
        }
        return catalogoMunicipioRepository.findByEstadoId(estadoId);
    }

    private CatalogoMunicipio buscarPorId(Long id) {
        return catalogoMunicipioRepository.findById(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Municipio no encontrado con id: " + id));
    }

    private CatalogoEstado resolverEstado(CatalogoEstado estado) {
        if (estado == null || estado.getId() == null) {
            throw new ValidationException(CODIGO_VALIDACION, "El estado del municipio es obligatorio (enviar su id)");
        }
        return catalogoEstadoRepository.findById(estado.getId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Estado no encontrado con id: " + estado.getId()));
    }
}