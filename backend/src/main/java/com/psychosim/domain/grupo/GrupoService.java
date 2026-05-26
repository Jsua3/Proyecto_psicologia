package com.psychosim.domain.grupo;

import com.psychosim.domain.user.User;
import com.psychosim.domain.user.UserRepository;
import com.psychosim.domain.user.UserRole;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GrupoDTO> listarDeProfesor(Long profesorId) {
        return grupoRepository.findByProfesorIdAndActivoTrue(profesorId)
                .stream().map(GrupoDTO::from).toList();
    }

    @Transactional
    public GrupoDTO crear(String nombre, String codigo, User profesor) {
        if (grupoRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe un grupo con el código: " + codigo);
        }
        Grupo grupo = new Grupo();
        grupo.setNombre(nombre);
        grupo.setCodigo(codigo);
        grupo.setProfesor(profesor);
        return GrupoDTO.from(grupoRepository.save(grupo));
    }

    @Transactional
    public GrupoDTO agregarEstudiante(Long grupoId, String email, User profesor) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new EntityNotFoundException("Grupo no encontrado: " + grupoId));
        if (!grupo.getProfesor().getId().equals(profesor.getId())) {
            throw new IllegalArgumentException("No tiene permiso sobre este grupo");
        }
        User estudiante = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + email));
        if (estudiante.getRole() != UserRole.ESTUDIANTE) {
            throw new IllegalArgumentException("El usuario no tiene rol de estudiante");
        }
        grupo.getEstudiantes().add(estudiante);
        return GrupoDTO.from(grupoRepository.save(grupo));
    }

    public record GrupoDTO(Long id, String nombre, String codigo, int totalEstudiantes) {
        static GrupoDTO from(Grupo g) {
            return new GrupoDTO(g.getId(), g.getNombre(), g.getCodigo(), g.getEstudiantes().size());
        }
    }
}
