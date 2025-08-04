package com.ra.base_spring_boot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ra.base_spring_boot.model.base.BaseObject;
import com.ra.base_spring_boot.model.constants.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User extends BaseObject
{
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    @Column(name ="last_name",nullable = false, length = 100)
    private String lastName;
    @Column(name = "email", nullable = false)
    private String email;
    @JsonIgnore
    @Column(name = "passwordF", nullable = false)
    private String password;
    @Column(name = "profile_image")
    private String profileImage;
    @Lob
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", updatable = false, nullable = false)
    private LocalDateTime updatedAt;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;


    @ManyToMany
    @JoinTable(
            name = "user_following",
            joinColumns = @JoinColumn(name= "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    private Set<User> followings = new HashSet<>();

    @ManyToMany(mappedBy = "followings")
    private Set<User> followers= new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserMood> moods;

    @Column(name = "facebook_id", unique = true)
    private String facebookId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    public int getAge() {
        if (dateOfBirth == null) return 0;
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}