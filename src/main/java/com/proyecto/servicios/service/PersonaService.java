package com.proyecto.servicios.service;

import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.model.PersonasSearchRequest;
import org.springframework.data.domain.Page;

public interface PersonaService {
    PersonaResponse creaPersona(PersonasRequest personasRequest);
    GenericResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest);
    GenericResponse actualizaPersona(PersonasRequest personasRequest);
    PersonaResponse obtenerPersona(String nombre);
    Page<PersonaResponse> buscarPersonas(PersonasSearchRequest filtros);
}
