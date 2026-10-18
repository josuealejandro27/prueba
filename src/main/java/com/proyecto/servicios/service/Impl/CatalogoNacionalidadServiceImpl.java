package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoNacionalidad;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadRequest;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadResponse;
import com.proyecto.servicios.model.catalogo.CatalogoNacionalidadSearchRequest;
import com.proyecto.servicios.repositorys.catalogos.CatalogoNacionalidadRepository;
import com.proyecto.servicios.service.CatalogoNacionalidadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CatalogoNacionalidadServiceImpl implements CatalogoNacionalidadService {

    private final CatalogoNacionalidadRepository catalogoNacionalidadRepository;

    public CatalogoNacionalidadServiceImpl(CatalogoNacionalidadRepository catalogoNacionalidadRepository) {
        this.catalogoNacionalidadRepository = catalogoNacionalidadRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoNacionalidadResponse obtenerPorId(Long id) {
        log.info("Consultando nacionalidad con ID: {}", id);
        return mapToResponse(buscarNacionalidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogoNacionalidadResponse> buscar(CatalogoNacionalidadSearchRequest filtros) {
        String nombre = filtros.getNombre() != null && !filtros.getNombre().isBlank()
                ? filtros.getNombre().trim() : null;
        log.info("Buscando nacionalidades con nombre: {}", nombre);

        Specification<CatalogoNacionalidad> spec = Specification.where(null);
        if (nombre != null) {
            String patron = "%" + nombre.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), patron));
        }

        return catalogoNacionalidadRepository.findAll(spec, filtros.aPageable()).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CatalogoNacionalidadResponse crear(CatalogoNacionalidadRequest request) {
        log.info("Creando nueva nacionalidad: {}", request.getNombre());
        CatalogoNacionalidad nacionalidad = new CatalogoNacionalidad();
        nacionalidad.setNombre(request.getNombre());
        return mapToResponse(catalogoNacionalidadRepository.save(nacionalidad));
    }

    @Override
    @Transactional
    public CatalogoNacionalidadResponse actualizar(CatalogoNacionalidadActualizaRequest request) {
        log.info("Actualizando nacionalidad con ID: {}", request.getId());
        CatalogoNacionalidad existente = buscarNacionalidad(request.getId());
        existente.setNombre(request.getNombre());
        return mapToResponse(catalogoNacionalidadRepository.save(existente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando nacionalidad con ID: {}", id);
        catalogoNacionalidadRepository.delete(buscarNacionalidad(id));
    }

    private CatalogoNacionalidad buscarNacionalidad(Long id) {
        return catalogoNacionalidadRepository.findById(id)
                .orElseThrow(() -> new CatalogoException(ApiResponseEnum.NO_ENCONTRADO,
                        "Nacionalidad no encontrada con ID: " + id));
    }

    private CatalogoNacionalidadResponse mapToResponse(CatalogoNacionalidad nacionalidad) {
        CatalogoNacionalidadResponse response = new CatalogoNacionalidadResponse();
        response.setId(nacionalidad.getId());
        response.setNombre(nacionalidad.getNombre());
        return response;
    }
}
