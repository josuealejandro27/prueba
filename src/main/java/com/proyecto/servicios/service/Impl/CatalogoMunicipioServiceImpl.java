package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.catalogos.CatalogoEstado;
import com.proyecto.servicios.entity.catalogos.CatalogoMunicipio;
import com.proyecto.servicios.enums.ApiResponseEnum;
import com.proyecto.servicios.exception.CatalogoException;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioActualizaRequest;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioRequest;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioResponse;
import com.proyecto.servicios.model.catalogo.CatalogoMunicipioSearchRequest;
import com.proyecto.servicios.repositorys.catalogos.CatalogoEstadoRepository;
import com.proyecto.servicios.repositorys.catalogos.CatalogoMunicipioRepository;
import com.proyecto.servicios.service.CatalogoMunicipioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CatalogoMunicipioServiceImpl implements CatalogoMunicipioService {

    private final CatalogoMunicipioRepository catalogoMunicipioRepository;
    private final CatalogoEstadoRepository catalogoEstadoRepository;

    public CatalogoMunicipioServiceImpl(CatalogoMunicipioRepository catalogoMunicipioRepository,
                                        CatalogoEstadoRepository catalogoEstadoRepository) {
        this.catalogoMunicipioRepository = catalogoMunicipioRepository;
        this.catalogoEstadoRepository = catalogoEstadoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoMunicipioResponse obtenerPorId(Long id) {
        log.info("Consultando municipio con ID: {}", id);
        CatalogoMunicipio municipio = buscarPorId(id);
        return mapToResponse(municipio);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogoMunicipioResponse> buscar(CatalogoMunicipioSearchRequest filtros) {
        String nombreLimpio = (filtros.getNombre() != null && !filtros.getNombre().isBlank())
                ? filtros.getNombre().trim() : null;
        log.info("Buscando municipios con filtros - nombre: {}, estadoId: {}", nombreLimpio, filtros.getEstadoId());

        Specification<CatalogoMunicipio> spec = Specification.where(null);

        if (nombreLimpio != null) {
            // Coincidencia parcial, sin distinguir mayúsculas/minúsculas.
            String patron = "%" + nombreLimpio.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), patron));
        }

        if (filtros.getEstadoId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado").get("id"), filtros.getEstadoId()));
        }

        return catalogoMunicipioRepository.findAll(spec, filtros.aPageable()).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CatalogoMunicipioResponse crear(CatalogoMunicipioRequest request) {
        log.info("Creando municipio: {} (estado ID: {})", request.getNombre(), request.getEstadoId());
        CatalogoMunicipio municipio = new CatalogoMunicipio();
        municipio.setNombre(request.getNombre().trim());
        municipio.setEstado(buscarEstado(request.getEstadoId()));
        CatalogoMunicipio guardado = catalogoMunicipioRepository.save(municipio);
        log.info("Municipio creado exitosamente con ID: {}", guardado.getId());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public CatalogoMunicipioResponse actualizar(CatalogoMunicipioActualizaRequest request) {
        log.info("Actualizando municipio con ID: {}", request.getId());
        CatalogoMunicipio municipio = buscarPorId(request.getId());
        municipio.setNombre(request.getNombre().trim());
        municipio.setEstado(buscarEstado(request.getEstadoId()));
        CatalogoMunicipio actualizado = catalogoMunicipioRepository.save(municipio);
        log.info("Municipio actualizado exitosamente con ID: {}", actualizado.getId());
        return mapToResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("Eliminando municipio con ID: {}", id);
        buscarPorId(id);
        catalogoMunicipioRepository.deleteById(id);
    }

    private CatalogoMunicipio buscarPorId(Long id) {
        return catalogoMunicipioRepository.findById(id)
                .orElseThrow(() -> new CatalogoException(ApiResponseEnum.NO_ENCONTRADO,
                        "Municipio no encontrado con ID: " + id));
    }

    private CatalogoEstado buscarEstado(Long estadoId) {
        return catalogoEstadoRepository.findById(estadoId)
                .orElseThrow(() -> new CatalogoException(ApiResponseEnum.NO_ENCONTRADO,
                        "Estado no encontrado con ID: " + estadoId));
    }

    private CatalogoMunicipioResponse mapToResponse(CatalogoMunicipio municipio) {
        CatalogoMunicipioResponse response = new CatalogoMunicipioResponse();
        response.setId(municipio.getId());
        response.setNombre(municipio.getNombre());
        response.setEstadoId(municipio.getEstado().getId());
        response.setEstadoNombre(municipio.getEstado().getNombre());
        return response;
    }
}
