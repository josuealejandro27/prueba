package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoColonia;
import com.proyecto.servicios.entity.catalogos.CatalogoMunicipio;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.repositorys.catalogos.CatalogoColoniaRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoMunicipioRepository;
import com.proyecto.servicios.service.CatalogoColoniaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class CatalogoColoniaServiceImpl implements CatalogoColoniaService {

    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";

    @Autowired
    private CatalogoColoniaRepository catalogoColoniaRepository;

    @Autowired
    private CatalogoMunicipioRepository catalogoMunicipioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoColonia> obtenerTodos() {
        log.info("Consultando todas las colonias del catálogo");
        return catalogoColoniaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoColonia obtenerPorId(Long id) {
        log.info("Consultando colonia con id: {}", id);
        return buscarPorId(id);
    }

    @Override
    @Transactional
    public CatalogoColonia crear(CatalogoColonia catalogoColonia) {
        log.info("Creando nueva colonia: {}", catalogoColonia.getNombre());
        CatalogoColonia nueva = new CatalogoColonia();
        nueva.setNombre(catalogoColonia.getNombre());
        nueva.setMunicipio(resolverMunicipio(catalogoColonia.getMunicipio()));
        return catalogoColoniaRepository.save(nueva);
    }

    @Override
    @Transactional
    public CatalogoColonia actualizar(Long id, CatalogoColonia catalogoColonia) {
        log.info("Actualizando colonia con id: {}", id);
        CatalogoColonia existente = buscarPorId(id);
        if (catalogoColonia.getNombre() != null) {
            existente.setNombre(catalogoColonia.getNombre());
        }
        if (catalogoColonia.getMunicipio() != null && catalogoColonia.getMunicipio().getId() != null) {
            existente.setMunicipio(resolverMunicipio(catalogoColonia.getMunicipio()));
        }
        return catalogoColoniaRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando colonia con id: {}", id);
        catalogoColoniaRepository.delete(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoColonia> buscarPorNombre(String nombre) {
        log.info("Buscando colonias por nombre: {}", nombre);
        return catalogoColoniaRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoColonia> obtenerPorMunicipioId(Long municipioId) {
        log.info("Consultando colonias del municipio con id: {}", municipioId);
        if (!catalogoMunicipioRepository.existsById(municipioId)) {
            throw new ValidationException(CODIGO_VALIDACION, "Municipio no encontrado con id: " + municipioId);
        }
        return catalogoColoniaRepository.findByMunicipioId(municipioId);
    }

    private CatalogoColonia buscarPorId(Long id) {
        return catalogoColoniaRepository.findById(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Colonia no encontrada con id: " + id));
    }

    private CatalogoMunicipio resolverMunicipio(CatalogoMunicipio municipio) {
        if (municipio == null || municipio.getId() == null) {
            throw new ValidationException(CODIGO_VALIDACION, "El municipio de la colonia es obligatorio (enviar su id)");
        }
        return catalogoMunicipioRepository.findById(municipio.getId())
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "Municipio no encontrado con id: " + municipio.getId()));
    }
}