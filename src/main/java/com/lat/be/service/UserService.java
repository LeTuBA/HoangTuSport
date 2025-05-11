package com.lat.be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lat.be.domain.Role;
import com.lat.be.domain.User;
import com.lat.be.domain.request.CreateUserDTO;
import com.lat.be.domain.request.UpdateUserDTO;
import com.lat.be.domain.response.ResCreateUserDTO;
import com.lat.be.domain.response.ResUpdateUserDTO;
import com.lat.be.domain.response.ResUserDTO;
import com.lat.be.domain.response.ResultPaginationDTO;
import com.lat.be.repository.UserRepository;
import com.lat.be.util.SecurityUtil;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final CloudinaryService cloudinaryService;


    public User handleCreateUser(User user, MultipartFile avatarFile) {
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = this.cloudinaryService.uploadImage(avatarFile);
            user.setAvatar(avatarUrl);
        }


        if(user.getRole() != null){
            Role r = this.roleService.fetchRoleById(user.getRole().getId());
            user.setRole(r != null ? r : null);
        }

        return this.userRepository.save(user);
    }

    public void handleDeleteUser(Long id) {
        this.userRepository.deleteById(id);
    }

    public User fetchUserById(Long id) {
        Optional<User> userOptional = this.userRepository.findById(id);
        if (userOptional.isPresent()) {
            return userOptional.get();
        }
        return null;
    }

    public ResultPaginationDTO handleGetUser(Specification<User> userSpec, Pageable pageable) {
        Page<User> pageUser = this.userRepository.findAll(userSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();

        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(pageUser.getTotalPages());
        meta.setTotal(pageUser.getTotalElements());

        rs.setMeta(meta);

        List<ResUserDTO> listUser  = pageUser.getContent()
                        .stream().map(item -> this.convertToResUserDTO(item))
                .collect(Collectors.toList());

        rs.setResult(listUser);

        return rs;
    }

    public User handleUpdateUser(Long userId, UpdateUserDTO reqUser, MultipartFile avatarFile) {
        User currentUser = this.fetchUserById(userId);

        if (currentUser!=null) {
            currentUser.setAddress(reqUser.getAddress());
            currentUser.setGender(reqUser.getGender());
            currentUser.setName(reqUser.getName());

            if(avatarFile != null && !avatarFile.isEmpty()){
                String avatarUrl = this.cloudinaryService.uploadImage(avatarFile);
                currentUser.setAvatar(avatarUrl);
            }

            if(reqUser.getRoleId() != null){
                Role r = this.roleService.fetchRoleById(reqUser.getRoleId());
                currentUser.setRole(r != null ? r : null);
            }
        }
        currentUser = this.userRepository.save(currentUser);
        return currentUser;
    }

    public User handleUpdateUserProfile(UpdateUserDTO reqUser, MultipartFile avatarFile) {
        User currentUser = this.getCurrentUser();
        if (currentUser!=null) {
            currentUser.setAddress(reqUser.getAddress());
            currentUser.setGender(reqUser.getGender());
            currentUser.setName(reqUser.getName());

            if(avatarFile != null && !avatarFile.isEmpty()){
                String avatarUrl = this.cloudinaryService.uploadImage(avatarFile);
                currentUser.setAvatar(avatarUrl);
            }
        }
        currentUser = this.userRepository.save(currentUser);
        return currentUser;
    }

    public User handleGetUserByEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserLogin()
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông tin người dùng hiện tại"));
        User user = handleGetUserByEmail(email);
        if (user == null) {
            throw new EntityNotFoundException("Không tìm thấy người dùng với email: " + email);
        }
        return user;
    }

    public ResCreateUserDTO convertToResCreateUserDTO(User user) {
        ResCreateUserDTO resCreateUserDTO = new ResCreateUserDTO();

        resCreateUserDTO.setId(user.getId());
        resCreateUserDTO.setEmail(user.getEmail());
        resCreateUserDTO.setName(user.getName());
        resCreateUserDTO.setCreatedAt(user.getCreatedAt());
        resCreateUserDTO.setGender(user.getGender());
        resCreateUserDTO.setAddress(user.getAddress());
        resCreateUserDTO.setRoleName(user.getRole().getName());
        resCreateUserDTO.setAvatar(user.getAvatar());

        return resCreateUserDTO;
    }

    public ResUpdateUserDTO convertToResUpdateUserDTO(User user) {
        ResUpdateUserDTO resUpdateUserDTO = new ResUpdateUserDTO();

        resUpdateUserDTO.setId(user.getId());
        resUpdateUserDTO.setName(user.getName());
        resUpdateUserDTO.setUpdatedAt(user.getUpdatedAt());
        resUpdateUserDTO.setGender(user.getGender());
        resUpdateUserDTO.setAddress(user.getAddress());
        resUpdateUserDTO.setAvatar(user.getAvatar());

        return resUpdateUserDTO;
    }

    public ResUserDTO convertToResUserDTO(User user) {
        ResUserDTO resUserDTO = new ResUserDTO();
        ResUserDTO.RoleUser roleUser = new ResUserDTO.RoleUser();

        resUserDTO.setId(user.getId());
        resUserDTO.setEmail(user.getEmail());
        resUserDTO.setName(user.getName());
        resUserDTO.setUpdatedAt(user.getUpdatedAt());
        resUserDTO.setCreatedAt(user.getCreatedAt());
        resUserDTO.setGender(user.getGender());
        resUserDTO.setAddress(user.getAddress());
        resUserDTO.setAvatar(user.getAvatar());

        if(user.getRole() != null){
            roleUser.setId(user.getRole().getId());
            roleUser.setName(user.getRole().getName());
            resUserDTO.setRole(roleUser);
        }


        return resUserDTO;
    }


    public User toEntity(CreateUserDTO formRequest) {
        return User.builder()
                .name(formRequest.getName())
                .email(formRequest.getEmail())
                .password(formRequest.getPassword())
                .gender(formRequest.getGender())
                .address(formRequest.getAddress())
                .role(formRequest.getRoleId() != null ? Role.builder().id(formRequest.getRoleId()).build() : null)
                .build();
    }

    public void updateUserToken(String token, String email){
        User currentUser = this.handleGetUserByEmail(email);
        if(currentUser != null){
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }

    public User getUserByRefreshTokenAndEmail(String token, String email){
        return this.userRepository.findByRefreshTokenAndEmail(token, email);
    }
}
