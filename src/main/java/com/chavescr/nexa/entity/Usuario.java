package com.chavescr.nexa.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 60)
    private String usuario; // nombre de usuario (ej: alejandro.chaves)

    @Column(unique = true, length = 30)
    private String cedula; // cédula de identidad costarricense

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean activo = true;

    // Many-to-Many con Rol
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    // Many-to-Many con Institucion
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuario_instituciones", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "institucion_id"))
    private Set<Institucion> instituciones = new HashSet<>();

    // Many-to-Many auto-referencial: un padre puede tener varios estudiantes asignados
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "padres_estudiantes", joinColumns = @JoinColumn(name = "padre_id"), inverseJoinColumns = @JoinColumn(name = "estudiante_id"))
    private Set<Usuario> estudiantes = new HashSet<>();

    // Lado inverso: los padres asignados a este estudiante
    @ManyToMany(mappedBy = "estudiantes")
    private Set<Usuario> padres = new HashSet<>();

    // Sección (grado + sección) del estudiante, si aplica
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_academico_id")
    private NivelAcademico nivelAcademico;

    // Última institución con la que trabajó, para auto-seleccionarla en el próximo login
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ultima_institucion_id")
    private Institucion ultimaInstitucion;

    public Usuario() {
    }

    // ─── UserDetails ───────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority(r.getNombre()))
                .collect(Collectors.toSet());
    }

    /** Spring Security usa este campo como "username" */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Set<Rol> getRoles() {
        return roles;
    }

    public void setRoles(Set<Rol> roles) {
        this.roles = roles;
    }

    @JsonIgnore
    public Set<Institucion> getInstituciones() {
        return instituciones;
    }

    public void setInstituciones(Set<Institucion> instituciones) {
        this.instituciones = instituciones;
    }

    @JsonIgnore
    public Set<Usuario> getEstudiantes() {
        return estudiantes;
    }

    public void setEstudiantes(Set<Usuario> estudiantes) {
        this.estudiantes = estudiantes;
    }

    @JsonIgnore
    public Set<Usuario> getPadres() {
        return padres;
    }

    public void setPadres(Set<Usuario> padres) {
        this.padres = padres;
    }

    public NivelAcademico getNivelAcademico() {
        return nivelAcademico;
    }

    public void setNivelAcademico(NivelAcademico nivelAcademico) {
        this.nivelAcademico = nivelAcademico;
    }

    public Institucion getUltimaInstitucion() {
        return ultimaInstitucion;
    }

    public void setUltimaInstitucion(Institucion ultimaInstitucion) {
        this.ultimaInstitucion = ultimaInstitucion;
    }

    @Override
    public String toString() {
        return "Usuario [id=" + id + ", nombre=" + nombre + ", email=" + email + ", usuario=" + usuario + ", cedula="
                + cedula + ", activo=" + activo + ", roles=" + roles + ", instituciones=" + instituciones + "]";
    }
}
