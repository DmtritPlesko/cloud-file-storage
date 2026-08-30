package com.cloud.storage.service.storage.impl;

import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.service.storage.FileStorageService;
import com.cloud.storage.service.storage.minio.MinioStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileStorageServiceImpl implements FileStorageService {

    MinioStorageService minioStorageService;

    @Override
    public ResourceResponse getResource(String path, UUID userId) {

        log.info("Выполняю получения по пути {}", path);

        return minioStorageService.getResource(path, userId);
    }

    @Override
    public List<ResourceResponse> uploadFile(String path, MultipartFile[] file, UUID userId) {

        log.info("выполяню загрузку файлов {}", file.length);

        return minioStorageService.uploadFile(path, file, userId);
    }

    @Override
    public List<ResourceResponse> searchResources(String query, UUID userId) {

        log.info("Поиск ресурсов по запросу: {} ", query);

        return minioStorageService.searchResources(query, userId);
    }

    @Override
    public StreamingResponseBody download(String path, UUID userId) {

        log.info("пытается скачать ресурс {}", path);

        return minioStorageService.downloadFile(path, userId);
    }

    @Override
    public void deleteResource(String path, UUID userId) {

        log.info("пользователь удаляет ресурс {}", path);

        minioStorageService.deleteResource(path, userId);
    }

    @Override
    public ResourceResponse moveResource(String fromPath, String toPath, UUID userId) {

        log.info("свап ресурса {} -> {}",
                fromPath, toPath);

        return minioStorageService.moveResource(fromPath, toPath, userId);
    }

    @Override
    public List<ResourceResponse> listFiles(String path, UUID userId) {

        log.info("список файлов в директории {}", path);

        return minioStorageService.listFiles(path, userId);
    }

    @Override
    public ResourceResponse createDirectory(String path, UUID userId) {

        log.info("Создание директории {} ", path);

        return minioStorageService.createDirectory(path, userId);
    }
}
