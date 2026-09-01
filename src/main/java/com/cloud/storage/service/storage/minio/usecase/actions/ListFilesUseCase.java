package com.cloud.storage.service.storage.minio.usecase.actions;

import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.handler.exception.resourse.ResourceNotFoundException;
import com.cloud.storage.service.storage.minio.usecase.BaseUseCase;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ListFilesUseCase extends BaseUseCase {

    public ListFilesUseCase(MinioClient minioClient,
                            @Value("${minio.bucket}")String bucketName) {
        super(minioClient, bucketName);
    }

    /**
     * <h1>Получает список файлов и папок в указанной директории
     *
     * @param path   - путь к директории (относительно корня пользователя)
     * @param userID - идентификатор пользователя
     * @return List - с информацией о содержимом папки:
     * path - путь к папке, в которой лежит ресурс
     * name - имя файла или папки
     * size - размер файла в байтах
     * type - тип ресурса (FILE или DIRECTORY)
     * @throws ResourceNotFoundException если произошла ошибка при обращении к MinIO/не найден ресурс
     *
     */
    public List<ResourceResponse> execute(String path, UUID userID) {

        String fullPath = fullPath(userID, path);

        String normalizedPath = normalizePath(fullPath);

        List<ResourceResponse> resources = new ArrayList<>();

        try {

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(normalizedPath)
                            .recursive(false)
                            .delimiter("/")
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                String itemName = item.objectName();

                if (itemName.equals(normalizedPath)) {
                    continue;
                }

                String name = extractName(itemName, normalizedPath);

                boolean isDirectory = item.isDir();

                ResourceResponse response = ResourceResponse.builder()
                        .path(path)
                        .name(name)
                        .size(isDirectory ? null : item.size())
                        .type(isDirectory ? "DIRECTORY" : "FILE")
                        .build();
                resources.add(response);
            }

        } catch (Exception e) {

            log.error("Ошибка при получении списка файлов из MinIO: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Папка не найдена или недоступна: " + path);
        }

        return resources;
    }
}
