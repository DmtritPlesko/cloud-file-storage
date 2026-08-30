package com.cloud.storage.config.minio;

import com.cloud.storage.config.minio.property.MinioProperty;
import io.minio.MinioClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MinioProperty.class)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinioConfig {

    MinioProperty minioProperty;

    @Bean
    public MinioClient minioClient() {
        log.info("Инициализация клиента");
        return MinioClient.builder()
                .endpoint(minioProperty.getUrl())
                .credentials(minioProperty.getAccessKey(), minioProperty.getSecretKey())
                .build();
    }
}
