package com.cloud.storage.service.storage.minio.usecase.actions;

import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.entity.enums.ResourceType;
import com.cloud.storage.service.storage.minio.usecase.BaseUseCase;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class UploadResourceUseCase extends BaseUseCase {

    public UploadResourceUseCase(MinioClient minioClient, String bucketName) {
        super(minioClient, bucketName);
    }

    /**
     * <h1>Загружает один или несколько файлов в хранилище MinIO в указанную директорию
     *
     * @param path   - путь к директории для загрузки (относительно корня пользователя)
     * @param files  - массив файлов для загрузки
     * @param userId - идентификатор пользователя
     * @return List - с информацией о загруженных файлах
     * @throws RuntimeException если:
     *                          <p>Произошла ошибка MinIO
     *                          <p>Не удалось прочитать файл
     *                          <p>Произошла неизвестная ошибка
     *
     */

    public List<ResourceResponse> execute(String path, MultipartFile[] files, UUID userId) {

        try {

            String normalizedPath = normalizePath(path);

            if (normalizedPath.startsWith("/")) {

                normalizedPath = normalizedPath.substring(1);
            }
            if (!normalizedPath.isEmpty() && !normalizedPath.endsWith("/")) {

                normalizedPath += "/";
            }

            List<ResourceResponse> uploadedResources = new ArrayList<>();

            for (MultipartFile file : files) {
                String filename = file.getOriginalFilename();

                if (filename == null || filename.trim().isEmpty()) {

                    log.warn("имя- пустой");
                    continue;
                }

                String objectName = fullPath(userId, normalizedPath + filename);
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(file.getInputStream(), file.getSize(), -1)
                                .contentType(file.getContentType() != null ?
                                        file.getContentType() :
                                        "application/octet-stream")
                                .build()
                );

                uploadedResources.add(
                        ResourceResponse.builder()
                                .name(filename)
                                .path(path)
                                .size(file.getSize())
                                .type(objectName.endsWith("/") ?
                                        ResourceType.DIRECTORY.toString() : ResourceType.FILE.toString())
                                .build()
                );
            }

            log.info("Успешно загружено {} файлов", uploadedResources.size());
            return uploadedResources;

        } catch (ErrorResponseException e) {

            log.error("Ошибка MinIO при загрузке: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка хранилища: " + e.getMessage(), e);

        } catch (Exception e) {

            log.error("Ошибка при загрузке файлов: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось загрузить файлы: " + e.getMessage(), e);
        }
    }
}
