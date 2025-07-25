package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.dto.resp.StatisticUserResponseDTO;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.model.constants.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IUserRepository extends JpaRepository<User, Long>
{
    String getNameUserById(Long id);
    Optional<User> findByEmail(String email);
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = :status")
    Optional<User> findByEmailAndStatus(@Param("email") String email, @Param("status") UserStatus status);
    @Query("select u FROM User u WHERE lower(u.firstName) LIKE lower(concat('%', ?1, '%')) OR lower(u.lastName) LIKE lower(concat('%', ?2, '%'))")
    Page<User> findByFirstNameOrLastName(String firstName, String lastName, Pageable pageable);
    @Query("select distinct u FROM User u JOIN  u.roles r" +
            " WHERE lower(u.firstName) LIKE lower(concat('%', ?1, '%')) OR" +
            " lower(u.lastName) LIKE lower(concat('%', ?2, '%'))" +
            "And r.roleName ='ROLE_ARTIST'")
    Page<User> searchArtist(String firstName, String lastName, Pageable pageable);
    @Query("select distinct u FROM User u JOIN  u.roles r" +
            " WHERE r.roleName ='ROLE_ARTIST'")
    Page<User> getArtist(Pageable pageable);

    @Query("""
    SELECT new com.ra.base_spring_boot.dto.resp.StatisticUserResponseDTO(
        COUNT(DISTINCT u),
        SUM(CASE WHEN r.roleName = com.ra.base_spring_boot.model.constants.RoleName.ROLE_USER THEN 1 ELSE 0 END),
        SUM(CASE WHEN r.roleName = com.ra.base_spring_boot.model.constants.RoleName.ROLE_ARTIST THEN 1 ELSE 0 END),
        SUM(CASE WHEN r.roleName = com.ra.base_spring_boot.model.constants.RoleName.ROLE_PREMIUM THEN 1 ELSE 0 END)
    )
    FROM User u
    JOIN u.roles r
""")
    StatisticUserResponseDTO getUserStatistics();

    @Query("""
    select  new com.ra.base_spring_boot.dto.resp.StatisticUserResponseDTO(
        COUNT(distinct u),
        SUM(CASE WHEN r.roleName = com.ra.base_spring_boot.model.constants.RoleName.ROLE_USER THEN 1 ELSE 0 END ),
        SUM(CASE WHEN r.roleName = com.ra.base_spring_boot.model.constants.RoleName.ROLE_ARTIST THEN 1 ELSE 0 END),
        SUM(CASE WHEN r.roleName = com.ra.base_spring_boot.model.constants.RoleName.ROLE_PREMIUM THEN 1 ELSE 0 END)
    )
    FROM User u
    join u.roles r
    WHERE u.createdAt> :start
    AND u.createdAt< :end
""")
    StatisticUserResponseDTO getUserStatisticsByDate(LocalDateTime start, LocalDateTime end);


    User getByEmail(String email);
    Optional<User> findByFacebookId(String facebookId);

    @Query(value = """
    SELECT u.*, MATCH(u.first_name, u.last_name) AGAINST (?1 IN BOOLEAN MODE) AS relevance
    FROM users u
    JOIN user_role ur ON u.id = ur.user_id
    JOIN roles r ON ur.role_id = r.id
    WHERE MATCH(u.first_name, u.last_name) AGAINST (?1 IN BOOLEAN MODE)
      AND r.roleName = 'ROLE_ARTIST'
    ORDER BY relevance DESC
""", nativeQuery = true)
    List<User> fullTextSearchUser(String keyword);





    boolean existsByEmail(String email);
}
