package com.psychosim.domain.caso;

import com.psychosim.domain.user.User;
import com.psychosim.domain.user.UserRepository;
import com.psychosim.domain.user.UserRole;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoServiceTest {

    @Mock
    private CasoRepository casoRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CasoService casoService;

    private User profesor;

    @BeforeEach
    void setUp() {
        profesor = new User();
        profesor.setId(1L);
        profesor.setNombre("Test");
        profesor.setApellido("Profesor");
        profesor.setRole(UserRole.PROFESOR);
    }

    @Test
    void listarActivos_retornaListaVaciaCorrectamente() {
        when(casoRepository.findByActivoTrue()).thenReturn(List.of());
        assertThat(casoService.listarActivos()).isEmpty();
    }

    @Test
    void obtenerDetalle_conIdInexistente_lanzaEntityNotFoundException() {
        when(casoRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> casoService.obtenerDetalle(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void crear_guardaCorrectamente() {
        var req = new CasoService.CasoRequest(
                "Caso Test", "Descripción", "Contexto", List.of());
        Caso casoGuardado = new Caso();
        casoGuardado.setId(1L);
        casoGuardado.setTitulo("Caso Test");
        casoGuardado.setCreatedBy(profesor);

        when(casoRepository.save(any(Caso.class))).thenReturn(casoGuardado);

        CasoDTO resultado = casoService.crear(req, profesor);
        assertThat(resultado.titulo()).isEqualTo("Caso Test");
        verify(casoRepository, times(1)).save(any(Caso.class));
    }

    @Test
    void eliminar_marcaCasoComoInactivo() {
        Caso caso = new Caso();
        caso.setId(1L);
        caso.setActivo(true);
        caso.setCreatedBy(profesor);

        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(casoRepository.save(any(Caso.class))).thenReturn(caso);

        casoService.eliminar(1L);

        assertThat(caso.isActivo()).isFalse();
        verify(casoRepository).save(caso);
    }
}
