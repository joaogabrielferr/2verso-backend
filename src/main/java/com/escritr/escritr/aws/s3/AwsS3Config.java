package com.escritr.escritr.aws.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class AwsS3Config {

    private static final Logger log = LoggerFactory.getLogger(AwsS3Config.class);

    @Value("${aws.s3.region}")
    private String awsRegion;

    @Value("${aws.accessKeyId:#{null}}")
    private String accessKey;

    @Value("${aws.secretKey:#{null}}")
    private String secretKey;

    //for production/default
    @Bean
    @Profile("!dev")
    public S3Client s3Client() {
        Region region = Region.of(awsRegion);

        // For Production: IAM Roles (EC2 Instance Profile)
        // or if using aws directly instead of localstack locally: ~/.aws/credentials or env vars AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
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

    @Bean
    @Profile("dev")
    public S3Client s3ClientDev(
            @Value("${aws.s3.localstack.endpoint}") String localstackEndpoint,
            @Value("${aws.s3.localstack.accessKey:test}") String localstackAccessKey, // Default "test" for LocalStack
            @Value("${aws.s3.localstack.secretKey:test}") String localstackSecretKey  // Default "test" for LocalStack
    ) throws URISyntaxException {
        log.info("Creating S3Client for LocalStack DEV environment. Endpoint: {}, Region: {}", localstackEndpoint, awsRegion);
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .endpointOverride(new URI(localstackEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstackAccessKey, localstackSecretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}