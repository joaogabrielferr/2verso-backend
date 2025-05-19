package com.escritr.escritr.aws.s3;

import com.escritr.escritr.common.enums.ErrorAssetEnum;
import com.escritr.escritr.common.enums.ErrorCodeEnum;
import com.escritr.escritr.common.services.FileValidationService;
import com.escritr.escritr.exceptions.FileUploadException;
import com.escritr.escritr.exceptions.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final FileValidationService fileValidationService;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(S3Client s3Client,FileValidationService fileValidationService){
        this.s3Client = s3Client;
        this.fileValidationService = fileValidationService;
    }


    public String uploadFile(MultipartFile file) {

        fileValidationService.validateFile(file);


        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if(originalFileName == null){
            throw new FileUploadException("Error while parsing the name of the file", ErrorAssetEnum.STORAGE, ErrorCodeEnum.STORAGE_ERROR);
        }
        int lastDot = originalFileName.lastIndexOf(".");
        if (lastDot > 0 && lastDot < originalFileName.length() - 1) {
            fileExtension = originalFileName.substring(lastDot);
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        try{
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
        }catch (S3Exception e) {
            logger.error("S3Exception during upload of file '{}' to bucket '{}': Status Code: {}, AWS Error Code: {}, Message: {}",
                    uniqueFileName, bucketName, e.statusCode(), e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
            throw new InternalServerErrorException("Failed to save image to storage due to an unexpected error with the storage provider.");
        } catch (IOException e) {
            logger.error("IOException during S3 upload preparation for file: {}. Error: {}", originalFileName, e.getMessage(), e);
            throw new FileUploadException("There was an error while processing the file for upload",ErrorAssetEnum.STORAGE,ErrorCodeEnum.STORAGE_ERROR);
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
        } catch(Exception e){
            throw new InternalServerErrorException("Failed to delete file");
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
