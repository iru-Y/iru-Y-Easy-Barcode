package com.scanner.barcode_api.repository;

import com.scanner.barcode_api.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {


    @Mock
    UserRepository userRepository;

    @Mock
    User user;

    @Test
    void findByEmail() {
    user = new User(UUID.randomUUID(), "example@gmail.com", "1234", "USER");

    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

    var result = userRepository.findByEmail(user.getEmail());
    assertThat(result).isPresent();

    }

}