package com.cloud.storage.service.storage.minio.usecase.actions;

import com.cloud.storage.handler.exception.resourse.ResourceNotFoundException;
import com.cloud.storage.service.storage.minio.usecase.BaseUseCase;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
public class DownloadResourceUseCase extends BaseUseCase {

    public DownloadResourceUseCase(MinioClient minioClient, String bucketName) {
        super(minioClient, bucketName);
    }

    /**
     * <h1>Скачивает файл или папку из хранилища MinIO
     *
     * @param path   - путь к ресурсу (файл/папка)
     * @param userId - айдишник пользака
     * @return стрим
     * @throws RuntimeException если:
     *                          <p>Ресурс не найден
     *                          <p>Произошла ошибка MinIO
     *                          <p>Не удалось создать ZIP-архив для папки
     *                          <p>Произошла неизвестная ошибка
     *                          <p>
     *                          <br>
     *                          <__что бы не гурзить файлы в оперативу используем стримин__>
     *
     */
    public StreamingResponseBody execute(String path, UUID userId) {

        try {

            String objectName = fullPath(userId, path);

            log.info("Подготовка к скачиванию ресурса: {} для пользователя: {}", objectName, userId);


            if (path.endsWith("/") || objectName.endsWith("/")) {

                log.info("Скачивание папк как ZIP архива: {}", path);
                return downloadFolderStreaming(objectName, userId);
            }

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            return outputStream -> {

                try (InputStream inputStream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build())) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalRead = 0;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                    }
                    outputStream.flush();

                    log.info("файл скачен");

                } catch (Exception e) {

                    log.error("Ошибка при скачивании файла: {}", e.getMessage(), e);
                    throw new RuntimeException("Ошибка при скачивании файла", e);
                }
            };

        } catch (ErrorResponseException e) {

            if (e.response().code() == 404) {
                log.warn("Ресурс не найден: {}", path);
                throw new ResourceNotFoundException("Ресурс не найден: " + path);
            }
            log.error("Ошибка MinIO при скачивании: {}", path, e);
            throw new RuntimeException("Ошибка хранилища: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Ошибка при скачивании ресурса: {}", path, e);
            throw new RuntimeException("Не удалось скачать ресурс: " + e.getMessage(), e);
        }
    }

    private StreamingResponseBody downloadFolderStreaming(String folderPath, UUID userId) {

        String folder = folderPath.endsWith("/") ? folderPath : folderPath + "/";

        log.info("Подготовка потоковой выгрузки ZIP-архива для папки: {}", folder);

        return outputStream -> {

            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {

                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(folder)
                                .recursive(true)
                                .build()
                );

                int fileCount = 0;

                for (Result<Item> result : results) {
                    Item item = result.get();
                    String objectName = item.objectName();

                    if (objectName.equals(folder)) {

                        continue;
                    }

                    if (objectName.endsWith("/")) {

                        continue;
                    }

                    String relativePath = objectName.substring(folder.length());

                    log.debug("Добавление в ZIP: {}", relativePath);

                    ZipEntry zipEntry = new ZipEntry(relativePath);
                    zos.putNextEntry(zipEntry);

                    try (InputStream fileStream = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectName)
                                    .build())) {
                        fileStream.transferTo(zos);
                    }

                    zos.closeEntry();
                    fileCount++;
                }

                log.info("В ZIP-архив добавлено {} файлов", fileCount);

            } catch (Exception e) {

                log.error("Ошибка при создании ZIP-архива: {}", e.getMessage(), e);
                throw new RuntimeException("Ошибка при создании ZIP-архива", e);
            }
        };
    }
}
