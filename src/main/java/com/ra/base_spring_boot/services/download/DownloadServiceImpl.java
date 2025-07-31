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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    // Device types enum
    public enum DeviceType {
        WINDOWS, MAC, ANDROID, IPHONE, OTHER
    }

    // Audio format preferences for each device type
    public enum AudioFormat {
        MP3("mp3", "audio/mpeg"),
        M4A("m4a", "audio/mp4");

        private final String extension;
        private final String mimeType;

        AudioFormat(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public String getExtension() { return extension; }
        public String getMimeType() { return mimeType; }
    }

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
        downloadSongFileWithUserAgent(userId, songId, null);
    }

    @Override
    @Transactional
    public void downloadSongFileWithUserAgent(Long userId, Long songId, String userAgent) {
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

            // Detect device type from User-Agent or use current request
            DeviceType deviceType = detectDeviceType(userAgent);
            AudioFormat preferredFormat = getPreferredAudioFormat(deviceType);

            // Save download record
            downloadSong(userId, DownloadRequestDTO.builder()
                    .songId(songId)
                    .downloadPath(getDeviceSpecificDownloadPath(deviceType))
                    .build());

            String fileName = getSafeFileName(song.getTitle()) + "." + preferredFormat.getExtension();
            String downloadPath = getDeviceSpecificDownloadPath(deviceType) + File.separator + fileName;

            // For mobile devices, we might want to stream instead of download to local storage
            if (deviceType == DeviceType.ANDROID || deviceType == DeviceType.IPHONE) {
                handleMobileDownload(fileUrl, fileName, preferredFormat, deviceType);
            } else {
                handleDesktopDownload(fileUrl, downloadPath, preferredFormat);
            }

            log.info("File download initiated successfully for device type {}: {}", deviceType, fileName);
        } catch (Exception e) {
            log.error("Error downloading file: ", e);
            throw new RuntimeException("Download failed: " + e.getMessage());
        }
    }

    private DeviceType detectDeviceType(String userAgent) {
        // Try to get User-Agent from current request if not provided
        if (userAgent == null) {
            try {
                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                HttpServletRequest request = attr.getRequest();
                userAgent = request.getHeader("User-Agent");
            } catch (Exception e) {
                log.debug("Could not get User-Agent from request context");
            }
        }

        if (userAgent != null) {
            userAgent = userAgent.toLowerCase();

            if (userAgent.contains("android")) {
                return DeviceType.ANDROID;
            } else if (userAgent.contains("iphone") || userAgent.contains("ipad") || userAgent.contains("ipod")) {
                return DeviceType.IPHONE;
            } else if (userAgent.contains("windows")) {
                return DeviceType.WINDOWS;
            } else if (userAgent.contains("mac")) {
                return DeviceType.MAC;
            }
        }

        // Fallback to server OS detection
        if (OS_NAME.contains("win")) {
            return DeviceType.WINDOWS;
        } else if (OS_NAME.contains("mac")) {
            return DeviceType.MAC;
        }

        return DeviceType.OTHER;
    }

    private AudioFormat getPreferredAudioFormat(DeviceType deviceType) {
        switch (deviceType) {
            case IPHONE:
                return AudioFormat.M4A; // iOS prefers M4A/AAC
            case ANDROID:
                return AudioFormat.MP3; // Android supports MP3 universally
            case MAC:
                return AudioFormat.M4A; // Mac prefers M4A but supports MP3
            case WINDOWS:
            case OTHER:
            default:
                return AudioFormat.MP3; // Universal fallback
        }
    }

    private String getDeviceSpecificDownloadPath(DeviceType deviceType) {
        switch (deviceType) {
            case ANDROID:
                // Android typically downloads to Downloads or Music folder
                return "/storage/emulated/0/Download"; // or could be configurable
            case IPHONE:
                // iOS apps have sandboxed storage, this would be app-specific
                return "~/Documents"; // Placeholder - actual implementation would differ
            case MAC:
                return System.getProperty("user.home") + "/Downloads";
            case WINDOWS:
                return "C:\\Users\\" + System.getProperty("user.name") + "\\Downloads";
            default:
                return DEFAULT_DOWNLOAD_PATH;
        }
    }

    private void handleMobileDownload(String fileUrl, String fileName, AudioFormat format, DeviceType deviceType) throws IOException {

        log.info("Preparing mobile download for device type: {} with format: {}", deviceType, format);

        String tempPath = System.getProperty("java.io.tmpdir") + File.separator + fileName;
        File tempFile = new File(tempPath);
        downloadWithTimeout(fileUrl, tempFile, getDeviceSpecificUserAgent(deviceType));

        log.info("Mobile download prepared: {}", tempPath);
    }

    private void handleDesktopDownload(String fileUrl, String downloadPath, AudioFormat format) throws IOException {
        File downloadFile = new File(downloadPath);
        File downloadDir = downloadFile.getParentFile();

        if (!downloadDir.exists() && !downloadDir.mkdirs()) {
            throw new RuntimeException("Cannot create Downloads directory: " + downloadDir.getAbsolutePath());
        }

        // Download with extended timeout and retry
        int maxRetries = 3;
        int currentRetry = 0;
        boolean downloaded = false;

        while (currentRetry < maxRetries && !downloaded) {
            try {
                downloadWithTimeout(fileUrl, downloadFile, "Mozilla/5.0 (Desktop)");
                downloaded = true;
            } catch (SocketTimeoutException e) {
                currentRetry++;
                if (currentRetry >= maxRetries) {
                    throw new RuntimeException("Download timed out after " + maxRetries + " attempts. Please try again later.");
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Download interrupted");
                }
            }
        }
    }

    private String getDeviceSpecificUserAgent(DeviceType deviceType) {
        switch (deviceType) {
            case ANDROID:
                return "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Mobile Safari/537.36";
            case IPHONE:
                return "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1";
            case MAC:
                return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
            case WINDOWS:
                return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
            default:
                return "Mozilla/5.0";
        }
    }

    // Helper methods
    private void downloadWithTimeout(String fileUrl, File downloadFile) throws IOException {
        downloadWithTimeout(fileUrl, downloadFile, "Mozilla/5.0");
    }

    private void downloadWithTimeout(String fileUrl, File downloadFile, String userAgent) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);

        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "audio/mpeg, audio/mp4, audio/ogg, audio/*");
        connection.setRequestProperty("Accept-Encoding", "identity"); // Disable compression for audio files

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned HTTP response code: " + responseCode + " for URL: " + fileUrl);
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
                    if (totalBytesRead % (fileSize / 10) == 0) { // Log every 10%
                        log.debug("Download progress: {}%", progress);
                    }
                }
            }
        }
    }

    private String getSafeFileName(String originalName) {
        return originalName
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .trim();
    }
}
