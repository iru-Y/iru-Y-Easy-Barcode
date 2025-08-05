package com.scanner.barcode_api.config.admin;

import com.scanner.barcode_api.models.User;
import com.scanner.barcode_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class LoginUserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${login.email}")
    private String loginEmail;

    @Value("${login.password}")
    private String loginPassword;

    @Value("${login.role:USER}")
    private String loginRole;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(loginEmail).isEmpty()) {
            User user = new User();
            user.setEmail(loginEmail);
            user.setPassword(passwordEncoder.encode(loginPassword));
            user.setRole(loginRole.toUpperCase());

            userRepository.save(user);
            System.out.println("🟢 Usuário de login criado com sucesso!");
        } else {
            System.out.println("🟡 Usuário de login já existe.");
        }
    }
}
