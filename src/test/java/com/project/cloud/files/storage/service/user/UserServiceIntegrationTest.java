package com.project.cloud.files.storage.service.user;

import com.project.cloud.files.storage.BaseIntegrationTest;
import com.project.cloud.files.storage.exception.NotUniqueFieldException;
import com.project.cloud.files.storage.model.entity.user.Role;
import com.project.cloud.files.storage.model.entity.user.User;
import com.project.cloud.files.storage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setPassword("password");
    }

    @Test
    @DisplayName("Should successfully create new user")
    void createUserSuccessTest() {

        String rawPassword = "password";
        testUser.setPassword(rawPassword);

        userService.create(testUser);

        User savedUser = userRepository.findByUsername(testUser.getUsername()).orElseThrow();

        assertNotNull(savedUser.getId());
        assertEquals(testUser.getUsername(), savedUser.getUsername());
        assertEquals(testUser.getEmail(), savedUser.getEmail());
        assertTrue(passwordEncoder.matches(rawPassword, savedUser.getPassword()));
        assertTrue(savedUser.getRoles().contains(Role.ROLE_USER));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void createUserDuplicateUsernameTest() {

        userService.create(testUser);

        User duplicateUser = new User();
        duplicateUser.setUsername(testUser.getUsername());
        duplicateUser.setEmail("different@test.com");
        duplicateUser.setPassword("password2");

        NotUniqueFieldException exception = assertThrows(NotUniqueFieldException.class,
                () -> userService.create(duplicateUser)
        );
        assertTrue(exception.getMessage().contains("username"));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void createUserDuplicateEmailTest() {

        userService.create(testUser);

        User duplicateUser = new User();
        duplicateUser.setUsername("different");
        duplicateUser.setEmail(testUser.getEmail());
        duplicateUser.setPassword("password2");

        NotUniqueFieldException exception = assertThrows(NotUniqueFieldException.class,
                () -> userService.create(duplicateUser)
        );
        assertTrue(exception.getMessage().contains("email"));
    }

    @Test
    @DisplayName("Should successfully get user by username")
    void getByUsernameSuccessTest() {

        userService.create(testUser);

        User retrievedUser = userService.getByUsername(testUser.getUsername());

        assertNotNull(retrievedUser);
        assertEquals(testUser.getUsername(), retrievedUser.getUsername());
        assertEquals(testUser.getEmail(), retrievedUser.getEmail());
    }

    @Test
    @DisplayName("Should throw exception when username not found")
    void getByUsernameNotFoundTest() {
        assertThrows(UsernameNotFoundException.class,
                () -> userService.getByUsername("nonexistent")
        );
    }

    @Test
    @DisplayName("Should successfully get user ID by username")
    void getIdByUsernameSuccessTest() {

        userService.create(testUser);

        Long userId = userService.getIdByUsername(testUser.getUsername());

        assertNotNull(userId);
        assertTrue(userId > 0);

        User savedUser = userRepository.findById(userId).orElseThrow();
        assertEquals(testUser.getUsername(), savedUser.getUsername());
    }
}