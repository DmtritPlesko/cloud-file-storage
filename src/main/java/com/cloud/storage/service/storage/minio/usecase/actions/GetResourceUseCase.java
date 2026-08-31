package com.cloud.storage.service.storage.minio.usecase.actions;

import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.entity.enums.ResourceType;
import com.cloud.storage.handler.exception.resourse.InvalidPathException;
import com.cloud.storage.handler.exception.resourse.ResourceNotFoundException;
import com.cloud.storage.service.storage.minio.usecase.BaseUseCase;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class GetResourceUseCase extends BaseUseCase {

    public GetResourceUseCase(MinioClient minioClient, String bucketName) {
        super(minioClient, bucketName);
    }

    /**
     * <h1> Получает информацию о ресурсе (файле или папке) в MinIO
     *
     * @param path   - путь к ресурсу относительно корня пользователя
     * @param userId - идентификатор пользователя
     * @return ResourceResponse - с информацией о ресурсе
     * @throws RuntimeException если:
     *                          <p>Ресурс не найден (404)
     *                          <p>Произошла ошибка MinIO
     *                          <p>Произошла неизвестная ошибка
     *
     */
    public ResourceResponse execute(String path, UUID userId) {

        String objectName = fullPath(userId, path);

        try {

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            String fullPath = stat.object();
            String name = fullPath;

            int lastSlash = fullPath.lastIndexOf("/");
            if (lastSlash != -1) {

                name = fullPath.substring(lastSlash + 1);
            }
            if (name.endsWith("/")) {

                name = name.substring(0, name.length() - 1);
            }
            boolean isDirectory = objectName.endsWith("/");

            return ResourceResponse.builder()
                    .path(objectName)
                    .name(name)
                    .size(isDirectory ? 0 : stat.size())
                    .type(isDirectory ?
                            ResourceType.DIRECTORY.toString() : ResourceType.FILE.toString())
                    .build();

        } catch (ErrorResponseException e) {

            if (e.response().code() == 404) {

                log.warn("Ресурс не найден: {}", path);
                throw new ResourceNotFoundException("Ресурс не найден: " + path);
            }
            log.error("Ошибка MinIO при получении ресурса: {}", path, e);
            throw new InvalidPathException("Ошибка хранилища: " + e.getMessage());

        } catch (Exception e) {

            log.error("Ошибка при получении ресурса: {}", path, e);
            throw new InvalidPathException("Не удалось получить информацию о ресурсе: " + e.getMessage());
        }
    }
}
