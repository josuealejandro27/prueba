package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.repositorys.catalogos.CatalogoGenerosRepository;
import com.proyecto.servicios.service.CatalogoGeneroService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CatalogoGeneroServiceImpl implements CatalogoGeneroService {

    @Autowired
    private CatalogoGenerosRepository catalogoGenerosRepository;

    @Override
    public List<CatalogoGeneros> obtenerTodos() {
        log.info("Consultando todos los géneros del catálogo");
        return catalogoGenerosRepository.findAll();
    }

    @Override
    public CatalogoGeneros obtenerPorId(Long id) {
        log.info("Consultando género con id: {}", id);
        return catalogoGenerosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Género no encontrado con id: " + id));
    }

    @Override
    public CatalogoGeneros crear(CatalogoGeneros catalogoGeneros) {
        log.info("Creando nuevo género: {}", catalogoGeneros.getTipo());
        return catalogoGenerosRepository.save(catalogoGeneros);
    }

    @Override
    public CatalogoGeneros actualizar(Long id, CatalogoGeneros catalogoGeneros) {
        log.info("Actualizando género con id: {}", id);
        CatalogoGeneros existente = catalogoGenerosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Género no encontrado con id: " + id));
        existente.setTipo(catalogoGeneros.getTipo());
        return catalogoGenerosRepository.save(existente);
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando género con id: {}", id);
        CatalogoGeneros existente = catalogoGenerosRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Género no encontrado con id: " + id));
        catalogoGenerosRepository.delete(existente);
    }
}
