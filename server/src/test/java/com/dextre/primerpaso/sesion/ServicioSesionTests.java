package com.dextre.primerpaso.sesion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.dextre.primerpaso.registro.CodificadorContrasena;
import com.dextre.primerpaso.sesion.DatosSesion.SolicitudInicioSesion;
import com.dextre.primerpaso.sesion.RepositorioSesion.CredencialesUsuario;
import com.dextre.primerpaso.sesion.RepositorioSesion.PerfilEmpresa;
import com.dextre.primerpaso.sesion.RepositorioSesion.PerfilPostulante;
import com.dextre.primerpaso.sesion.RepositorioSesion.UsuarioCuenta;

class ServicioSesionTests {

    private RepositorioSesion repositorio;
    private CodificadorContrasena codificador;
    private ServicioSesion servicio;

    @BeforeEach
    void prepararServicio() {
        repositorio = mock(RepositorioSesion.class);
        codificador = mock(CodificadorContrasena.class);
        when(codificador.codificar(anyString())).thenReturn("hash-ficticio");
        servicio = new ServicioSesion(repositorio, codificador);
        when(repositorio.buscarCredencialesPorCorreo("ana@ejemplo.com"))
                .thenReturn(Optional.of(credenciales("activo")));
        when(codificador.verificar("Prueba123!", "hash-real")).thenReturn(true);
        when(repositorio.buscarPostulante(10L)).thenReturn(Optional.of(new PerfilPostulante("Ana", "López")));
        when(repositorio.buscarUsuarioPorId(10L))
                .thenReturn(Optional.of(new UsuarioCuenta(10L, "ana@ejemplo.com", "activo")));
    }

    @Test
    void normalizaCorreoYTipoSinModificarContrasena() {
        String contrasena = " contraseña con espacios 123 ";
        when(codificador.verificar(contrasena, "hash-real")).thenReturn(true);
        SolicitudInicioSesion solicitud = new SolicitudInicioSesion(" ANA@EJEMPLO.COM ", contrasena,
                " POSTULANTE ");

        var usuario = servicio.iniciarSesion(solicitud);

        verify(repositorio).buscarCredencialesPorCorreo("ana@ejemplo.com");
        verify(codificador).verificar(contrasena, "hash-real");
        assertEquals("postulante", usuario.tipoCuenta());
        assertEquals("Ana", usuario.nombres());
        assertEquals("López", usuario.apellidos());
        assertNull(usuario.idEmpresa());
        assertNull(usuario.rolEmpresa());
        assertFalse(solicitud.toString().contains(contrasena));
        assertFalse(credenciales("activo").toString().contains("hash-real"));
    }

    @Test
    void verificaHashFicticioParaCorreosInexistentesSinGenerarloEnCadaIntento() {
        when(repositorio.buscarCredencialesPorCorreo("nadie@ejemplo.com")).thenReturn(Optional.empty());
        SolicitudInicioSesion solicitud = new SolicitudInicioSesion("nadie@ejemplo.com", "Prueba123!", "postulante");

        assertEquals(mensajeGenerico(), assertThrows(AccesoNoAutorizadoException.class,
                () -> servicio.iniciarSesion(solicitud)).getMessage());
        assertThrows(AccesoNoAutorizadoException.class, () -> servicio.iniciarSesion(solicitud));

        verify(codificador, times(2)).verificar("Prueba123!", "hash-ficticio");
        verify(codificador, times(1)).codificar(anyString());
        verify(repositorio, never()).buscarPostulante(any(Long.class));
    }

    @Test
    void rechazaContrasenaIncorrectaSinConsultarPerfil() {
        var error = assertThrows(AccesoNoAutorizadoException.class,
                () -> servicio.iniciarSesion(new SolicitudInicioSesion("ana@ejemplo.com", "Incorrecta!", "postulante")));

        assertEquals(mensajeGenerico(), error.getMessage());
        verify(codificador).verificar("Incorrecta!", "hash-real");
        verify(repositorio, never()).buscarPostulante(any(Long.class));
    }

