package com.dextre.primerpaso.registro;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioRegistro {

    private final JdbcTemplate plantillaJdbc;

    public RepositorioRegistro(JdbcTemplate plantillaJdbc) {
        this.plantillaJdbc = plantillaJdbc;
    }

    public long crearUsuario(String correo, String contrasenaCodificada) {
        return obtenerIdentificador(plantillaJdbc.queryForObject("""
                INSERT INTO users (email_user, password_hash_user,
                    terms_accepted_at_user, terms_version_user)
                VALUES (?, ?, now(), 'registro-v1')
                RETURNING id_user
                """, Long.class, correo, contrasenaCodificada));
    }

    public long resolverCarrera(String nombre) {
        return resolverCatalogo("""
                INSERT INTO careers (name_career) VALUES (?)
                ON CONFLICT ((lower(btrim(name_career))))
                DO UPDATE SET name_career = careers.name_career
                RETURNING id_career, active_career
                """, nombre, "La carrera seleccionada no está disponible.");
    }

    public long resolverHabilidad(String nombre) {
        return resolverCatalogo("""
                INSERT INTO skills (name_skill) VALUES (?)
                ON CONFLICT ((lower(btrim(name_skill))))
                DO UPDATE SET name_skill = skills.name_skill
                RETURNING id_skill, active_skill
                """, nombre, "Una habilidad seleccionada no está disponible.");
    }

    public long resolverArea(String nombre) {
        return resolverCatalogo("""
                INSERT INTO work_areas (name_work_area) VALUES (?)
                ON CONFLICT ((lower(btrim(name_work_area))))
                DO UPDATE SET name_work_area = work_areas.name_work_area
                RETURNING id_work_area, active_work_area
                """, nombre, "Un área de interés seleccionada no está disponible.");
    }

    public void crearPostulante(long idUsuario, long idCarrera, PerfilPostulante perfil) {
        plantillaJdbc.update("""
                INSERT INTO candidates (id_candidate, first_name_candidate,
                    last_name_candidate, institution_candidate, career_id_candidate,
                    academic_status_candidate, current_term_candidate,
                    graduation_year_candidate, marketing_opt_in_candidate)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, idUsuario, perfil.nombres(), perfil.apellidos(), perfil.institucion(),
                idCarrera, perfil.condicionAcademica(), perfil.cicloActual(),
                perfil.anioEgreso(), perfil.aceptaComunicaciones());
    }

    public long crearEmpresa(PerfilEmpresa perfil) {
        return obtenerIdentificador(plantillaJdbc.queryForObject("""
                INSERT INTO companies (trade_name_company, country_code_company,
                    tax_id_company, industry_company, city_company, website_company)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id_company
                """, Long.class, perfil.nombreComercial(), perfil.codigoPais(),
                perfil.identificacionFiscal(), perfil.sector(), perfil.ciudad(), perfil.sitioWeb()));
    }

    public void crearMiembroEmpresa(long idEmpresa, long idUsuario, MiembroEmpresa miembro) {
        plantillaJdbc.update("""
                INSERT INTO company_members (company_id_company_member, user_id_company_member,
                    role_company_member, first_name_company_member,
                    last_name_company_member, phone_company_member)
                VALUES (?, ?, 'administrador', ?, ?, ?)
                """, idEmpresa, idUsuario, miembro.nombres(), miembro.apellidos(), miembro.telefono());
    }

    public void vincularHabilidad(long idPostulante, long idHabilidad) {
        plantillaJdbc.update("""
                INSERT INTO candidate_skills (candidate_id_candidate_skill, skill_id_candidate_skill)
                VALUES (?, ?)
                """, idPostulante, idHabilidad);
    }

    public void vincularAreaPostulante(long idPostulante, long idArea) {
        plantillaJdbc.update("""
                INSERT INTO candidate_work_areas
                    (candidate_id_candidate_work_area, work_area_id_candidate_work_area)
                VALUES (?, ?)
                """, idPostulante, idArea);
    }

    public void vincularAreaEmpresa(long idEmpresa, long idArea) {
        plantillaJdbc.update("""
                INSERT INTO company_work_areas
                    (company_id_company_work_area, work_area_id_company_work_area)
                VALUES (?, ?)
                """, idEmpresa, idArea);
    }

    private long resolverCatalogo(String consulta, String nombre, String mensajeInactivo) {
        EntradaCatalogo entrada = plantillaJdbc.queryForObject(consulta,
                (resultado, numeroFila) -> new EntradaCatalogo(resultado.getLong(1), resultado.getBoolean(2)),
                nombre);
        if (entrada == null) {
            throw new IllegalStateException("No se pudo resolver el catálogo del registro.");
        }
        if (!entrada.activo()) {
            throw new IllegalArgumentException(mensajeInactivo);
        }
        return entrada.identificador();
    }

    private long obtenerIdentificador(Long identificador) {
        if (identificador == null) {
            throw new IllegalStateException("No se pudo completar el registro.");
        }
        return identificador;
    }

    private record EntradaCatalogo(long identificador, boolean activo) { }

    public record PerfilPostulante(String nombres, String apellidos, String institucion,
            String carrera, String condicionAcademica, Integer cicloActual, Integer anioEgreso,
            boolean aceptaComunicaciones) { }

    public record PerfilEmpresa(String nombreComercial, String codigoPais, String identificacionFiscal,
            String sector, String ciudad, String sitioWeb) { }

    public record MiembroEmpresa(String nombres, String apellidos, String telefono) { }
}
