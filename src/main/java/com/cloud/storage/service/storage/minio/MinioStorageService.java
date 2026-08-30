package com.cloud.storage.service.storage.minio;

import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.entity.enums.ResourceType;
import com.cloud.storage.handler.exception.resourse.ResourceNotFoundException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MinioStorageService {

    final static String USER_FOLDER_PREFIX = "user-";
    final static String USER_FOLDER_SUFFIX = "-files";
    final MinioClient minioClient;
    @Value("${minio.bucket}")
    String bucketName;

    /**
     * <h1>Инициализирует бакет в MinIO при запуске приложения
     * <h3>Проверяет существование бакета и создает его, если он не найден
     *
     * @throws RuntimeException если не удается проверить/создать/подключиться
     */
    @PostConstruct
    public void initBucket() {

        try {

            log.info("Выполняю проверку бакета");
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!found) {

                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Баскет {} успешно создан", bucketName);

            } else log.info("Бакет {} уже существует", bucketName);

        } catch (Exception e) {

            log.error("Неизвестная ошибка при создании бакета {}", bucketName);
            throw new RuntimeException("Невозможно создать бакет");
        }
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
    public ResourceResponse getResource(String path, UUID userId) {

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
                throw new RuntimeException("Ресурс не найден: " + path);
            }
            log.error("Ошибка MinIO при получении ресурса: {}", path, e);
            throw new ResourceNotFoundException("Ошибка хранилища: " + e.getMessage());

        } catch (Exception e) {

            log.error("Ошибка при получении ресурса: {}", path, e);
            throw new RuntimeException("Не удалось получить информацию о ресурсе: " + e.getMessage(), e);
        }
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

    public List<ResourceResponse> uploadFile(String path, MultipartFile[] files, UUID userId) {

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
                    log.warn("Пропущен файл с пустым именем");
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
    public List<ResourceResponse> searchResources(String query, UUID userId) {

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
            throw new RuntimeException("Не удалось выполнить поиск: " + e.getMessage(), e);
        }

        return results;
    }

    /**
     * <h1>Скачивает файл или папку из хранилища MinIO
     *
     * @param path   - путь к ресурсу (файлу или папке)
     * @param userId - идентификатор пользователя
     * @return стриминговое скачивание
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
    public StreamingResponseBody downloadFile(String path, UUID userId) {

        try {

            String objectName = fullPath(userId, path);

            log.info("Подготовка к скачиванию ресурса: {} для пользователя: {}", objectName, userId);


            if (path.endsWith("/") || objectName.endsWith("/")) {

                log.info("Скачивание папки как ZIP-архива: {}", path);
                return downloadFolderStreaming(objectName, userId);
            }

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            log.info("Файл найден. Размер: {} bytes, Content-Type: {}",
                    stat.size(), stat.contentType());

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
                throw new RuntimeException("Ресурс не найден: " + path);
            }
            log.error("Ошибка MinIO при скачивании: {}", path, e);
            throw new RuntimeException("Ошибка хранилища: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Ошибка при скачивании ресурса: {}", path, e);
            throw new RuntimeException("Не удалось скачать ресурс: " + e.getMessage(), e);
        }
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
    public void deleteResource(String path, UUID userId) {
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
    public ResourceResponse moveResource(String fromPath, String toPath, UUID userId) {
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

                    throw new FileNotFoundException("Исходный файл не найден: " + fromPath);
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
                throw new IllegalArgumentException("Файл уже существует в месте назначения: " + toPath);
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
            throw new RuntimeException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {

            log.warn("Ошибка валидации: {}", e.getMessage());
            throw e;
        } catch (Exception e) {

            log.error("Ошибка при перемещении ресурса: {} -> {}", fromPath, toPath, e);
            throw new RuntimeException("Не удалось переместить ресурс: " + e.getMessage(), e);
        }
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
     * @throws RuntimeException/ResourceNotFoundException если произошла ошибка при обращении к MinIO/не найден ресурс
     *
     */
    public List<ResourceResponse> listFiles(String path, UUID userID) {

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
    public ResourceResponse createDirectory(String path, UUID userId) {

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
                throw new IllegalArgumentException("Директория уже существует: " + path);
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


    /**
     * <h1>Вспомогательные методы:
     *
     * <p>userFolder - дириктория пользака
     * <p>fullPath - асболютный путь
     * <p>normalizePath - нормализацяи пути(привод к нужному формвату)
     * <p>extractName - извлечение данных пользователя из стороки
     * <p>downloadFolder - запаковка в ZIPку и отдача на скачиваени (избегаем битые зипки)
     * <p>deleteFolderRecursively - рекурсивное удаление папки с файлыми внутри (сначала корень потом родитель)
     *
     */
    private String userFolder(UUID uuid) {
        return USER_FOLDER_PREFIX + uuid + USER_FOLDER_SUFFIX;
    }

    private String fullPath(UUID uuid, String path) {
        return userFolder(uuid) + "/" + path;
    }

    private String normalizePath(String path) {

        if (path == null || path.isEmpty()) {

            return "";
        }

        String normalized = path.trim().replaceAll("/+", "/");

        if (normalized.startsWith("/")) {

            normalized = normalized.substring(1);
        }
        if (!normalized.isEmpty() && !normalized.endsWith("/")) {

            normalized += "/";
        }

        return normalized;
    }

    private String extractName(String fullPath, String basePath) {

        String relativePath = fullPath.substring(basePath.length());

        if (relativePath.endsWith("/")) {

            relativePath = relativePath.substring(0, relativePath.length() - 1);
        }
        return relativePath;
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