    @Test
    void rechazaUsuarioSuspendidoTrasVerificarLaContrasena() {
        when(repositorio.buscarCredencialesPorCorreo("ana@ejemplo.com"))
                .thenReturn(Optional.of(credenciales("suspendido")));

        var error = assertThrows(AccesoNoAutorizadoException.class, () -> servicio.iniciarSesion(solicitud("postulante")));

        assertEquals(mensajeGenerico(), error.getMessage());
        verify(codificador).verificar("Prueba123!", "hash-real");
        verify(repositorio, never()).buscarPostulante(any(Long.class));
    }

    @Test
    void rechazaTipoEmpresaSiLaCuentaSoloEsPostulante() {
        when(repositorio.buscarEmpresaActiva(10L, null)).thenReturn(Optional.empty());

        var error = assertThrows(AccesoNoAutorizadoException.class, () -> servicio.iniciarSesion(solicitud("empresa")));

        assertEquals(mensajeGenerico(), error.getMessage());
        verify(repositorio, never()).buscarPostulante(any(Long.class));
    }

    @Test
    void rechazaTipoPostulanteSiLaCuentaSoloEsEmpresa() {
        when(repositorio.buscarPostulante(10L)).thenReturn(Optional.empty());

        assertThrows(AccesoNoAutorizadoException.class, () -> servicio.iniciarSesion(solicitud("postulante")));

        verify(repositorio, never()).buscarEmpresaActiva(eq(10L), any());
    }

    @Test
    void iniciaSesionDeEmpresaConSuMembresiaYDatosDelRepresentante() {
        when(repositorio.buscarEmpresaActiva(10L, null))
                .thenReturn(Optional.of(new PerfilEmpresa(30L, "Empresa prueba", "Ana", "López", "administrador")));

        var usuario = servicio.iniciarSesion(solicitud("empresa"));

        assertEquals(30L, usuario.idEmpresa());
        assertEquals("Empresa prueba", usuario.nombreEmpresa());
        assertEquals("administrador", usuario.rolEmpresa());
        assertEquals("Ana", usuario.nombres());
    }

    @Test
    void permiteMembresiasAntiguasSinNombresSinExponerValoresNulos() {
        when(repositorio.buscarEmpresaActiva(10L, null))
                .thenReturn(Optional.of(new PerfilEmpresa(30L, "Empresa prueba", null, null, "reclutador")));

        var usuario = servicio.iniciarSesion(solicitud("empresa"));

        assertEquals("Empresa prueba", usuario.nombres());
        assertEquals("", usuario.apellidos());
        assertEquals("reclutador", usuario.rolEmpresa());
    }

    @Test
    void revalidaElUsuarioSinRecuperarHashesNiVerificarContrasenas() {
        var usuario = servicio.consultarUsuario(10L, "postulante", null);

        assertEquals(10L, usuario.idUsuario());
        verify(repositorio).buscarUsuarioPorId(10L);
        verify(repositorio, never()).buscarCredencialesPorCorreo(anyString());
        verify(codificador, never()).verificar(anyString(), anyString());
    }

    @Test
    void invalidaSesionSiElUsuarioFueSuspendido() {
        when(repositorio.buscarUsuarioPorId(10L))
                .thenReturn(Optional.of(new UsuarioCuenta(10L, "ana@ejemplo.com", "suspendido")));

        assertThrows(AccesoNoAutorizadoException.class, () -> servicio.consultarUsuario(10L, "postulante", null));

        verify(repositorio, never()).buscarPostulante(any(Long.class));
    }

