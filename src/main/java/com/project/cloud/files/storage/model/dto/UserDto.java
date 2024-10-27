package com.project.cloud.files.storage.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@NotNull
public class UserDto {

    @Length(min = 3, max = 50, message = "Username length should be between 3 and 50 symbols")
    private String username;

    @Email(message = "Email should have valid form")
    private String email;

    @Length(min = 5, max = 50, message = "Password length should be between 5 and 50 symbols")
    private String password;


}
