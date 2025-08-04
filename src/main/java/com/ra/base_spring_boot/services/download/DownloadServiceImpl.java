package com.ra.base_spring_boot.services.download;

import com.ra.base_spring_boot.dto.req.DownloadRequestDTO;
import com.ra.base_spring_boot.dto.resp.DownloadResponseDTO;
import com.ra.base_spring_boot.model.Download;
import com.ra.base_spring_boot.model.Song;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.DownloadRepository;
import com.ra.base_spring_boot.repository.SongRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.exception.HttpNotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class DownloadServiceImpl implements DownloadService {
    private static final Logger log = LoggerFactory.getLogger(DownloadServiceImpl.class);

    @Autowired
    private DownloadRepository downloadRepository;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private SongRepository songRepository;


    @Override
    @Transactional(readOnly = true)
    public Page<DownloadResponseDTO> getDownloadsByUserWithPaging(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User not found"));

        Page<Download> downloads = downloadRepository.findByUsersWithSongs(user, pageable);
        return downloads.map(download -> {
            Song song = download.getSongs();
            return DownloadResponseDTO.builder()
                    .userId(user.getId())
                    .songId(song.getId())
                    .songTitle(song.getTitle())
                    .albumName(song.getAlbums() != null ? song.getAlbums().getTitle() : null)
                    .duration(song.getDuration().toString())
                    .build();
        });
    }

    @Override
    @Transactional
    public void deleteDownload(Long userId, Long songId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new HttpNotFound("User not found"));
            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new HttpNotFound("Song not found"));

            List<Download> downloads = downloadRepository.findAllByUsersAndSongs(user, song);
            if (downloads.isEmpty()) {
                throw new HttpNotFound("Download record not found");
            }
            downloadRepository.deleteAll(downloads);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete download: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DownloadResponseDTO downloadSong(Long userId, DownloadRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HttpNotFound("User not found"));
        Song song = songRepository.findById(requestDTO.getSongId())
                .orElseThrow(() -> new HttpNotFound("Song not found"));

        if (song.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
            throw new RuntimeException("Only approved songs can be downloaded");
        }

        Download existingDownload = downloadRepository.findByUsersAndSongs(user, song)
                .orElse(null);

        if (existingDownload == null) {
            Download download = Download.builder()
                    .users(user)
                    .songs(song)
                    .addedAt(java.time.LocalDateTime.now())
                    .build();
            downloadRepository.save(download);
        }

        return DownloadResponseDTO.builder()
                .userId(user.getId())
                .songId(song.getId())
                .songTitle(song.getTitle())
                .albumName(song.getAlbums() != null ? song.getAlbums().getTitle() : null)
                .duration(song.getDuration().toString())
                .build();
    }

    @Override
    @Transactional
    public byte[] downloadSongFile(Long userId, Long songId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new HttpNotFound("User not found"));
            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new HttpNotFound("Song not found"));

            if (song.getStatus() != com.ra.base_spring_boot.model.constants.ApprovalStatus.APPROVED) {
                throw new RuntimeException("Only approved songs can be downloaded");
            }

            String fileUrl = song.getFileUrl();
            if (fileUrl == null || fileUrl.isEmpty()) {
                throw new RuntimeException("Song URL does not exist");
            }

            Download existingDownload = downloadRepository.findByUsersAndSongs(user, song)
                    .orElse(null);

            if (existingDownload == null) {
                Download download = Download.builder()
                        .users(user)
                        .songs(song)
                        .addedAt(java.time.LocalDateTime.now())
                        .build();
                downloadRepository.save(download);
            }

            return downloadFileData(fileUrl);

        } catch (Exception e) {
            log.error("Error downloading file: ", e);
            throw new RuntimeException("Download failed: " + e.getMessage());
        }
    }


    public HttpHeaders createDownloadHeaders(Song song) {
        HttpHeaders headers = new HttpHeaders();

        try {
            String fileName = sanitizeFileName(song.getTitle()) + ".mp3";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());

            // Hỗ trợ tất cả trình duyệt và thiết bị
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName);

            // Content-Type cho audio
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));

            // Headers để hỗ trợ download trên mobile
            headers.add("Content-Transfer-Encoding", "binary");
            headers.add("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
            headers.add("Pragma", "public");

            // Hỗ trợ Safari và iOS
            headers.add("Accept-Ranges", "bytes");

            // Thêm headers cho mobile optimization
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("X-Frame-Options", "DENY");

        } catch (Exception e) {
            log.warn("Error creating download headers: ", e);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=song.mp3");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        return headers;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "song";

        return fileName.replaceAll("[<>:\"/\\\\|?*]", "_")
                .replaceAll("[\\x00-\\x1f\\x7f]", "")
                .trim();
    }

    private byte[] downloadFileData(String fileUrl) throws Exception {
        int maxRetries = 3;
        int currentRetry = 0;
        byte[] fileData = null;

        while (currentRetry < maxRetries && fileData == null) {
            try {
                fileData = downloadWithTimeout(fileUrl);
                break;
            } catch (SocketTimeoutException e) {
                currentRetry++;
                if (currentRetry >= maxRetries) {
                    throw new RuntimeException("Download timed out after " + maxRetries + " attempts. Please try again later.");
                }
                Thread.sleep(1000);
            }
        }

        if (fileData == null) {
            throw new RuntimeException("Failed to download file data");
        }

        log.info("File data downloaded successfully, size: {} bytes", fileData.length);
        return fileData;
    }

    private byte[] downloadWithTimeout(String fileUrl) throws Exception {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(120000);

        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; MusicApp/1.0; +http://musicapp.com/bot)");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("Cache-Control", "no-cache");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP error code: " + responseCode);
        }

        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            log.debug("Downloaded {} bytes from {}", totalBytes, fileUrl);
            return outputStream.toByteArray();
        } finally {
            connection.disconnect();
        }
    }
}
