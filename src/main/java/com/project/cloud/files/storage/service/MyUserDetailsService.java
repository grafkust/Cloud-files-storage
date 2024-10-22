package com.project.cloud.files.storage.service;

import com.project.cloud.files.storage.model.entity.user.Role;
import com.project.cloud.files.storage.model.entity.user.User;
import com.project.cloud.files.storage.model.entity.userDetails.MyUserDetails;
import com.project.cloud.files.storage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MyUserDetailsService implements UserDetailsService {


    @Autowired
    private  UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws InternalAuthenticationServiceException {

        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            throw new InternalAuthenticationServiceException("User with username '" + username + "' not found");
        }

        return create(user.get());
    }

    private MyUserDetails create(User user) {
        return new MyUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                mapToGrantedAuthorities(new ArrayList<>(user.getRoles()))
        );
    }

    private List<GrantedAuthority> mapToGrantedAuthorities(List<Role> roles) {
        return roles.stream()
                .map(Enum::name)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

}