package com.rayfay.gira.service.impl;

import com.rayfay.gira.dto.request.CreateUserRequest;
import com.rayfay.gira.dto.request.UpdateUserRequest;
import com.rayfay.gira.dto.response.UserResponse;
import com.rayfay.gira.entity.User;
import com.rayfay.gira.entity.UserRole;
import com.rayfay.gira.entity.UserStatus;
import com.rayfay.gira.exception.ResourceNotFoundException;
import com.rayfay.gira.exception.UserAlreadyExistsException;
import com.rayfay.gira.mapper.UserMapper;
import com.rayfay.gira.repository.UserRepository;
import com.rayfay.gira.service.interfaces.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("邮箱已存在");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = getUserOrThrow(id);

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        if (!currentUser.getRole().equals(UserRole.ADMIN) && !user.getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("无权限修改其他用户信息");
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("邮箱已存在");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getStatus() != null && currentUser.getRole().equals(UserRole.ADMIN)) {
            user.setStatus(request.getStatus());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void updatePassword(Long id, String oldPassword, String newPassword) {
        User user = getUserOrThrow(id);

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!user.getUsername().equals(currentUsername)) {
            throw new AccessDeniedException("无权限修改其他用户密码");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(getUserOrThrow(id));
    }

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserOrThrow(id);

        if (user.getRole().equals(UserRole.ADMIN)) {
            long adminCount = userRepository.countByRole(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("系统必须保留至少一个管理员账号");
            }
        }

        userRepository.delete(user);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
}