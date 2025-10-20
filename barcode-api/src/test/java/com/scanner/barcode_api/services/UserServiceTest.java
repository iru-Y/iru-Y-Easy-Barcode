package com.scanner.barcode_api.services;

import com.scanner.barcode_api.dtos.ChangePasswordRequest;
import com.scanner.barcode_api.models.User;
import com.scanner.barcode_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void changePassword_shouldUpdatePassword_whenOldPasswordMatches() {
        User user = new User(UUID.randomUUID(), "example@gmail.com", "encoded1234", "USER");
        ChangePasswordRequest request = new ChangePasswordRequest("1234", "4321");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "encoded1234")).thenReturn(true);
        when(passwordEncoder.encode("4321")).thenReturn("encoded4321");

        userService.changePassword(user.getEmail(), request);

        assertThat(user.getPassword()).isEqualTo("encoded4321");
        verify(userRepository).save(user);
        verify(userRepository).flush();
    }

    @Test
    void changePassword_shouldThrow_whenOldPasswordDoesNotMatch() {
        User user = new User(UUID.randomUUID(), "example@gmail.com", "encoded1234", "USER");
        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "4321");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded1234")).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                userService.changePassword(user.getEmail(), request)
        );

        verify(userRepository, never()).save(any());
    }
}
