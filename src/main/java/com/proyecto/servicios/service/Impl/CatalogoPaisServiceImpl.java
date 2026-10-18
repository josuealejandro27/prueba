package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoPais;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.model.catalogo.CatalogoPaisActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoPaisRequest;
import com.proyecto.servicios.model.catalogo.CatalogoPaisResponse;
import com.proyecto.servicios.model.catalogo.CatalogoPaisSearchRequest;
import com.proyecto.servicios.repositorys.catalogos.CatalogoPaisRepository;
import com.proyecto.servicios.service.CatalogoPaisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CatalogoPaisServiceImpl implements CatalogoPaisService {

    private final CatalogoPaisRepository catalogoPaisRepository;

    public CatalogoPaisServiceImpl(CatalogoPaisRepository catalogoPaisRepository) {
        this.catalogoPaisRepository = catalogoPaisRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoPaisResponse obtenerPorId(Long id) {
        log.info("Consultando país con ID: {}", id);
        return mapToResponse(buscarPais(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogoPaisResponse> buscar(CatalogoPaisSearchRequest filtros) {
        String nombre = filtros.getNombre() != null && !filtros.getNombre().isBlank()
                ? filtros.getNombre().trim() : null;
        log.info("Buscando países con nombre: {}", nombre);

        Specification<CatalogoPais> spec = Specification.where(null);
        if (nombre != null) {
            String patron = "%" + nombre.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), patron));
        }

        return catalogoPaisRepository.findAll(spec, filtros.aPageable()).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CatalogoPaisResponse crear(CatalogoPaisRequest request) {
        log.info("Creando nuevo país: {}", request.getNombre());
        CatalogoPais pais = new CatalogoPais();
        pais.setNombre(request.getNombre());
        return mapToResponse(catalogoPaisRepository.save(pais));
    }

    @Override
    @Transactional
    public CatalogoPaisResponse actualizar(CatalogoPaisActualizaRequest request) {
        log.info("Actualizando país con ID: {}", request.getId());
        CatalogoPais existente = buscarPais(request.getId());
        existente.setNombre(request.getNombre());
        return mapToResponse(catalogoPaisRepository.save(existente));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando país con ID: {}", id);
        catalogoPaisRepository.delete(buscarPais(id));
    }

    private CatalogoPais buscarPais(Long id) {
        return catalogoPaisRepository.findById(id)
                .orElseThrow(() -> new CatalogoException(ApiResponseEnum.NO_ENCONTRADO,
                        "País no encontrado con ID: " + id));
    }

    private CatalogoPaisResponse mapToResponse(CatalogoPais pais) {
        CatalogoPaisResponse response = new CatalogoPaisResponse();
        response.setId(pais.getId());
        response.setNombre(pais.getNombre());
        return response;
    }
}
