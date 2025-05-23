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

        // Reminder:
        // For production i should use IAM Roles (EC2 Instance Profile)
        // for staging or hmg env i can use aws directly with keys located on ~/.aws/credentials
        // either way the aws sdk will recognize the keys automatically
        return S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
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