package com.dextre.primerpaso.sesion;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioSesion {

    private final JdbcTemplate plantillaJdbc;

    public RepositorioSesion(JdbcTemplate plantillaJdbc) {
        this.plantillaJdbc = plantillaJdbc;
    }

    public Optional<CredencialesUsuario> buscarCredencialesPorCorreo(String correo) {
        return plantillaJdbc.query("""
                SELECT id_user, email_user, password_hash_user, status_user
                FROM users WHERE email_user = ?
                """, (resultado, numeroFila) -> new CredencialesUsuario(resultado.getLong("id_user"),
                resultado.getString("email_user"), resultado.getString("password_hash_user"),
                resultado.getString("status_user")), correo).stream().findFirst();
    }

    public Optional<UsuarioCuenta> buscarUsuarioPorId(long idUsuario) {
        return plantillaJdbc.query("""
                SELECT id_user, email_user, status_user
                FROM users WHERE id_user = ?
                """, (resultado, numeroFila) -> new UsuarioCuenta(resultado.getLong("id_user"),
                resultado.getString("email_user"), resultado.getString("status_user")),
                idUsuario).stream().findFirst();
    }

    public Optional<PerfilPostulante> buscarPostulante(long idUsuario) {
        return plantillaJdbc.query("""
                SELECT first_name_candidate, last_name_candidate
                FROM candidates WHERE id_candidate = ?
                """, (resultado, numeroFila) -> new PerfilPostulante(
                resultado.getString("first_name_candidate"), resultado.getString("last_name_candidate")),
                idUsuario).stream().findFirst();
    }

    public Optional<PerfilEmpresa> buscarEmpresaActiva(long idUsuario, Long idEmpresa) {
        String consulta = """
                SELECT empresa.id_company, empresa.trade_name_company,
                    miembro.first_name_company_member, miembro.last_name_company_member,
                    miembro.role_company_member
                FROM company_members miembro
                JOIN companies empresa ON empresa.id_company = miembro.company_id_company_member
                WHERE miembro.user_id_company_member = ?
                    AND miembro.active_company_member = TRUE AND empresa.active_company = TRUE
                """;
        Object[] parametros;
        if (idEmpresa == null) {
            parametros = new Object[] { idUsuario };
        } else {
            consulta += " AND empresa.id_company = ?";
            parametros = new Object[] { idUsuario, idEmpresa };
        }
        consulta += " ORDER BY empresa.id_company LIMIT 1";
        return plantillaJdbc.query(consulta, (resultado, numeroFila) -> new PerfilEmpresa(
                resultado.getLong("id_company"), resultado.getString("trade_name_company"),
                resultado.getString("first_name_company_member"),
                resultado.getString("last_name_company_member"),
                resultado.getString("role_company_member")), parametros).stream().findFirst();
    }

    public record CredencialesUsuario(long idUsuario, String correo, String contrasenaCodificada,
            String estado) {

        @Override
        public String toString() {
            return "CredencialesUsuario[datos privados]";
        }
    }

    public record UsuarioCuenta(long idUsuario, String correo, String estado) {
    }

    public record PerfilPostulante(String nombres, String apellidos) {
    }

    public record PerfilEmpresa(long idEmpresa, String nombreEmpresa, String nombres, String apellidos,
            String rol) {
    }
}
