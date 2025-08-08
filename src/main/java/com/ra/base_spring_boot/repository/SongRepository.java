package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface SongRepository extends JpaRepository<Song, Long> {
    List<Song> findByAlbums_Id(Long albumId);

    List<Song> findByUsers_Id(Long userId);

    List<Song> findByGenres_Id(Long genreId);

    //top nghệ sĩ được yêu thích tính theo view của nghệ sĩ đó có - chỉ tính những bài hát APPROVED
    @Query(value = """
                SELECT u.id, u.first_name, u.last_name, u.profile_image, SUM(s.views) AS total_views
                FROM songs s
                JOIN users u ON s.artist_id = u.id
                WHERE s.status = 'APPROVED'
                GROUP BY u.id
                ORDER BY SUM(s.views) DESC
                LIMIT 10
            """, nativeQuery = true)
    List<Object[]> findTop10ArtistsByTotalViews();

    //thể loại nhạc nổi bật được tính theo view - chỉ tính những bài hát APPROVED
    @Query(value = """
                SELECT g.id, g.genre_name, g.genre_image, SUM(s.views) AS total_views
                FROM genres g
                JOIN song_genre sg ON g.id = sg.genre_id
                JOIN songs s ON s.id = sg.song_id
                WHERE s.status = 'APPROVED'
                GROUP BY g.id
                ORDER BY total_views DESC
                LIMIT 10
            """, nativeQuery = true)
    List<Object[]> findTop10GenresByTotalViews();

    //top 10 album nổi bật nhất tính theo tổng view của cả album đó - chỉ tính album và bài hát APPROVED
    @Query(value = """
    (
        SELECT a.id, a.title AS albumName, a.cover_image AS albumImage, SUM(s.views) AS totalViews, 1 as priority
        FROM albums a
        JOIN songs s ON s.album_id = a.id
        WHERE a.status = 'APPROVED'
          AND s.status = 'APPROVED'
          AND MONTH(s.created_at) = MONTH(NOW())
          AND YEAR(s.created_at) = YEAR(NOW())
        GROUP BY a.id
        ORDER BY totalViews DESC
        LIMIT 10
    )
    UNION ALL
    (
        SELECT a.id, a.title AS albumName, a.cover_image AS albumImage, SUM(s.views) AS totalViews, 2 as priority
        FROM albums a
        JOIN songs s ON s.album_id = a.id
        WHERE a.status = 'APPROVED'
          AND s.status = 'APPROVED'
          AND (
            (MONTH(s.created_at) = MONTH(DATE_SUB(NOW(), INTERVAL 1 MONTH))
             AND YEAR(s.created_at) = YEAR(DATE_SUB(NOW(), INTERVAL 1 MONTH)))
          )
        GROUP BY a.id
        ORDER BY totalViews DESC
        LIMIT 10
    )
    UNION ALL
    (
        SELECT a.id, a.title AS albumName, a.cover_image AS albumImage, SUM(s.views) AS totalViews, 3 as priority
        FROM albums a
        JOIN songs s ON s.album_id = a.id
        WHERE a.status = 'APPROVED'
          AND s.status = 'APPROVED'
          AND (
                (YEAR(s.created_at) < YEAR(DATE_SUB(NOW(), INTERVAL 1 MONTH)))
          OR
                (YEAR(s.created_at) = YEAR(DATE_SUB(NOW(), INTERVAL 1 MONTH))
         AND MONTH(s.created_at) < MONTH(DATE_SUB(NOW(), INTERVAL 1 MONTH))
            )
         )
        GROUP BY a.id
        ORDER BY totalViews DESC
        LIMIT 10
    )
    ORDER BY priority ASC, totalViews DESC
    LIMIT 10
""", nativeQuery = true)
    List<Object[]> findTop10AlbumsBySongViews();


    //top 15 bài nhạc có lượt nghe cao nhất trong tuần - ưu tiên bài hát có activity gần đây, fallback về top tổng thể nếu không đủ
    @Query(value = """
                (SELECT s.*, 1 as priority
                 FROM songs s
                 WHERE s.status = 'APPROVED' 
                 AND s.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                 ORDER BY s.views DESC
                 LIMIT 15)
                UNION ALL
                (SELECT s.*, 2 as priority
                 FROM songs s
                 WHERE s.status = 'APPROVED' 
                 AND s.created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
                 AND s.created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)
                 ORDER BY s.views DESC
                 LIMIT 15)
                UNION ALL
                    (
                        SELECT s.*, 3 as priority
                        FROM songs s
                        WHERE s.status = 'APPROVED'
                          AND s.created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
                        ORDER BY s.views DESC
                        LIMIT 15
                    )
                ORDER BY priority ASC, views DESC
                LIMIT 15
            """, nativeQuery = true)
    List<Song> findTop15SongsOfWeekByViews();

    Song findById(long id);

    @Query(value = """
            SELECT s.id, s.title, s.file_url, s.profile_pic_url
            FROM songs s
            JOIN song_genre sg ON s.id = sg.song_id
            JOIN genres g ON g.id = sg.genre_id
            WHERE g.genre_name = :genreName AND s.status = 'APPROVED'
            ORDER BY s.id ASC
            """, nativeQuery = true)
    List<Object[]> findSongsByGenreName(@Param("genreName") String genreName);

    @Query(value = """
            SELECT DISTINCT s.id, s.title, s.file_url, s.profile_pic_url
            FROM songs s
            JOIN song_genre sg ON s.id = sg.song_id
            WHERE sg.genre_id = :genreId AND s.status = 'APPROVED'
            ORDER BY s.id ASC
            """, nativeQuery = true)
    List<Object[]> findSongsByGenreId(@Param("genreId") Long genreId);

    @Query("""
                SELECT count(s) from Song s
                where s.createdAt> :start AND s.createdAt < :end
            """)
    Long countSongByDate(LocalDateTime start, LocalDateTime end);

    Song findTopByOrderByViewsDesc();

    @Query(value = """
            SELECT s.* FROM songs s 
            WHERE s.status = 'APPROVED'
            ORDER BY s.views DESC 
            LIMIT 12
            """, nativeQuery = true)
    List<Song> findTopSongByViews();

    @Query(value = """
                SELECT s.*
                FROM songs s
                WHERE s.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) AND s.status = 'APPROVED'
                ORDER BY s.views DESC
                LIMIT 12
            """, nativeQuery = true)
    List<Song> getTopSongsInMonth();


    @Modifying
    @Query("UPDATE Song s SET s.albums = null WHERE s.albums.id = :albumId")
    void updateAlbumToNullByAlbumId(@Param("albumId") Long albumId);

    // Methods for approval workflow
    List<Song> findByStatus(com.ra.base_spring_boot.model.constants.ApprovalStatus status);

    // Thêm method để lấy bài hát theo status và sắp xếp theo views giảm dần
    List<Song> findByStatusOrderByViewsDesc(com.ra.base_spring_boot.model.constants.ApprovalStatus status);

    List<Song> findByUsers_IdAndStatus(Long userId, com.ra.base_spring_boot.model.constants.ApprovalStatus status);

    @Query("SELECT s FROM Song s WHERE s.status = :status")
    List<Song> findSongsByStatus(@Param("status") com.ra.base_spring_boot.model.constants.ApprovalStatus status);

    // Methods for pagination
    @Query("SELECT s FROM Song s WHERE s.status = :status")
    org.springframework.data.domain.Page<Song> findByStatusPaged(
            @Param("status") com.ra.base_spring_boot.model.constants.ApprovalStatus status,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM Song s WHERE s.users.id = :userId AND s.status = :status")
    org.springframework.data.domain.Page<Song> findByUsers_IdAndStatusPaged(
            @Param("userId") Long userId,
            @Param("status") com.ra.base_spring_boot.model.constants.ApprovalStatus status,
            org.springframework.data.domain.Pageable pageable);

    List<Song> findByUsers_IdAndStatusIn(Long userId, List<com.ra.base_spring_boot.model.constants.ApprovalStatus> statuses);

    // Thêm phương thức mới để lấy songs theo album chỉ với status APPROVED
    @Query("SELECT s FROM Song s WHERE s.albums.id = :albumId AND s.status = 'APPROVED'")
    List<Song> findApprovedSongsByAlbumId(@Param("albumId") Long albumId);

    //tìm bài nhạc có lyrics giống với keyword
    @Query("""
                SELECT s FROM Song s WHERE LOWER(s.lyrics) LIKE CONCAT('%', LOWER(:keykeyword), '%')
            """)
    List<Song> findByLyricsContainingIgnoreCase(@Param("keykeyword") String keykeyword);

    @Query(value = """
                SELECT s.*,
                  (MATCH(s.title) AGAINST (?1 IN BOOLEAN MODE) * 10  + 6 *
                    MATCH(u.first_name, u.last_name) AGAINST (?1 IN BOOLEAN MODE)+
                    4 * MATCH(a.title) AGAINST (?1 IN BOOLEAN MODE) + 5 *
                    (CASE WHEN s.artist_id =(
                                  SELECT s2.artist_id
                                  FROM songs s2
                                  WHERE MATCH(s2.title) AGAINST (?1 IN BOOLEAN MODE)
                                  AND s2.status = 'APPROVED'
                                  ORDER BY MATCH(s2.title) AGAINST (?1 IN BOOLEAN MODE) DESC
                                  LIMIT 1
                                  )
                                  THEN 3 ELSE 0 END
                                )
                    + MATCH(s.lyrics) AGAINST (?1 IN BOOLEAN MODE) * 0.5
                    ) AS relevance
                FROM songs s
                JOIN users u ON s.artist_id = u.id
                JOIN albums a ON s.album_id = a.id
                JOIN user_role ur ON u.id = ur.user_id
                JOIN roles r ON ur.role_id = r.id
                WHERE (
                      MATCH(s.title, s.lyrics) AGAINST (?1 IN BOOLEAN MODE)
                      OR MATCH(u.first_name, u.last_name) AGAINST (?1 IN BOOLEAN MODE)
                      OR MATCH(a.title) AGAINST (?1 IN BOOLEAN MODE)
                      OR s.artist_id =(
                                  SELECT s2.artist_id
                                  FROM songs s2
                                  WHERE MATCH(s2.title) AGAINST (?1 IN BOOLEAN MODE)
                                  AND s2.status = 'APPROVED'
                                  ORDER BY MATCH(s2.title) AGAINST (?1 IN BOOLEAN MODE) DESC
                                  LIMIT 1
                                  )
                    )
                    AND r.roleName = 'ROLE_ARTIST'
                    AND s.status = 'APPROVED'
                ORDER BY relevance DESC
            """, nativeQuery = true)
    List<Song> fullTextSearchSongs(String keyword);

    // Methods cho album detail statistics
    @Query("SELECT COUNT(s) FROM Song s WHERE s.albums.id = :albumId AND s.status = 'APPROVED'")
    Integer countByAlbumsId(@Param("albumId") Long albumId);

    @Query("SELECT COALESCE(SUM(s.views), 0) FROM Song s WHERE s.albums.id = :albumId AND s.status = 'APPROVED'")
    Long sumViewsByAlbumId(@Param("albumId") Long albumId);

    @Query("SELECT COALESCE(COUNT(d), 0) FROM Download d JOIN d.songs s WHERE s.albums.id = :albumId AND s.status = 'APPROVED'")
    Integer sumDownloadsByAlbumId(@Param("albumId") Long albumId);

    @Query("SELECT s.duration FROM Song s WHERE s.albums.id = :albumId AND s.status = 'APPROVED'")
    List<String> findDurationsByAlbumId(@Param("albumId") Long albumId);

    // Methods cho artist statistics
    @Query("SELECT COUNT(s) FROM Song s WHERE s.users.id = :userId AND s.status = :status")
    Integer countByUsersIdAndStatus(@Param("userId") Long userId, @Param("status") com.ra.base_spring_boot.model.constants.ApprovalStatus status);
}