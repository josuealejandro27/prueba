package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoEstadoCivil;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilResponse;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoCivilSearchRequest;
import com.proyecto.servicios.repositorys.catalogos.CatalogoEstadoCivilRepository;
import com.proyecto.servicios.service.CatalogoEstadoCivilService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CatalogoEstadoCivilServiceImpl implements CatalogoEstadoCivilService {

    private final CatalogoEstadoCivilRepository catalogoEstadoCivilRepository;

    public CatalogoEstadoCivilServiceImpl(CatalogoEstadoCivilRepository catalogoEstadoCivilRepository) {
        this.catalogoEstadoCivilRepository = catalogoEstadoCivilRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoEstadoCivilResponse obtenerPorId(Long id) {
        log.info("Consultando estado civil con ID: {}", id);
        return mapToResponse(buscarEstadoCivil(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogoEstadoCivilResponse> buscar(CatalogoEstadoCivilSearchRequest filtros) {
        String nombre = filtros.getNombre() != null && !filtros.getNombre().isBlank()
                ? filtros.getNombre().trim() : null;
        log.info("Buscando estados civiles con nombre: {}", nombre);

        Specification<CatalogoEstadoCivil> spec = Specification.where(null);
        if (nombre != null) {
            String patron = "%" + nombre.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), patron));
        }

        return catalogoEstadoCivilRepository.findAll(spec, filtros.aPageable()).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CatalogoEstadoCivilResponse crear(CatalogoEstadoCivilRequest request) {
        log.info("Creando nuevo estado civil: {}", request.getNombre());
        CatalogoEstadoCivil estadoCivil = new CatalogoEstadoCivil();
        estadoCivil.setNombre(request.getNombre());
        return mapToResponse(catalogoEstadoCivilRepository.save(estadoCivil));
    }

    @Override
    @Transactional
    public CatalogoEstadoCivilResponse actualizar(CatalogoEstadoCivilActualizaRequest request) {
        log.info("Actualizando estado civil con ID: {}", request.getId());
        CatalogoEstadoCivil existente = buscarEstadoCivil(request.getId());
        existente.setNombre(request.getNombre());
        return mapToResponse(catalogoEstadoCivilRepository.save(existente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando estado civil con ID: {}", id);
        catalogoEstadoCivilRepository.delete(buscarEstadoCivil(id));
    }

    private CatalogoEstadoCivil buscarEstadoCivil(Long id) {
        return catalogoEstadoCivilRepository.findById(id)
                .orElseThrow(() -> new CatalogoException(ApiResponseEnum.NO_ENCONTRADO,
                        "Estado civil no encontrado con ID: " + id));
    }

    private CatalogoEstadoCivilResponse mapToResponse(CatalogoEstadoCivil estadoCivil) {
        CatalogoEstadoCivilResponse response = new CatalogoEstadoCivilResponse();
        response.setId(estadoCivil.getId());
        response.setNombre(estadoCivil.getNombre());
        return response;
    }
}
