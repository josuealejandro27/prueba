package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoEstado;
import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoRequest;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoResponse;
import com.proyecto.servicios.model.catalogo.CatalogoEstadoSearchRequest;
import com.proyecto.servicios.repositorys.catalogos.CatalogoEstadoRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoPaisRepository;
import com.proyecto.servicios.service.CatalogoEstadoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CatalogoEstadoServiceImpl implements CatalogoEstadoService {

    private final CatalogoEstadoRepository catalogoEstadoRepository;
    private final CatalogoPaisRepository catalogoPaisRepository;

    public CatalogoEstadoServiceImpl(CatalogoEstadoRepository catalogoEstadoRepository,
                                     CatalogoPaisRepository catalogoPaisRepository) {
        this.catalogoEstadoRepository = catalogoEstadoRepository;
        this.catalogoPaisRepository = catalogoPaisRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoEstadoResponse obtenerPorId(Long id) {
        log.info("Consultando estado con ID: {}", id);
        CatalogoEstado estado = buscarPorId(id);
        return mapToResponse(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogoEstadoResponse> buscar(CatalogoEstadoSearchRequest filtros) {
        String nombreLimpio = (filtros.getNombre() != null && !filtros.getNombre().isBlank())
                ? filtros.getNombre().trim() : null;
        log.info("Buscando estados con filtros - nombre: {}, paisId: {}", nombreLimpio, filtros.getPaisId());

        Specification<CatalogoEstado> spec = Specification.where(null);

        if (nombreLimpio != null) {
            // Coincidencia parcial, sin distinguir mayúsculas/minúsculas.
            String patron = "%" + nombreLimpio.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), patron));
        }

        if (filtros.getPaisId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("pais").get("id"), filtros.getPaisId()));
        }

        return catalogoEstadoRepository.findAll(spec, filtros.aPageable()).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CatalogoEstadoResponse crear(CatalogoEstadoRequest request) {
        log.info("Creando estado: {} (país ID: {})", request.getNombre(), request.getPaisId());
        CatalogoEstado estado = new CatalogoEstado();
        estado.setNombre(request.getNombre().trim());
        estado.setPais(buscarPais(request.getPaisId()));
        CatalogoEstado guardado = catalogoEstadoRepository.save(estado);
        log.info("Estado creado exitosamente con ID: {}", guardado.getId());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public CatalogoEstadoResponse actualizar(CatalogoEstadoActualizaRequest request) {
        log.info("Actualizando estado con ID: {}", request.getId());
        CatalogoEstado estado = buscarPorId(request.getId());
        estado.setNombre(request.getNombre().trim());
        estado.setPais(buscarPais(request.getPaisId()));
        CatalogoEstado actualizado = catalogoEstadoRepository.save(estado);
        log.info("Estado actualizado exitosamente con ID: {}", actualizado.getId());
        return mapToResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando estado con ID: {}", id);
        buscarPorId(id);
        catalogoEstadoRepository.deleteById(id);
    }

    private CatalogoEstado buscarPorId(Long id) {
        return catalogoEstadoRepository.findById(id)
                .orElseThrow(() -> new CatalogoException(ApiResponseEnum.NO_ENCONTRADO,
                        "Estado no encontrado con ID: " + id));
    }

    private CatalogoPais buscarPais(Long paisId) {
        return catalogoPaisRepository.findById(paisId)
                .orElseThrow(() -> new CatalogoException(ApiResponseEnum.NO_ENCONTRADO,
                        "País no encontrado con ID: " + paisId));
    }

    private CatalogoEstadoResponse mapToResponse(CatalogoEstado estado) {
        CatalogoEstadoResponse response = new CatalogoEstadoResponse();
        response.setId(estado.getId());
        response.setNombre(estado.getNombre());
        response.setPaisId(estado.getPais().getId());
        response.setPaisNombre(estado.getPais().getNombre());
        return response;
    }
}
