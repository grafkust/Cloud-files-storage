package com.project.cloud.files.storage.service.user;

import com.project.cloud.files.storage.exception.NotUniqueFieldException;
import com.project.cloud.files.storage.model.dto.UserDto;
import com.project.cloud.files.storage.model.entity.user.Role;
import com.project.cloud.files.storage.model.entity.user.User;
import com.project.cloud.files.storage.repository.UserRepository;
import com.project.cloud.files.storage.util.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public void create(User user) {
        checkFieldsOnUnique(user);
        prepareUser(user);
        userRepository.save(user);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User with username '" + username + "' not found"));
    }

    public Long getIdByUsername(String username) {
        User user = getByUsername(username);
        return user.getId();
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

    private void prepareUser(User user) {
        setRoleUser(user);
        setEncodePassword(user);
    }

    private void setEncodePassword(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
    }

    private void setRoleUser(User user) {
        Set<Role> roles = Set.of(Role.ROLE_USER);
        user.setRoles(roles);
    }
}
