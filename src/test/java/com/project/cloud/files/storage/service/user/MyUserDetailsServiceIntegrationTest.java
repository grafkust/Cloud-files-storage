package com.project.cloud.files.storage.service.user;

import com.project.cloud.files.storage.BaseIntegrationTest;
import com.project.cloud.files.storage.model.entity.user.Role;
import com.project.cloud.files.storage.model.entity.user.User;
import com.project.cloud.files.storage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MyUserDetailsServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setPassword("password");
        testUser.setRoles(Set.of(Role.ROLE_USER));
    }

    @Test
    @DisplayName("Should successfully load user by username")
    void loadUserByUsernameSuccessTest() {

        userRepository.save(testUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.getUsername());

        assertNotNull(userDetails);
        assertEquals(testUser.getUsername(), userDetails.getUsername());
        assertEquals(testUser.getPassword(), userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("Should successfully map multiple roles to authorities")
    void loadUserWithMultipleRolesTest() {

        testUser.setRoles(Set.of(Role.ROLE_USER, Role.ROLE_ADMIN));
        userRepository.save(testUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.getUsername());

        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertTrue(authorities.contains("ROLE_USER"));
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertEquals(2, authorities.size());
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void loadUserByUsernameNotFoundTest() {
        String nonExistentUsername = "nonexistent";

        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername(nonExistentUsername)
        );
    }

    @Test
    @DisplayName("Should create MyUserDetails with empty roles")
    void loadUserWithEmptyRolesTest() {

        testUser.setRoles(new HashSet<>());
        userRepository.save(testUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername(testUser.getUsername());

        assertTrue(userDetails.getAuthorities().isEmpty());
    }
}