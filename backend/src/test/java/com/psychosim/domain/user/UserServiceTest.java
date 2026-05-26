package com.psychosim.domain.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Test
    void user_nombreCompleto_concatenaNombreYApellido() {
        User user = new User();
        user.setNombre("María");
        user.setApellido("González");

        assertThat(user.getNombreCompleto()).isEqualTo("María González");
    }

    @Test
    void user_activoPorDefecto() {
        User user = new User();
        assertThat(user.isActivo()).isTrue();
    }

    @Test
    void user_createdAtSeInicializa() {
        User user = new User();
        assertThat(user.getCreatedAt()).isNotNull();
    }
}
