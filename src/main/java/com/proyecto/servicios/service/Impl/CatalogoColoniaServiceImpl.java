package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoColonia;
import com.proyecto.servicios.entity.catalogos.CatalogoMunicipio;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaResponse;
import com.proyecto.servicios.model.catalogo.CatalogoColoniaSearchRequest;
import com.proyecto.servicios.repositorys.catalogos.CatalogoColoniaRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoMunicipioRepository;
import com.proyecto.servicios.service.CatalogoColoniaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CatalogoColoniaServiceImpl implements CatalogoColoniaService {

    private final CatalogoColoniaRepository catalogoColoniaRepository;
    private final CatalogoMunicipioRepository catalogoMunicipioRepository;

    public CatalogoColoniaServiceImpl(CatalogoColoniaRepository catalogoColoniaRepository,
                                      CatalogoMunicipioRepository catalogoMunicipioRepository) {
        this.catalogoColoniaRepository = catalogoColoniaRepository;
        this.catalogoMunicipioRepository = catalogoMunicipioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoColoniaResponse obtenerPorId(Long id) {
        log.info("Consultando colonia con ID: {}", id);
        CatalogoColonia colonia = buscarPorId(id);
        return mapToResponse(colonia);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogoColoniaResponse> buscar(CatalogoColoniaSearchRequest filtros) {
        String nombreLimpio = (filtros.getNombre() != null && !filtros.getNombre().isBlank())
                ? filtros.getNombre().trim() : null;
        log.info("Buscando colonias con filtros - nombre: {}, municipioId: {}", nombreLimpio, filtros.getMunicipioId());

        Specification<CatalogoColonia> spec = Specification.where(null);

        if (nombreLimpio != null) {
            // Coincidencia parcial, sin distinguir mayúsculas/minúsculas.
            String patron = "%" + nombreLimpio.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), patron));
        }

        if (filtros.getMunicipioId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("municipio").get("id"), filtros.getMunicipioId()));
        }

        return catalogoColoniaRepository.findAll(spec, filtros.aPageable()).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CatalogoColoniaResponse crear(CatalogoColoniaRequest request) {
        log.info("Creando colonia: {} (municipio ID: {})", request.getNombre(), request.getMunicipioId());
        CatalogoColonia colonia = new CatalogoColonia();
        colonia.setNombre(request.getNombre().trim());
        colonia.setMunicipio(buscarMunicipio(request.getMunicipioId()));
        CatalogoColonia guardada = catalogoColoniaRepository.save(colonia);
        log.info("Colonia creada exitosamente con ID: {}", guardada.getId());
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public CatalogoColoniaResponse actualizar(CatalogoColoniaActualizaRequest request) {
        log.info("Actualizando colonia con ID: {}", request.getId());
        CatalogoColonia colonia = buscarPorId(request.getId());
        colonia.setNombre(request.getNombre().trim());
        colonia.setMunicipio(buscarMunicipio(request.getMunicipioId()));
        CatalogoColonia actualizada = catalogoColoniaRepository.save(colonia);
        log.info("Colonia actualizada exitosamente con ID: {}", actualizada.getId());
        return mapToResponse(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando colonia con ID: {}", id);
        buscarPorId(id);
        catalogoColoniaRepository.deleteById(id);
    }

    private CatalogoColonia buscarPorId(Long id) {
        return catalogoColoniaRepository.findById(id)
                .orElseThrow(() -> new CatalogoException(ApiResponseEnum.NO_ENCONTRADO,
                        "Colonia no encontrada con ID: " + id));
    }

    private CatalogoMunicipio buscarMunicipio(Long municipioId) {
        return catalogoMunicipioRepository.findById(municipioId)
                .orElseThrow(() -> new CatalogoException(ApiResponseEnum.NO_ENCONTRADO,
                        "Municipio no encontrado con ID: " + municipioId));
    }

    private CatalogoColoniaResponse mapToResponse(CatalogoColonia colonia) {
        CatalogoColoniaResponse response = new CatalogoColoniaResponse();
        response.setId(colonia.getId());
        response.setNombre(colonia.getNombre());
        response.setMunicipioId(colonia.getMunicipio().getId());
        response.setMunicipioNombre(colonia.getMunicipio().getNombre());
        return response;
    }
}
