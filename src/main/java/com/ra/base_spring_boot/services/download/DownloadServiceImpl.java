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
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

@Service
public class DownloadServiceImpl implements DownloadService {
    private static final Logger log = LoggerFactory.getLogger(DownloadServiceImpl.class);
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String DEFAULT_DOWNLOAD_PATH = getDefaultDownloadPath();

    private static String getDefaultDownloadPath() {
        String username = System.getProperty("user.name");
        if (OS_NAME.contains("win")) {
            return "C:\\Users\\" + username + "\\Downloads";
        } else if (OS_NAME.contains("mac")) {
            return "/Users/" + username + "/Downloads";
        } else {
            return System.getProperty("user.home") + File.separator + "Downloads";
        }
    }

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
    public void downloadSongFile(Long userId, Long songId) {
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

            // Save download record
            downloadSong(userId, DownloadRequestDTO.builder()
                    .songId(songId)
                    .downloadPath(DEFAULT_DOWNLOAD_PATH)
                    .build());

            String fileName = getSafeFileName(song.getTitle()) + ".mp3";
            String downloadPath = DEFAULT_DOWNLOAD_PATH + File.separator + fileName;
            File downloadFile = new File(downloadPath);

            File downloadDir = new File(DEFAULT_DOWNLOAD_PATH);
            if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                throw new RuntimeException("Cannot create Downloads directory");
            }

            // Download with extended timeout and retry
            int maxRetries = 3;
            int currentRetry = 0;
            boolean downloaded = false;

            while (currentRetry < maxRetries && !downloaded) {
                try {
                    downloadWithTimeout(fileUrl, downloadFile);
                    downloaded = true;
                } catch (SocketTimeoutException e) {
                    currentRetry++;
                    if (currentRetry >= maxRetries) {
                        throw new RuntimeException("Download timed out after " + maxRetries + " attempts. Please try again later.");
                    }
                    // Wait before retrying
                    Thread.sleep(1000);
                }
            }

            log.info("File downloaded successfully: {}", fileName);
        } catch (Exception e) {
            log.error("Error downloading file: ", e);
            throw new RuntimeException("Download failed: " + e.getMessage());
        }
    }

    // Helper methods
    private void downloadWithTimeout(String fileUrl, File downloadFile) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);

        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept", "audio/mpeg, audio/*");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned HTTP response code: " + responseCode);
        }

        int fileSize = connection.getContentLength();

        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream out = new FileOutputStream(downloadFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytesRead = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if (fileSize > 0) {
                    int progress = (int) ((totalBytesRead * 100L) / fileSize);
                    log.debug("Download progress: {}%", progress);
                }
            }
        }
    }

    private String getSafeFileName(String originalName) {
        return originalName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_");
    }
}
