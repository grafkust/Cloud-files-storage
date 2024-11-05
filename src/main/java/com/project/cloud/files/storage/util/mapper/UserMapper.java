package com.project.cloud.files.storage.util.mapper;

import com.project.cloud.files.storage.model.dto.UserDto;
import com.project.cloud.files.storage.model.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ModelMapper modelMapper;

    public User toEntity(UserDto userDto) {
        return modelMapper.map(userDto, User.class);
    }

    public UserDto toDto(User user) {
        return modelMapper.map(user, UserDto.class);
    }


}
