package com.dextre.primerpaso.sesion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.dextre.primerpaso.sesion.DatosSesion.SolicitudInicioSesion;
import com.dextre.primerpaso.sesion.DatosSesion.UsuarioSesion;

@RestController
@RequestMapping("/api/sesion")
public class ControladorSesion {

    private static final String ATRIBUTO_USUARIO = "primerpaso.idUsuario";
    private static final String ATRIBUTO_TIPO = "primerpaso.tipoCuenta";
    private static final String ATRIBUTO_EMPRESA = "primerpaso.idEmpresa";
    private static final String ATRIBUTO_CSRF = "primerpaso.tokenCsrf";
    private final SecureRandom aleatorio = new SecureRandom();
    private final ServicioSesion servicio;
    private final boolean cookieSegura;

    public ControladorSesion(ServicioSesion servicio,
            @Value("${server.servlet.session.cookie.secure:false}") boolean cookieSegura) {
        this.servicio = servicio;
        this.cookieSegura = cookieSegura;
    }

    @GetMapping("/seguridad")
    public ResponseEntity<SeguridadSesion> obtenerSeguridad(HttpServletRequest solicitud) {
        HttpSession sesion = solicitud.getSession(true);
        sesion.setMaxInactiveInterval(1800);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new SeguridadSesion(obtenerToken(sesion)));
    }

    @PostMapping(consumes = "application/json")
    public ResponseEntity<UsuarioSesion> iniciarSesion(@Valid @RequestBody SolicitudInicioSesion datos,
            @RequestHeader(value = "X-CSRF-Token", required = false) String token,
            HttpServletRequest solicitud) {
        HttpSession sesionAnterior = solicitud.getSession(false);
        comprobarToken(sesionAnterior, token);
        UsuarioSesion usuario = servicio.iniciarSesion(datos);
        sesionAnterior.invalidate();
        HttpSession sesion = solicitud.getSession(true);
        sesion.setMaxInactiveInterval(1800);
        sesion.setAttribute(ATRIBUTO_USUARIO, usuario.idUsuario());
        sesion.setAttribute(ATRIBUTO_TIPO, usuario.tipoCuenta());
        if (usuario.idEmpresa() != null) {
            sesion.setAttribute(ATRIBUTO_EMPRESA, usuario.idEmpresa());
        }
        obtenerToken(sesion);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(usuario);
    }

    @GetMapping
    public ResponseEntity<UsuarioSesion> consultarSesion(HttpServletRequest solicitud,
            HttpServletResponse respuesta) {
        HttpSession sesion = solicitud.getSession(false);
        if (sesion == null) {
            throw new AccesoNoAutorizadoException("Inicia sesión para continuar.");
        }
        try {
            if (!(sesion.getAttribute(ATRIBUTO_USUARIO) instanceof Long idUsuario)
                    || !(sesion.getAttribute(ATRIBUTO_TIPO) instanceof String tipoCuenta)) {
                throw new AccesoNoAutorizadoException("Inicia sesión para continuar.");
            }
            Object empresa = sesion.getAttribute(ATRIBUTO_EMPRESA);
            Long idEmpresa = empresa instanceof Long identificador ? identificador : null;
            if ("empresa".equals(tipoCuenta) && idEmpresa == null) {
                throw new AccesoNoAutorizadoException("Inicia sesión para continuar.");
            }
            UsuarioSesion usuario = servicio.consultarUsuario(idUsuario, tipoCuenta, idEmpresa);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(usuario);
        } catch (AccesoNoAutorizadoException excepcion) {
            sesion.invalidate();
            borrarCookie(respuesta);
            throw excepcion;
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> cerrarSesion(
            @RequestHeader(value = "X-CSRF-Token", required = false) String token,
            HttpServletRequest solicitud, HttpServletResponse respuesta) {
        HttpSession sesion = solicitud.getSession(false);
        comprobarToken(sesion, token);
        sesion.invalidate();
        borrarCookie(respuesta);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private String obtenerToken(HttpSession sesion) {
        if (sesion.getAttribute(ATRIBUTO_CSRF) instanceof String token) {
            return token;
        }
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sesion.setAttribute(ATRIBUTO_CSRF, token);
        return token;
    }

    private void comprobarToken(HttpSession sesion, String recibido) {
        Object guardado = sesion == null ? null : sesion.getAttribute(ATRIBUTO_CSRF);
        if (!(guardado instanceof String esperado) || recibido == null || recibido.length() != 43
                || !MessageDigest.isEqual(esperado.getBytes(StandardCharsets.US_ASCII),
                        recibido.getBytes(StandardCharsets.US_ASCII))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La solicitud de sesión no es válida. Recarga la página e inténtalo nuevamente.");
        }
    }

    private void borrarCookie(HttpServletResponse respuesta) {
        respuesta.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("PRIMERPASO_SESION", "")
                .path("/").httpOnly(true).sameSite("Strict").secure(cookieSegura)
                .maxAge(Duration.ZERO).build().toString());
    }

    public record SeguridadSesion(String tokenCsrf) {
    }
}
