package com.cloud.storage.service.storage.minio.usecase.actions;

import com.cloud.storage.service.storage.minio.usecase.BaseUseCase;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class DeleteResourceUseCase extends BaseUseCase {

    public DeleteResourceUseCase(MinioClient minioClient, String bucketName) {
        super(minioClient, bucketName);
    }

    /**
     * <h1>Удаляет файл или папку из хранилища MinIO
     *
     * @param path   - путь к ресурсу (файл/папка)
     * @param userId - идентификатор пользователя (от @CurrentUser)
     * @throws RuntimeException если:
     *                          <p>Ресурс не найден
     *                          <p>Произошла ошибка MinIO
     *                          <p>Не удалось удалить ресурс
     *
     */
    public void execute(String path, UUID userId) {
        String objectName = fullPath(userId, path);

        try {

            if (objectName.endsWith("/")) {

                log.info("Удаление папки рекурсивно: {}", objectName);
                deleteFolderRecursively(objectName);
            } else {

                log.info("Удаление файла: {}", objectName);
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
                log.info("Файл удален: {}", objectName);
            }

            log.info("Ресурс успешно удален: {}", objectName);

        } catch (Exception e) {

            log.error("Ошибка при удалении ресурса: {}", path, e);
            throw new RuntimeException("Не удалось удалить ресурс: " + e.getMessage(), e);
        }
    }

    private void deleteFolderRecursively(String folderPath) throws Exception {

        String folder = folderPath.endsWith("/") ? folderPath : folderPath + "/";

        log.info("Рекурсивное удаление папки: {}", folder);

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(folder)
                        .recursive(true)
                        .build()
        );

        List<String> objectsToDelete = new ArrayList<>();

        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();

            if (!objectName.equals(folder)) {

                objectsToDelete.add(objectName);
            }
        }

        if (objectsToDelete.isEmpty()) {

            log.info("Папка пуста: {}", folder);
        } else {

            log.info("Найдено {} объектов для удаления", objectsToDelete.size());

            for (String objectName : objectsToDelete) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
                log.debug("Удален объект: {}", objectName);
            }
            log.info("Все объекты в папке удалены");
        }

        try {

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(folder)
                            .build()
            );
            log.info("Папка удалена: {}", folder);
        } catch (Exception e) {

            log.warn("Не удалось удалить папку: {}", e.getMessage());
        }
    }
}
