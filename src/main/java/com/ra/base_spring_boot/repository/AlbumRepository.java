package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Album;
import com.ra.base_spring_boot.model.constants.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {

    List<Album> findByUsersId(Long artistId);

    List<Album> findByTitleContainingIgnoreCase(String title);

    // Featured albums list (assuming featured means most viewed) - only APPROVED albums
    @Query(value = """
                SELECT a.* FROM albums a
                JOIN songs s ON s.album_id = a.id
                WHERE a.status = 'APPROVED' AND s.status = 'APPROVED'
                GROUP BY a.id
                ORDER BY SUM(s.views) DESC
                LIMIT 10
            """, nativeQuery = true)
    List<Album> findFeaturedAlbums();

    // Top 15 albums by total views - only APPROVED albums
    @Query(value = """
                SELECT a.* FROM albums a
                JOIN songs s ON s.album_id = a.id
                WHERE a.status = 'APPROVED' AND s.status = 'APPROVED'
                GROUP BY a.id
                ORDER BY SUM(s.views) DESC
                LIMIT 15
            """, nativeQuery = true)
    List<Album> findTop15Albums();

    // Most popular album of each artist - only APPROVED albums
    @Query(value = """
            SELECT a.* FROM albums a
            JOIN (
                SELECT s.album_id, a2.artist_id, SUM(s.views) AS total_views
                FROM songs s
                JOIN albums a2 ON s.album_id = a2.id
                WHERE a2.status = 'APPROVED' AND s.status = 'APPROVED'
                GROUP BY s.album_id, a2.artist_id
            ) AS sub ON a.id = sub.album_id
            JOIN (
                SELECT artist_id, MAX(total_views) AS max_views FROM (
                    SELECT a2.artist_id, s.album_id, SUM(s.views) AS total_views
                    FROM songs s
                    JOIN albums a2 ON s.album_id = a2.id
                    WHERE a2.status = 'APPROVED' AND s.status = 'APPROVED'
                    GROUP BY s.album_id, a2.artist_id
                ) AS inner_sub
                GROUP BY artist_id
            ) AS top ON top.artist_id = sub.artist_id AND top.max_views = sub.total_views
            """, nativeQuery = true)
    List<Album> findOneTopAlbumPerArtist();

    @Query("""
            SELECT count(a) FROM Album a
            WHERE a.createdAt > :start AND a.createdAt < :end
            """)
    Long countAlbumByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT * FROM albums WHERE artist_id = :artistId AND status = 'APPROVED'", nativeQuery = true)
    List<Album> findAlbumsByArtistId(@Param("artistId") Long artistId);

    // Method to get all APPROVED albums for guests
    @Query("SELECT a FROM Album a WHERE a.status = 'APPROVED'")
    List<Album> findAllApprovedAlbums();

    // Method to get newly released APPROVED albums only
    @Query(value = """
                SELECT * FROM albums 
                WHERE status = 'APPROVED' 
                ORDER BY created_at DESC 
                LIMIT 8
            """, nativeQuery = true)
    List<Album> findNewlyReleasedApprovedAlbums();

    List<Album> findByStatus(ApprovalStatus status);

    List<Album> findByUsersIdAndStatus(Long userId, ApprovalStatus status);

    List<Album> findByUsersIdAndStatusIn(Long userId, List<ApprovalStatus> statuses);

    // Paged versions
    @Query("SELECT a FROM Album a WHERE a.status = :status")
    Page<Album> findByStatusPaged(
            @Param("status") ApprovalStatus status,
            Pageable pageable);

    @Query("SELECT a FROM Album a WHERE a.users.id = :userId AND a.status = :status")
    Page<Album> findByUsersIdAndStatusPaged(
            @Param("userId") Long userId,
            @Param("status") ApprovalStatus status,
            Pageable pageable);

    @Query(value = """
                SELECT a.*,
                  (MATCH(title) AGAINST (?1 IN BOOLEAN MODE) *5 +3 *(CASE WHEN a.artist_id=(
                    SELECT artist_id FROM albums a2 WHERE 
                        MATCH(a2.title) AGAINST (?1 IN BOOLEAN MODE)
                        AND a2.status = 'APPROVED'
                        ORDER BY MATCH(a2.title) AGAINST (?1 IN BOOLEAN MODE) DESC
                        LIMIT 1
                            )THEN 3 ELSE 0 END
                      )+ 5 *
                    (CASE WHEN a.artist_id =(
                                  SELECT s2.artist_id
                                  FROM songs s2
                                  WHERE MATCH(s2.title) AGAINST (?1 IN BOOLEAN MODE)
                                  AND s2.status = 'APPROVED'
                                  ORDER BY MATCH(s2.title) AGAINST (?1 IN BOOLEAN MODE) DESC
                                  LIMIT 1
                                  )
                                  THEN 3 ELSE 0 END
                                )
                    ) AS relevance
                FROM albums a
                WHERE( MATCH(title) AGAINST (?1 IN BOOLEAN MODE)
                OR a.artist_id=(
                    SELECT artist_id FROM albums a2 WHERE 
                        MATCH(a2.title) AGAINST (?1 IN BOOLEAN MODE)
                        AND a2.status = 'APPROVED'
                        ORDER BY MATCH(a2.title) AGAINST (?1 IN BOOLEAN MODE) DESC
                        LIMIT 1
                )
                OR a.artist_id =(
                    SELECT s2.artist_id
                        FROM songs s2
                        WHERE MATCH(s2.title) AGAINST (?1 IN BOOLEAN MODE)
                        AND s2.status = 'APPROVED'
                        ORDER BY MATCH(s2.title) AGAINST (?1 IN BOOLEAN MODE) DESC
                        LIMIT 1
)    )
                  AND status = 'APPROVED'
                ORDER BY relevance DESC
            """, nativeQuery = true)
    List<Album> fullTextSearchAlbum(String keyword);

    @Query("SELECT COUNT(a) FROM Album a WHERE a.users.id = :userId AND a.status = :status")
    Integer countByUsersIdAndStatus(@Param("userId") Long userId, @Param("status") ApprovalStatus status);
}

