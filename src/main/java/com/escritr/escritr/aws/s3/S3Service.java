package com.escritr.escritr.aws.s3;

import com.escritr.escritr.common.ErrorAssetEnum;
import com.escritr.escritr.common.ErrorCodeEnum;
import com.escritr.escritr.exceptions.BadRequestException;
import com.escritr.escritr.exceptions.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file) {

        try{
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            logger.info("File {} uploaded successfully to S3 bucket {}.", uniqueFileName, bucketName);

            GetUrlRequest getUrlRequest = GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueFileName)
                    .build();
            URL url = s3Client.utilities().getUrl(getUrlRequest);
            return url.toString();
        }catch (IOException e) {
            logger.info("IOException:Error while saving image to storage:{}", e.getMessage());
            throw new BadRequestException("Error while saving image to storage", ErrorAssetEnum.ARTICLE, ErrorCodeEnum.STORAGE_ERROR);
        } catch (Exception e) {
            logger.info("Error while saving image to storage:{}", e.getMessage());
            throw new InternalServerErrorException("Error while saving image to storage");
        }

    }

    public void deleteFile(String fileKey) {
        try {

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            s3Client.headObject(headObjectRequest); // Throws S3Exception if not found

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            logger.info("File {} deleted successfully from S3 bucket {}.", fileKey, bucketName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                logger.warn("File {} not found in S3 bucket {} during deletion attempt.", fileKey, bucketName);
            } else {
                logger.error("Error deleting file {} from S3: {}", fileKey, e.getMessage(), e);
                throw e;
            }
        }
    }


    public String extractKeyFromUrl(String fileUrl) {
        System.out.println("file url:" + fileUrl);
        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath();
            // Path usually starts with '/', remove it
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        } catch (Exception e) {
            logger.error("Could not parse S3 key from URL: {}", fileUrl, e);
            return null;
        }
    }

    public void deleteMultipleFiles(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return;
        }

        List<ObjectIdentifier> objectsToDelete = fileKeys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .collect(Collectors.toList());

        DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(Delete.builder().objects(objectsToDelete).quiet(false).build())
                .build();

        try {
            DeleteObjectsResponse response = s3Client.deleteObjects(deleteObjectsRequest);
            logger.info("Successfully deleted {} objects.", response.deleted().size());
            if (response.hasErrors()) {
                response.errors().forEach(error ->
                        logger.error("Error deleting object {}: {}", error.key(), error.message()));
            }
        } catch (S3Exception e) {
            logger.error("Error during batch delete from S3: {}", e.getMessage(), e);
            throw e;
        }
    }
}
