package com.escritr.escritr.config.local;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@Profile("dev")
public class LocalStackBucketInitializer {
    private final S3Client s3Client;
    private final String bucketName;

    public LocalStackBucketInitializer(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    //creates the s3 bucket in localstack
    //the S3Client object using localstack is constructed on aws/s3/S3Config
    @PostConstruct
    public void init() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            System.out.println("LocalStack S3 bucket '" + bucketName + "' already exists.");
        } catch (NoSuchBucketException e) {
            System.out.println("LocalStack S3 bucket '" + bucketName + "' does not exist. Creating...");
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            System.out.println("LocalStack S3 bucket '" + bucketName + "' created.");
        } catch (S3Exception e) {
            System.err.println("Error checking/creating LocalStack S3 bucket: " + e.getMessage());
        }
    }
}