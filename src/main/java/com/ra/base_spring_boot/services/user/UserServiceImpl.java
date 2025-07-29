package com.ra.base_spring_boot.services.user;

import com.ra.base_spring_boot.controller.NotificationWebSocketController;
import com.ra.base_spring_boot.dto.req.UpdateProfileRequestDTO;
import com.ra.base_spring_boot.dto.resp.*;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.UserStatus;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.impl.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {


    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private NotificationWebSocketController notificationWebSocketController;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");


    public ProfileResponseDTO mapToProfileDTO(User user) {
        return ProfileResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .profileImage(user.getProfileImage())
                .createdAt(user.getCreatedAt().format(formatter))
                .updatedAt(user.getUpdatedAt().format(formatter))
                .age(user.getAge())
                .dateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                .build();
    }
    public UserResponseDTO mapToDTO(User user){
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getRoleName().name())
                .collect(Collectors.toSet());

        String createdAtStr = user.getCreatedAt() != null ? user.getCreatedAt().format(fmt) : "";
        String updatedAtStr = user.getUpdatedAt() != null ? user.getUpdatedAt().format(fmt) : "";

        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .status(user.getStatus())
                .profileImage(user.getProfileImage())
                .createdAtStr(createdAtStr)
                .updatedAtStr(updatedAtStr)
                .roleNames(roleNames)
                .build();
    }
    @Override
    public List<Long> getFollowerIds(UserService userService, Long artistId) {
        return userService.getFollowers(artistId)
                .stream()
                .map(UserFollowResponseDTO::getId)
                .toList();
    }
    @Override
    public Page<UserResponseDTO> getUsers(String keyword,Pageable pageable) {
        Page<User> users;
        if(keyword == null || keyword.isEmpty()){
            users = userRepository.findAll(pageable);
        }else {
            users = userRepository.findByFirstNameOrLastName(keyword, keyword, pageable);
        }
        List<UserResponseDTO> userDTOS = users.getContent().stream().map(this::mapToDTO).collect(Collectors.toList());
        return new PageImpl<>(userDTOS, pageable, users.getTotalElements());

    }
    @Override
    public Page<ArtistResponseDTO> getArtist(String keyword, Pageable pageable) {
        Page<User> users;
        if(keyword == null || keyword.isEmpty()){
            users = userRepository.getArtist(pageable);

        }else {
            users = userRepository.searchArtist(keyword, keyword, pageable);
        }
        List<ArtistResponseDTO> userDTOS = users.getContent().stream()
                .map(user -> ArtistResponseDTO.builder()
                        .artistId(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .profile_image(user.getProfileImage())
                        .build())
                .collect(Collectors.toList());
        return new PageImpl<>(userDTOS, pageable, users.getTotalElements());
    }

    @Override
    public UserResponseDTO changeStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        if (status == UserStatus.BLOCKED || (oldStatus == UserStatus.BLOCKED && status != UserStatus.BLOCKED)) {
            sendUserStatusNotification(user, status, oldStatus);
        }

        return mapToDTO(user);
    }

    private void sendUserStatusNotification(User user, UserStatus newStatus, UserStatus oldStatus) {
        String message;
        String type;
        boolean requireLogout = false;

        if (newStatus == UserStatus.BLOCKED) {
            message = "Your account has been locked by the administrator. Please contact support for more details.";
            type = "ACCOUNT_BLOCKED";
            requireLogout = true;
        } else if (oldStatus == UserStatus.BLOCKED && (newStatus == UserStatus.ACTIVE || newStatus == UserStatus.VERIFY)) {
            message = "Your account has been unlocked. You can continue using the service.";
            type = "ACCOUNT_UNBLOCKED";
            requireLogout = false;
        } else {
            return;
        }

        try {
            UserBlockNotificationDTO notification = UserBlockNotificationDTO.builder()
                    .userId(user.getId())
                    .username(user.getFirstName() + " " + user.getLastName())
                    .message(message)
                    .type(type)
                    .timestamp(LocalDateTime.now())
                    .requireLogout(requireLogout)
                    .build();

            if (notificationWebSocketController != null) {
                notificationWebSocketController.sendUserBlockNotification(user.getId(), notification);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public StatisticUserResponseDTO getStatisticUserResponseDTO(LocalDateTime start, LocalDateTime end) {
        if(start == null && end == null){
            return userRepository.getUserStatistics();
        } else if(start.isAfter(end)){
            throw new IllegalArgumentException("start time is after end time");
        }

        return userRepository.getUserStatisticsByDate(start, end);
    }

    @Override
    public ProfileResponseDTO getProfileById(Long id) {
        return mapToProfileDTO(userRepository.findById(id).orElse(null));
    }
    @Override
    public ProfileResponseDTO updateProfile(Long id, UpdateProfileRequestDTO updateProfileRequestDTO) {
        User user1 = userRepository.findById(id).orElse(null);
        if(user1 == null){
            throw new IllegalArgumentException("user not found");
        }
        if (updateProfileRequestDTO.getBio() != null) {
            user1.setBio(updateProfileRequestDTO.getBio());
        }
        if (updateProfileRequestDTO.getFirstName() != null) {
            user1.setFirstName(updateProfileRequestDTO.getFirstName());
        }
        if (updateProfileRequestDTO.getLastName() != null) {
            user1.setLastName(updateProfileRequestDTO.getLastName());
        }


        if(updateProfileRequestDTO.getFile() != null){
            user1.setProfileImage(cloudinaryService.uploadFile(updateProfileRequestDTO.getFile(), "images"));
        }
        if (updateProfileRequestDTO.getDateOfBirth() != null) {
            try {
                user1.setDateOfBirth(java.time.LocalDate.parse(updateProfileRequestDTO.getDateOfBirth()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date of birth. Correct format: yyyy-MM-dd");            }
        }
        user1.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user1);
        return mapToProfileDTO(user1);
    }
    @Override
    public void changePassWord(Long id, String oldPassWord, String newPassWord) {
        User user =  userRepository.findById(id).orElse(null);
        String oldPassWordEncoding =passwordEncoder.encode(oldPassWord);
        if(user == null){
            throw new IllegalArgumentException("user not found");
        }else if(!passwordEncoder.matches(oldPassWord, user.getPassword())){
            throw new IllegalArgumentException("old Password does not match");
        }
        user.setPassword(passwordEncoder.encode(newPassWord));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    @Override
    public List<UserFollowResponseDTO> getFollowers(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if(user == null){
            throw new IllegalArgumentException("user not found");
        }
        return user.getFollowers().stream().map(follower -> new UserFollowResponseDTO(
                        follower.getId(),
                        follower.getFirstName() + " " + follower.getLastName()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserFollowResponseDTO> getFollowings(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if(user == null){
            throw new IllegalArgumentException("user not found");
        }
        return user.getFollowings().stream().map(following ->new UserFollowResponseDTO(
                        following.getId(),
                        following.getFirstName()+ " " +following.getLastName()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void followUser(Long currentUserId, Long targetUserId) {
        if(currentUserId.equals(targetUserId)){
            throw new IllegalArgumentException("You cannot follow yourself.");
        }
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if(currentUser == null || targetUser == null){
            throw new IllegalArgumentException("user not found");
        }
        if(currentUser.getFollowers().contains(targetUser)){
            throw new IllegalArgumentException("Already following this user.");
        }
        currentUser.getFollowings().add(targetUser);
        userRepository.save(currentUser);
    }

    @Override
    public void unFollowUser(Long currentUserId, Long targetUserId) {
        if(currentUserId.equals(targetUserId)){
            throw new IllegalArgumentException("You cannot follow yourself.");
        }
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if(currentUser == null || targetUser == null){
            throw new IllegalArgumentException("user not found");
        }
        if(currentUser.getFollowers().contains(targetUser)){
            throw new IllegalArgumentException("You are not following this user.");
        }
        currentUser.getFollowings().remove(targetUser);
        userRepository.save(currentUser);
    }

    @Override
    public boolean isFollowing(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) return false;

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (currentUser == null){
            throw new IllegalArgumentException("user not found");
        }
        return  currentUser.getFollowings().stream()
                .anyMatch(user -> user.getId().equals(targetUserId));
    }

    @Override
    public void setPassword(Long id, String newPassword) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password already set. Use change password instead.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public boolean hasPassword(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        return user.getPassword() != null && !user.getPassword().isEmpty();
    }
    @Override
    public User getArtistById(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getRoles() != null && user.getRoles().stream().anyMatch(role -> role.getRoleName().name().equals("ROLE_ARTIST")))
                .orElseThrow(() -> new RuntimeException("Artist not found or user is not an artist"));
    }

    @Override
    public UserViewArtistResponseDTO getUserViewArtistById(Long id) {
        User artist = getArtistById(id);
        return UserViewArtistResponseDTO.builder()
                .id(artist.getId())
                .firstName(artist.getFirstName())
                .lastName(artist.getLastName())
                .email(artist.getEmail())
                .profileImage(artist.getProfileImage())
                .bio(artist.getBio())
                .status(artist.getStatus())
                .createdAt(artist.getCreatedAt())
                .updatedAt(artist.getUpdatedAt())
                .roles(artist.getRoles() != null ? artist.getRoles().stream().map(role -> role.getRoleName().name()).collect(java.util.stream.Collectors.toSet()) : null)
                .followings(artist.getFollowings() != null ? artist.getFollowings().stream().map(User::getId).collect(java.util.stream.Collectors.toSet()) : null)
                .followers(artist.getFollowers() != null ? artist.getFollowers().stream().map(User::getId).collect(java.util.stream.Collectors.toSet()) : null)
                .build();
    }
    @Override
    public List<UserResponseDTO> fullTextSearchUsers(String keyword) {
        List<User> userList = userRepository.fullTextSearchUser(keyword);
        return userList.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
}
