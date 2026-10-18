package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoGeneros;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroRequest;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroResponse;
import com.proyecto.servicios.model.catalogo.CatalogoGeneroSearchRequest;
import com.proyecto.servicios.repositorys.catalogos.CatalogoGenerosRepository;
import com.proyecto.servicios.service.CatalogoGeneroService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CatalogoGeneroServiceImpl implements CatalogoGeneroService {

    private final CatalogoGenerosRepository catalogoGenerosRepository;

    public CatalogoGeneroServiceImpl(CatalogoGenerosRepository catalogoGenerosRepository) {
        this.catalogoGenerosRepository = catalogoGenerosRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoGeneroResponse obtenerPorId(Long id) {
        log.info("Consultando género con ID: {}", id);
        return mapToResponse(buscarGenero(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogoGeneroResponse> buscar(CatalogoGeneroSearchRequest filtros) {
        String tipo = filtros.getTipo() != null && !filtros.getTipo().isBlank()
                ? filtros.getTipo().trim() : null;
        log.info("Buscando géneros con tipo: {}", tipo);

        Specification<CatalogoGeneros> spec = Specification.where(null);
        if (tipo != null) {
            String patron = "%" + tipo.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("tipo")), patron));
        }

        return catalogoGenerosRepository.findAll(spec, filtros.aPageable()).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CatalogoGeneroResponse crear(CatalogoGeneroRequest request) {
        log.info("Creando nuevo género: {}", request.getTipo());
        CatalogoGeneros genero = new CatalogoGeneros();
        genero.setTipo(request.getTipo());
        return mapToResponse(catalogoGenerosRepository.save(genero));
    }

    @Override
    @Transactional
    public CatalogoGeneroResponse actualizar(CatalogoGeneroActualizaRequest request) {
        log.info("Actualizando género con ID: {}", request.getId());
        CatalogoGeneros existente = buscarGenero(request.getId());
        existente.setTipo(request.getTipo());
        return mapToResponse(catalogoGenerosRepository.save(existente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando género con ID: {}", id);
        catalogoGenerosRepository.delete(buscarGenero(id));
    }

    private CatalogoGeneros buscarGenero(Long id) {
        return catalogoGenerosRepository.findById(id)
                .orElseThrow(() -> new CatalogoException(ApiResponseEnum.NO_ENCONTRADO,
                        "Género no encontrado con ID: " + id));
    }

    private CatalogoGeneroResponse mapToResponse(CatalogoGeneros genero) {
        CatalogoGeneroResponse response = new CatalogoGeneroResponse();
        response.setId(genero.getId());
        response.setTipo(genero.getTipo());
        return response;
    }
}
