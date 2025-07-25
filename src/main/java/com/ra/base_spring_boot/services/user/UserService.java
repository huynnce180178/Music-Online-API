package com.ra.base_spring_boot.services.user;

import com.ra.base_spring_boot.dto.req.UpdateProfileRequestDTO;
import com.ra.base_spring_boot.dto.resp.*;

import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface UserService {
    Page<UserResponseDTO> getUsers(String keyword,Pageable pageable);
    UserResponseDTO changeStatus(Long id, UserStatus status);
    Page<ArtistResponseDTO> getArtist(String keyword, Pageable pageable);
    StatisticUserResponseDTO getStatisticUserResponseDTO(LocalDateTime start, LocalDateTime end);
    ProfileResponseDTO getProfileById(Long id);
    ProfileResponseDTO updateProfile(Long id, UpdateProfileRequestDTO updateProfileRequestDTO);
    void changePassWord(Long id, String oldPassWord, String newPassWord);
    //follow_api
    List<UserFollowResponseDTO> getFollowers(Long id);
    List<UserFollowResponseDTO> getFollowings(Long id);
    void followUser(Long currentUserId, Long targetUserId);
    void unFollowUser(Long currentUserId, Long targetUserId);
    boolean isFollowing(Long currentUserId, Long targetUserId);
    void setPassword(Long id, String newPassword);
    boolean hasPassword(Long id);
    User getArtistById(Long userId);
    UserViewArtistResponseDTO getUserViewArtistById(Long id);
    List<UserResponseDTO> fullTextSearchUsers(String keyword);
    List<Long> getFollowerIds(UserService userService, Long artistId);
}
