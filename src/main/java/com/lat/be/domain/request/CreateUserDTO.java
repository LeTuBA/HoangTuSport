package com.lat.be.domain.request;

import com.lat.be.util.constant.GenderEnum;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserDTO {
    @Size(min = 3, message = "Tên phải có ít nhất 3 ký tự")
    private String name;

    @NotNull
    @Email(message = "Email không đúng định dạng", regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")
    private String email;
    
    @NotNull
    private String password;
    private GenderEnum gender;
    private String address;
    private Long roleId;
}
