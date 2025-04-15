package com.lat.be.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import com.lat.be.util.constant.GenderEnum;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ResCreateUserDTO {
    Long id;
    String name;
    String email;
    GenderEnum gender;
    String address;
    Instant createdAt;
    String avatar;
    String roleName;
}
