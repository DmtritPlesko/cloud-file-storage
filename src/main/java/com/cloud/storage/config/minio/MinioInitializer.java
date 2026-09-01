package com.cloud.storage.config.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MinioInitializer {

    final MinioClient minioClient;

    @Value("${minio.bucket}")
    String bucketName;

    @PostConstruct
    public void initBucket() {
        try {
            log.info("Выполняю проверку бакета: {}", bucketName);

            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Бакет {} успешно создан", bucketName);
            } else {
                log.info("Бакет {} уже существует", bucketName);
            }

        } catch (Exception e) {
            log.error("Неизвестная ошибка при создании бакета {}", bucketName, e);
            throw new RuntimeException("Невозможно создать бакет: " + bucketName, e);
        }
    }
}
