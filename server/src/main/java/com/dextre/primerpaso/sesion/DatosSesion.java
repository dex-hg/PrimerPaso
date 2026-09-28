package com.dextre.primerpaso.sesion;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class DatosSesion {

    private DatosSesion() {
    }

    public record SolicitudInicioSesion(
            @NotBlank @Email @Size(max = 254) String correo,
            @NotBlank @Size(min = 8, max = 128) String contrasena,
            @NotBlank @Pattern(regexp = "postulante|empresa") String tipoCuenta) {

        public SolicitudInicioSesion {
            correo = correo == null ? null : correo.strip().toLowerCase(Locale.ROOT);
            tipoCuenta = tipoCuenta == null ? null : tipoCuenta.strip().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return "SolicitudInicioSesion[datos privados]";
        }
    }

    public record UsuarioSesion(long idUsuario, String nombres, String apellidos, String correo,
            String tipoCuenta, Long idEmpresa, String nombreEmpresa, String rolEmpresa) {
    }
}
