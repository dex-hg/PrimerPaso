package com.dextre.primerpaso.registro;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dextre.primerpaso.registro.DatosRegistro.RespuestaRegistro;
import com.dextre.primerpaso.registro.DatosRegistro.SolicitudEmpresa;
import com.dextre.primerpaso.registro.DatosRegistro.SolicitudPostulante;

@RestController
@RequestMapping("/api/registro")
public class ControladorRegistro {

    private final ServicioRegistro servicio;

    public ControladorRegistro(ServicioRegistro servicio) {
        this.servicio = servicio;
    }

    @PostMapping(value = "/postulantes", consumes = "application/json")
    public ResponseEntity<RespuestaRegistro> registrarPostulante(
            @Valid @RequestBody SolicitudPostulante solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(servicio.registrarPostulante(solicitud));
    }

    @PostMapping(value = "/empresas", consumes = "application/json")
    public ResponseEntity<RespuestaRegistro> registrarEmpresa(
            @Valid @RequestBody SolicitudEmpresa solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(servicio.registrarEmpresa(solicitud));
    }
}
