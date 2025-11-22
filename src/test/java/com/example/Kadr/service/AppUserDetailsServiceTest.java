package com.example.Kadr.service;

import com.example.Kadr.config.AppUserDetails;
import com.example.Kadr.model.Role;
import com.example.Kadr.model.User;
import com.example.Kadr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadsUserWhenExists() {
        Role role = Role.builder().title("User").build();
        User user = User.builder().username("demo").password("pwd").role(role).build();
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(user));

        var details = (AppUserDetails) service.loadUserByUsername("demo");

        assertThat(details.getUsername()).isEqualTo("demo");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER");
    }

    @Test
    void throwsWhenUserMissing() {
        when(userRepository.findByUsername("absent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("absent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");
    }
}