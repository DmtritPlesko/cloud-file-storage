package com.cloud.storage.service.storage.minio.usecase.actions;

import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.entity.enums.ResourceType;
import com.cloud.storage.handler.exception.resourse.ResourceNotFoundException;
import com.cloud.storage.service.storage.minio.usecase.BaseUseCase;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class SearchResourcesUseCase extends BaseUseCase {

    public SearchResourcesUseCase(MinioClient minioClient, String bucketName) {
        super(minioClient, bucketName);
    }

    /**
     * <h1>Выполняет поиск файлов по имени в хранилище пользователя.
     *
     * @param query  - поисковый запрос (подстрока для поиска в имени файла)
     * @param userId - идентификатор пользователя
     * @return List  - с информацией о найденных файлах
     * @throws RuntimeException если:
     *                          <p>Произошла ошибка при обращении к MinIO
     *                          <p>Не удалось выполнить поиск
     *
     */
    public List<ResourceResponse> execute(String query, UUID userId) {

        String userFolder = userFolder(userId);

        List<ResourceResponse> results = new ArrayList<>();

        try {

            Iterable<Result<Item>> res = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(userFolder)
                            .recursive(true)
                            .build()
            );

            String searchQuery = query.toLowerCase().trim();

            for (Result<Item> result : res) {
                Item item = result.get();
                String objectName = item.objectName();

                if (objectName.endsWith("/")) {
                    continue;
                }

                String fileName = objectName.substring(objectName.lastIndexOf("/") + 1);

                if (fileName.toLowerCase().contains(searchQuery)) {

                    String folderPath = objectName.substring(0, objectName.lastIndexOf("/") + 1);
                    String relativePath = folderPath.substring(userFolder.length());

                    if (relativePath.endsWith("/")) {

                        relativePath = relativePath.substring(0, relativePath.length() - 1);
                    }

                    ResourceResponse response = ResourceResponse.builder()
                            .path(relativePath)
                            .name(fileName)
                            .size(item.size())
                            .type(relativePath.endsWith("/") ?
                                    ResourceType.DIRECTORY.toString() : ResourceType.FILE.toString())
                            .build();
                    results.add(response);
                }
            }

            log.info("Найдено {} результатов по запросу '{}'", results.size(), query);

        } catch (Exception e) {

            log.error("Ошибка при поиске ресурсов: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Не удалось выполнить поиск: " + e.getMessage());
        }

        return results;
    }
}
