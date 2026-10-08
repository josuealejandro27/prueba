package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.exception.ValidationException;
import com.proyecto.servicios.repositorys.catalogos.CatalogoPaisRepository;
import com.proyecto.servicios.service.CatalogoPaisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class CatalogoPaisServiceImpl implements CatalogoPaisService {

    private static final String CODIGO_VALIDACION = "VALIDATION_ERROR";

    @Autowired
    private CatalogoPaisRepository catalogoPaisRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoPais> obtenerTodos() {
        log.info("Consultando todos los países del catálogo");
        return catalogoPaisRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoPais obtenerPorId(Long id) {
        log.info("Consultando país con id: {}", id);
        return buscarPorId(id);
    }

    @Override
    @Transactional
    public CatalogoPais crear(CatalogoPais catalogoPais) {
        log.info("Creando nuevo país: {}", catalogoPais.getNombre());
        return catalogoPaisRepository.save(catalogoPais);
    }

    @Override
    @Transactional
    public CatalogoPais actualizar(Long id, CatalogoPais catalogoPais) {
        log.info("Actualizando país con id: {}", id);
        CatalogoPais existente = buscarPorId(id);
        if (catalogoPais.getNombre() != null) {
            existente.setNombre(catalogoPais.getNombre());
        }
        return catalogoPaisRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando país con id: {}", id);
        catalogoPaisRepository.delete(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoPais> buscarPorNombre(String nombre) {
        log.info("Buscando países por nombre: {}", nombre);
        return catalogoPaisRepository.findByNombreContainingIgnoreCase(nombre);
    }

    private CatalogoPais buscarPorId(Long id) {
        return catalogoPaisRepository.findById(id)
                .orElseThrow(() -> new ValidationException(CODIGO_VALIDACION, "País no encontrado con id: " + id));
    }
}