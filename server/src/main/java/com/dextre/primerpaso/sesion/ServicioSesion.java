package com.dextre.primerpaso.sesion;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dextre.primerpaso.registro.CodificadorContrasena;
import com.dextre.primerpaso.sesion.DatosSesion.SolicitudInicioSesion;
import com.dextre.primerpaso.sesion.DatosSesion.UsuarioSesion;
import com.dextre.primerpaso.sesion.RepositorioSesion.CredencialesUsuario;

@Service
public class ServicioSesion {

    private static final String MENSAJE_ACCESO_INVALIDO = "Correo, contraseña o tipo de cuenta incorrectos.";
    private final RepositorioSesion repositorio;
    private final CodificadorContrasena codificador;
    private final String hashFicticio;

    public ServicioSesion(RepositorioSesion repositorio, CodificadorContrasena codificador) {
        this.repositorio = repositorio;
        this.codificador = codificador;
        hashFicticio = codificador.codificar(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public UsuarioSesion iniciarSesion(SolicitudInicioSesion solicitud) {
        validarSolicitud(solicitud);
        CredencialesUsuario credenciales = repositorio.buscarCredencialesPorCorreo(solicitud.correo())
                .orElse(null);
        String hash = credenciales == null ? hashFicticio : credenciales.contrasenaCodificada();
        boolean contrasenaValida = codificador.verificar(solicitud.contrasena(), hash);
        if (!contrasenaValida || credenciales == null || !"activo".equals(credenciales.estado())) {
            throw accesoInvalido();
        }
        return consultarPerfil(credenciales.idUsuario(), credenciales.correo(), solicitud.tipoCuenta(), null);
    }

    @Transactional(readOnly = true)
    public UsuarioSesion consultarUsuario(long idUsuario, String tipoCuenta, Long idEmpresa) {
        if (idUsuario <= 0 || !tipoCuentaValido(tipoCuenta)
                || ("empresa".equals(tipoCuenta) && (idEmpresa == null || idEmpresa <= 0))
                || ("postulante".equals(tipoCuenta) && idEmpresa != null)) {
            throw accesoInvalido();
        }
        var cuenta = repositorio.buscarUsuarioPorId(idUsuario).orElseThrow(this::accesoInvalido);
        if (!"activo".equals(cuenta.estado())) {
            throw accesoInvalido();
        }
        return consultarPerfil(idUsuario, cuenta.correo(), tipoCuenta, idEmpresa);
    }

    private UsuarioSesion consultarPerfil(long idUsuario, String correo, String tipoCuenta, Long idEmpresa) {
        if ("postulante".equals(tipoCuenta)) {
            var perfil = repositorio.buscarPostulante(idUsuario).orElseThrow(this::accesoInvalido);
            return new UsuarioSesion(idUsuario, perfil.nombres(), perfil.apellidos(), correo,
                    tipoCuenta, null, null, null);
        }
        var perfil = repositorio.buscarEmpresaActiva(idUsuario, idEmpresa).orElseThrow(this::accesoInvalido);
        String nombres = perfil.nombres() == null ? perfil.nombreEmpresa() : perfil.nombres();
        String apellidos = perfil.apellidos() == null ? "" : perfil.apellidos();
        return new UsuarioSesion(idUsuario, nombres, apellidos, correo, tipoCuenta,
                perfil.idEmpresa(), perfil.nombreEmpresa(), perfil.rol());
    }

    private void validarSolicitud(SolicitudInicioSesion solicitud) {
        if (solicitud == null || solicitud.correo() == null || solicitud.correo().isBlank()
                || solicitud.correo().length() > 254 || solicitud.contrasena() == null
                || solicitud.contrasena().isBlank() || solicitud.contrasena().length() < 8
                || solicitud.contrasena().length() > 128 || !tipoCuentaValido(solicitud.tipoCuenta())) {
            throw accesoInvalido();
        }
    }

    private boolean tipoCuentaValido(String tipoCuenta) {
        return "postulante".equals(tipoCuenta) || "empresa".equals(tipoCuenta);
    }

    private AccesoNoAutorizadoException accesoInvalido() {
        return new AccesoNoAutorizadoException(MENSAJE_ACCESO_INVALIDO);
    }
}
