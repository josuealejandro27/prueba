package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.exception.PersonaException;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.model.PersonasSearchRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.PersonaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class PersonasServiceImpl implements PersonaService {
    @Autowired
    private PersonasRepository personasRepository;
    @Override
    public PersonaResponse creaPersona(PersonasRequest personasRequest) {
        PersonaResponse person=new PersonaResponse();
     Personas persona=new Personas();
     persona.setNombre(personasRequest.getNombre());
     persona.setApellidoMaterno(personasRequest.getApellidoMaterno());
     persona.setApellidoP(personasRequest.getApellidoP());
     personasRepository.save(persona);
     person.setCodigo(1);
     person.setMensaje("Exito");
     BeanUtils.copyProperties(persona,person);

     return person;
    }

    @Override
    public GenericResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest) {
        GenericResponse genericResponse=new GenericResponse();

        Optional<Personas> existePersona=personasRepository.findByNombre(eliminaPersonaRequest.getNombre());
        if(existePersona.isPresent()){
            Personas personaElimina=existePersona.get();
            personasRepository.delete(personaElimina);
            genericResponse.setCodigo(0);
            genericResponse.setMensaje("La persona ha sido eliminada correctamente");

        }else{
            genericResponse.setCodigo(1);
            genericResponse.setMensaje("La persona no existe ");
        }
       return genericResponse;

    }

    @Override
    public GenericResponse actualizaPersona(PersonasRequest personasRequest) {
        GenericResponse genericResponse=new GenericResponse();
        Optional<Personas> existePersona=personasRepository.findByNombre(personasRequest.getNombre());
        if(existePersona.isPresent()){
            Personas personaActualiza=existePersona.get();
            personaActualiza.setApellidoP(personasRequest.getApellidoP());
            personaActualiza.setApellidoMaterno(personasRequest.getApellidoMaterno());
            personasRepository.save(personaActualiza);
            genericResponse.setCodigo(0);
            genericResponse.setMensaje("la persona ha sido actualizada correctamente");

        }else{
            genericResponse.setCodigo(1);
            genericResponse.setMensaje("La persona no existe ");
        }
        return genericResponse;
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaResponse obtenerPersona(String nombre) {
        String nombreLimpio = nombre == null ? "" : nombre.trim();
        log.info("Consultando persona por nombre: {}", nombreLimpio);

        Personas persona = personasRepository.findByNombre(nombreLimpio)
                .orElseThrow(() -> new PersonaException("PERSONA_ERROR",
                        "Persona no encontrada con nombre: " + nombreLimpio, HttpStatus.NOT_FOUND));

        return mapToResponse(persona);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PersonaResponse> buscarPersonas(PersonasSearchRequest filtros) {
        String nombre = limpiar(filtros.getNombre());
        String apellidoP = limpiar(filtros.getApellidoP());
        String apellidoMaterno = limpiar(filtros.getApellidoMaterno());
        log.info("Consultando personas con filtros - nombre: {}, apellidoP: {}, apellidoMaterno: {}",
                nombre, apellidoP, apellidoMaterno);

        Specification<Personas> spec = Specification.where(null);

        if (nombre != null) {
            // Coincidencia parcial, sin distinguir mayúsculas/minúsculas.
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
        }

        if (apellidoP != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("apellidoP")), "%" + apellidoP.toLowerCase() + "%"));
        }

        if (apellidoMaterno != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("apellidoMaterno")), "%" + apellidoMaterno.toLowerCase() + "%"));
        }

        return personasRepository.findAll(spec, filtros.aPageable())
                .map(this::mapToResponse);
    }

    private String limpiar(String valor) {
        return (valor != null && !valor.isBlank()) ? valor.trim() : null;
    }

    private PersonaResponse mapToResponse(Personas persona) {
        PersonaResponse response = new PersonaResponse();
        response.setCodigo(1);
        response.setMensaje("Exito");
        BeanUtils.copyProperties(persona, response);
        return response;
    }
}
