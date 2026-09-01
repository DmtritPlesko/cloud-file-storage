package com.cloud.storage.service.storage.minio.usecase.actions;

import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.entity.enums.ResourceType;
import com.cloud.storage.handler.exception.resourse.ResourceAlreadyExistsException;
import com.cloud.storage.service.storage.minio.usecase.BaseUseCase;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
@Component
public class CreateDirectoryUseCase extends BaseUseCase {

    public CreateDirectoryUseCase(MinioClient minioClient,
                                  @Value("${minio.bucket}") String bucketName) {
        super(minioClient, bucketName);
    }

    /**
     * <h1>Создает новую пустую директорию в хранилище MinIO
     *
     * @param path   - путь к создаваемой директории (относительно корня пользователя)
     * @param userId - идентификатор пользователя
     * @return ResourceResponse с информацией о созданной директории:
     * name - имя созданной директории
     * path - полный путь к директории
     * size - всегда 0(так как новая всегда пустая)
     * type - DIRECTORY
     * @throws RuntimeException если:
     *                          Произошла ошибка при обращении к MinIO
     *                          Не удалось создать директорию
     *
     */
    public ResourceResponse execute(String path, UUID userId) {

        String normalizedPath = path.replaceAll("^/", "").replaceAll("/$", "");
        String objectName = fullPath(userId, normalizedPath + "/");

        try {

            try {

                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
                throw new ResourceAlreadyExistsException("Директория уже существует: " + path);
            } catch (ErrorResponseException e) {

                if (e.response().code() != 404) {

                    throw e;
                }
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .contentType("application/x-directory")
                            .build()
            );

            log.info("Директория успешно создана: {}", objectName);

            return ResourceResponse.builder()
                    .name(normalizedPath.substring(normalizedPath.lastIndexOf("/") + 1))
                    .path(objectName)
                    .size(0L)
                    .type(ResourceType.DIRECTORY.toString())
                    .build();

        } catch (IllegalArgumentException e) {

            throw e;
        } catch (Exception e) {

            log.error("Ошибка при создании директории: {}", path, e);
            throw new RuntimeException("Не удалось создать директорию: " + e.getMessage(), e);
        }
    }
}