    @Test
    void conservaLaEmpresaSeleccionadaAunqueExistaOtraEmpresaActiva() {
        when(repositorio.buscarEmpresaActiva(10L, 30L)).thenReturn(Optional.empty());
        when(repositorio.buscarEmpresaActiva(10L, null))
                .thenReturn(Optional.of(new PerfilEmpresa(40L, "Otra empresa", "Ana", "López", "reclutador")));

        assertThrows(AccesoNoAutorizadoException.class, () -> servicio.consultarUsuario(10L, "empresa", 30L));

        verify(repositorio).buscarEmpresaActiva(10L, 30L);
        verify(repositorio, never()).buscarEmpresaActiva(10L, null);
    }

    @Test
    void rechazaSesionesSinEmpresaOConIdentificadoresYTiposInvalidos() {
        assertThrows(AccesoNoAutorizadoException.class, () -> servicio.consultarUsuario(10L, "empresa", null));
        assertThrows(AccesoNoAutorizadoException.class, () -> servicio.consultarUsuario(10L, "empresa", 0L));
        assertThrows(AccesoNoAutorizadoException.class, () -> servicio.consultarUsuario(0L, "postulante", null));
        assertThrows(AccesoNoAutorizadoException.class, () -> servicio.consultarUsuario(10L, "administrador", null));
        assertThrows(AccesoNoAutorizadoException.class, () -> servicio.consultarUsuario(10L, "postulante", 30L));
        verify(repositorio, never()).buscarUsuarioPorId(any(Long.class));
    }

    @Test
    void rechazaSolicitudesNulasYContrasenasFueraDelContrato() {
        assertThrows(AccesoNoAutorizadoException.class, () -> servicio.iniciarSesion(null));
        for (String contrasena : List.of("", "corta", "        ", "x".repeat(129))) {
            assertThrows(AccesoNoAutorizadoException.class,
                    () -> servicio.iniciarSesion(new SolicitudInicioSesion("ana@ejemplo.com", contrasena, "postulante")));
        }
        verify(repositorio, never()).buscarCredencialesPorCorreo(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultaEmpresaExigeMembresiaYEmpresaActivasYElIdentificadorExacto() throws Exception {
        JdbcTemplate plantilla = mock(JdbcTemplate.class);
        ResultSet resultado = mock(ResultSet.class);
        when(resultado.getLong("id_company")).thenReturn(30L);
        when(resultado.getString("trade_name_company")).thenReturn("Empresa prueba");
        when(resultado.getString("role_company_member")).thenReturn("reclutador");
        when(plantilla.query(anyString(), any(RowMapper.class), eq(10L), eq(30L)))
                .thenAnswer(invocacion -> {
                    RowMapper<?> mapeador = invocacion.getArgument(1);
                    return List.of(mapeador.mapRow(resultado, 0));
                });

        var perfil = new RepositorioSesion(plantilla).buscarEmpresaActiva(10L, 30L).orElseThrow();

        ArgumentCaptor<String> consulta = ArgumentCaptor.forClass(String.class);
        verify(plantilla).query(consulta.capture(), any(RowMapper.class), eq(10L), eq(30L));
        assertTrue(consulta.getValue().contains("miembro.active_company_member = TRUE"));
        assertTrue(consulta.getValue().contains("empresa.active_company = TRUE"));
        assertTrue(consulta.getValue().contains("AND empresa.id_company = ?"));
        assertTrue(consulta.getValue().contains("ORDER BY empresa.id_company LIMIT 1"));
        assertEquals(30L, perfil.idEmpresa());
        assertEquals("reclutador", perfil.rol());
    }

    private SolicitudInicioSesion solicitud(String tipoCuenta) {
        return new SolicitudInicioSesion("ana@ejemplo.com", "Prueba123!", tipoCuenta);
    }

    private CredencialesUsuario credenciales(String estado) {
        return new CredencialesUsuario(10L, "ana@ejemplo.com", "hash-real", estado);
    }

    private String mensajeGenerico() {
        return "Correo, contraseña o tipo de cuenta incorrectos.";
    }
}
