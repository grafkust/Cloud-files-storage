package com.project.cloud.files.storage.service;

import com.project.cloud.files.storage.exception.NotUniqueFieldException;
import com.project.cloud.files.storage.mapper.UserMapper;
import com.project.cloud.files.storage.model.dto.UserDto;
import com.project.cloud.files.storage.model.entity.user.Role;
import com.project.cloud.files.storage.model.entity.user.User;
import com.project.cloud.files.storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;


    public void create(User user) {

        checkFieldsOnUnique(user);

        Set<Role> roles = Set.of(Role.ROLE_USER);
        user.setRoles(roles);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }


    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    private void checkFieldsOnUnique(User user) {

        String errorMessage;
        boolean usernameNotUnique = userRepository.findByUsername(user.getUsername()).isPresent();
        boolean emailNotUnique = userRepository.findByEmail(user.getEmail()).isPresent();

        if (usernameNotUnique && emailNotUnique) {
            errorMessage = "User with this username and email already exists";
        } else if (usernameNotUnique) {
            errorMessage = "User with this username already exists";
        } else if (emailNotUnique)
            errorMessage = "User with this email already exists";

        else return;

        NotUniqueFieldException exception = new NotUniqueFieldException(errorMessage);
        UserDto userDto = userMapper.toDto(user);
        exception.setUserDto(userDto);
        throw exception;

    }
}
