package com.ra.base_spring_boot.services.wishlist;

import com.ra.base_spring_boot.dto.req.WishlistRequestDTO;
import com.ra.base_spring_boot.dto.resp.WishlistResponseDTO;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.Wishlist;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.repository.WishlistRepository;
import com.ra.base_spring_boot.exception.HttpNotFound;
import com.ra.base_spring_boot.exception.HttpUnAuthorized;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {


    private final WishlistRepository wishlistRepository;
    private final IUserRepository userRepository;
    private final SongRepository songRepository;

    private static final Map<String, String> SORT_MAPPINGS = new HashMap<>() {{
        put("songId", "songs.id");
        put("title", "songs.title");
        put("artistName", "songs.users.firstName");
    }};

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new HttpUnAuthorized("Authentication required.");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpNotFound("User not found")).getId();
    }

    @Override
    public WishlistResponseDTO addToWishlist(WishlistRequestDTO requestDTO) {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new HttpNotFound("User not found"));
        Song song = songRepository.findById(requestDTO.getSongId()).orElseThrow(() -> new HttpNotFound("Song not found"));

        // Kiểm tra status APPROVED trước khi thêm vào wishlist
        if (song.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
            throw new HttpBadRequest("Only approved songs can be added to wishlist");
        }

        Wishlist oldWishlist = wishlistRepository.findByUsers_IdAndSongs_Id(userId, song.getId());
        if (oldWishlist != null) {
            wishlistRepository.deleteById(oldWishlist.getId());
            return new WishlistResponseDTO(
                song.getId(),
                "REMOVED FROM WISHLIST " + song.getTitle(),
                song.getDuration(),
                song.getUsers().getFirstName() + " " + song.getUsers().getLastName(),
                song.getFileUrl(),
                song.getProfilePicUrl()
            );
        }
        Wishlist wishlist = Wishlist.builder().users(user).songs(song).build();
        wishlistRepository.save(wishlist);
        return new WishlistResponseDTO(
                song.getId(),
                song.getTitle(),
                song.getDuration(),
                song.getUsers().getFirstName() + " " + song.getUsers().getLastName(),
                song.getFileUrl(),
                song.getProfilePicUrl()
        );
    }

    @Override
    public String removeFromWishlist(Long songId) {
        Long userId = getCurrentUserId();
        Wishlist wishlist = wishlistRepository.findByUsers_IdAndSongs_Id(userId, songId);
        if (wishlist == null) {
            throw new HttpNotFound("Song not found in wishlist");
        }
        wishlistRepository.delete(wishlist);
        return "Song removed from wishlist successfully.";
    }

    @Override
    public Page<WishlistResponseDTO> getWishlistByCurrentUser(Pageable pageable) {
        Long userId = getCurrentUserId();
        String sortBy = pageable.getSort().isSorted() ?
                pageable.getSort().iterator().next().getProperty() : "songId";
        return getWishlistByUserId(userId, pageable, sortBy);
    }

    @Override
    public Page<WishlistResponseDTO> getWishlistByUserId(Long userId, Pageable pageable, String sortBy) {
        Sort sort = Sort.by(Sort.Direction.fromString(pageable.getSort().getOrderFor(sortBy) != null ?
                pageable.getSort().getOrderFor(sortBy).getDirection().name() : Sort.Direction.ASC.name()),
                SORT_MAPPINGS.getOrDefault(sortBy, sortBy));

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort);

        Page<Wishlist> wishlistPage = wishlistRepository.findByUsers_Id(userId, sortedPageable);

        return wishlistPage.map(w -> {
            // Chỉ trả về DTO nếu bài hát đã được APPROVED
            if (w.getSongs().getStatus() == com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
                return new WishlistResponseDTO(
                        w.getSongs().getId(),
                        w.getSongs().getTitle(),
                        w.getSongs().getDuration(),
                        w.getSongs().getUsers().getFirstName() + " " + w.getSongs().getUsers().getLastName(),
                        w.getSongs().getFileUrl(),
                        w.getSongs().getProfilePicUrl()
                );
            }
            return new WishlistResponseDTO(null, "Song not available", null, "", "", "");
        });
    }
    @Override
    public Long countLikeBySongId(Long songId) {
        return wishlistRepository.countWishlistBySongs_Id(songId);
    }
}
