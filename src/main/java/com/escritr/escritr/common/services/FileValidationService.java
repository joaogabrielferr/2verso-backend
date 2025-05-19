package com.escritr.escritr.common.services;

import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;
import com.escritr.escritr.exceptions.FileSizeLimitExceededException;
import com.escritr.escritr.exceptions.FileUploadException;
import com.escritr.escritr.exceptions.UnsupportedFileTypeException;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class FileValidationService {

    private static final Logger logger = LoggerFactory.getLogger(FileValidationService.class);

    private final List<String> allowedMimeTypes;
    private final List<String> allowedExtensions;
    private final long maxFileSize; // in bytes

    private final Tika tika = new Tika(); // For MIME type detection

    public FileValidationService(
            @Value("${file.upload.allowed-mime-types:image/jpeg,image/png,image/gif,image/webp}") String allowedMimeTypesCsv,
            @Value("${file.upload.allowed-extensions:jpg,jpeg,png,gif,webp}") String allowedExtensionsCsv,
            @Value("${spring.servlet.multipart.max-file-size}") String maxFileSizeStr
    ) {
        this.allowedMimeTypes = Arrays.asList(allowedMimeTypesCsv.toLowerCase().split("\\s*,\\s*"));
        this.allowedExtensions = Arrays.asList(allowedExtensionsCsv.toLowerCase().split("\\s*,\\s*"));
        this.maxFileSize = parseSize(maxFileSizeStr);
        logger.info("FileValidationService initialized. Allowed MIME types: {}, Allowed extensions: {}, Max file size: {} bytes",
                this.allowedMimeTypes, this.allowedExtensions, this.maxFileSize);
    }

    private long parseSize(String size) {
        if (size == null || size.isBlank()) {
            return 10 * 1024 * 1024; //10mb as default
        }
        size = size.toUpperCase();
        long l = Long.parseLong(size.substring(0, size.length() - 2));
        if (size.endsWith("KB")) {
            return l * 1024;
        }
        if (size.endsWith("MB")) {
            return l * 1024 * 1024;
        }
        if (size.endsWith("GB")) {
            return l * 1024 * 1024 * 1024;
        }
        try {
            return Long.parseLong(size); // if no unit specified then consider bytes
        } catch (NumberFormatException e) {
            logger.warn("Could not parse max-file-size '{}'. Defaulting to 10MB.", size);
            return 10 * 1024 * 1024;
        }
    }


    public void validateFile(MultipartFile file) throws FileUploadException {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or not provided.", ErrorAssetEnum.STORAGE, ErrorCodeEnum.STORAGE_ERROR);
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new FileUploadException("File name is empty.",ErrorAssetEnum.STORAGE, ErrorCodeEnum.STORAGE_ERROR);
        }

        if (file.getSize() > maxFileSize) {
            throw new FileSizeLimitExceededException(
                    String.format("File size exceeds the limit of %d MB.", maxFileSize / (1024 * 1024)),ErrorAssetEnum.STORAGE, ErrorCodeEnum.STORAGE_ERROR);
        }


        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null || !allowedExtensions.contains(extension.toLowerCase())) {
            logger.warn("File upload rejected. Invalid extension: '{}'. Allowed: {}", extension, allowedExtensions);
            throw new UnsupportedFileTypeException(
                    String.format("File extension '%s' is not allowed. Allowed extensions are: %s",
                            extension, String.join(", ", allowedExtensions)),ErrorAssetEnum.STORAGE, ErrorCodeEnum.STORAGE_ERROR);
        }

        // Validate MIME type using Apache Tika
        String detectedMimeType;
        try (InputStream inputStream = file.getInputStream()) {
            detectedMimeType = tika.detect(inputStream);
        } catch (IOException e) {
            logger.error("Could not read file content to detect MIME type for file: {}", originalFilename, e);
            throw new FileUploadException("Could not process file content.",ErrorAssetEnum.STORAGE, ErrorCodeEnum.STORAGE_ERROR);
        }

        if (detectedMimeType == null || !allowedMimeTypes.contains(detectedMimeType.toLowerCase())) {
            logger.warn("File upload rejected. Invalid MIME type: '{}' for file '{}'. Client reported: '{}'. Allowed: {}",
                    detectedMimeType, originalFilename, file.getContentType(), allowedMimeTypes);
            throw new UnsupportedFileTypeException(
                    String.format("File type '%s' is not allowed. Allowed types are: %s",
                            detectedMimeType, String.join(", ", allowedMimeTypes)),ErrorAssetEnum.STORAGE, ErrorCodeEnum.STORAGE_ERROR);
        }

        logger.debug("File validation successful for: {}, Detected MIME type: {}, Size: {}",
                originalFilename, detectedMimeType, file.getSize());
    }
}