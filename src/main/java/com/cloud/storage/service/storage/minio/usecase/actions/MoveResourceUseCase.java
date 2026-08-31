package com.cloud.storage.service.storage.minio.usecase.actions;

import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.entity.enums.ResourceType;
import com.cloud.storage.handler.exception.resourse.ResourceAlreadyExistsException;
import com.cloud.storage.handler.exception.resourse.ResourceNotFoundException;
import com.cloud.storage.service.storage.minio.usecase.BaseUseCase;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.UUID;

@Slf4j
@Component
public class MoveResourceUseCase extends BaseUseCase {

    public MoveResourceUseCase(MinioClient minioClient, String bucketName) {
        super(minioClient, bucketName);
    }

    /**
     * <h1>Перемещает файл/директорию/меняет имя(файла + дира) в хранилище MinIO.
     *
     * @param fromPath - путь к исходному файлу (относительно корня пользователя)
     * @param toPath   - путь назначения (относительно корня пользователя)
     * @param userId   - идентификатор пользователя
     * @return ResourceResponse - с информацией о перемещенном файле
     * <p>path - путь к папке где находится файл
     * <p>name - имя файла
     * <p>size - размер файла в байтах
     * <p>type - тип ресурса (FILE/DIRECTORY)
     * @throws RuntimeException если:
     *                          <p>Исходный файл не найден
     *                          <p>Файл уже существует в месте назначения
     *                          <p>Не удалось создать папку назначения
     *                          <p>Ошибка при удалении исходного файла
     *                          <p>Файл не скопировался в новое место
     *                          <p>Произошла неизвестная ошибка
     *                          <p>Ошибка при копировании файла
     *
     */
    public ResourceResponse execute(String fromPath, String toPath, UUID userId) {
        String fromObjectName = fullPath(userId, fromPath);
        String toObjectName = fullPath(userId, toPath);

        log.info("Перемещение ресурса: {} -> {} пользователем: {}",
                fromObjectName, toObjectName, userId);

        try {

            try {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fromObjectName)
                                .build()
                );
                log.info("Исходный файл найден: {}", fromObjectName);
            } catch (ErrorResponseException e) {

                if (e.response().code() == 404) {

                    throw new ResourceNotFoundException("Исходный файл не найден: " + fromPath);
                }
                throw e;
            }

            try {

                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(toObjectName)
                                .build()
                );
                throw new ResourceAlreadyExistsException("Файл уже существует в месте назначения: " + toPath);
            } catch (ErrorResponseException e) {

                if (e.response().code() != 404) {
                    throw e;
                }
            }

            String folderPath = toObjectName.substring(0, toObjectName.lastIndexOf("/") + 1);
            if (!folderPath.isEmpty() && !folderPath.equals("/")) {

                try {

                    minioClient.statObject(
                            StatObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(folderPath)
                                    .build()
                    );
                } catch (ErrorResponseException e) {

                    if (e.response().code() == 404) {

                        log.info("Создание папки: {}", folderPath);
                        minioClient.putObject(
                                PutObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(folderPath)
                                        .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                                        .build()
                        );
                    } else {
                        throw e;
                    }
                }
            }

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(toObjectName)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(fromObjectName)
                                    .build())
                            .build()
            );
            log.info("Файл скопирован: {} -> {}", fromObjectName, toObjectName);

            try {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(toObjectName)
                                .build()
                );
                log.info("Файл в новом месте найден");
            } catch (ErrorResponseException e) {

                log.error("Файл не найден в новом месте после копирования!");
                throw new RuntimeException("Файл не скопировался в новое место");
            }

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fromObjectName)
                            .build()
            );

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(toObjectName)
                            .build());

            log.info("Исходный файл удален: {}", fromObjectName);

            log.info("Ресурс успешно перемещен: {} -> {}", fromObjectName, toObjectName);

            return ResourceResponse.builder()
                    .path(folderPath)
                    .name(toPath)
                    .size(stat.size())
                    .type(toPath.endsWith("/") ?
                            ResourceType.DIRECTORY.toString() : ResourceType.FILE.toString())
                    .build();

        } catch (FileNotFoundException e) {

            log.warn("Исходный ресурс не найден: {}", fromPath);
            throw new ResourceNotFoundException(e.getMessage());
        } catch (IllegalArgumentException e) {

            log.warn("Ошибка валидации: {}", e.getMessage());
            throw e;
        } catch (Exception e) {

            log.error("Ошибка при перемещении ресурса: {} -> {}", fromPath, toPath, e);
            throw new RuntimeException("Не удалось переместить ресурс: " + e.getMessage(), e);
        }
    }

}
