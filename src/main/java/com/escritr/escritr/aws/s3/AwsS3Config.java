package com.escritr.escritr.aws.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {

    @Value("${aws.s3.region}")
    private String awsRegion;

    @Value("${aws.accessKeyId:#{null}}")
    private String accessKey;

    @Value("${aws.secretKey:#{null}}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        Region region = Region.of(awsRegion);

        // For Production: IAM Roles (EC2 Instance Profile)
        // for local dev: ~/.aws/credentials (I'm currently using this) or env vars AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
        if (accessKey == null || secretKey == null) {
            return S3Client.builder()
                    .region(region)
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }

        // For local dev explicitly using application.properties keys
        return S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}