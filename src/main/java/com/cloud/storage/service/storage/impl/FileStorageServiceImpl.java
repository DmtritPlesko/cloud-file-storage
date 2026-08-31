package com.cloud.storage.service.storage.impl;

import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.service.storage.FileStorageService;
import com.cloud.storage.service.storage.minio.MinioFacade;
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

    MinioFacade minioFacadeService;

    @Override
    public ResourceResponse getResource(String path, UUID userId) {

        return minioFacadeService.getResource(path, userId);
    }

    @Override
    public List<ResourceResponse> uploadFile(String path, MultipartFile[] file, UUID userId) {

        return minioFacadeService.uploadFile(path, file, userId);
    }

    @Override
    public List<ResourceResponse> searchResources(String query, UUID userId) {

        return minioFacadeService.searchResources(query, userId);
    }

    @Override
    public StreamingResponseBody download(String path, UUID userId) {

        return minioFacadeService.download(path, userId);
    }

    @Override
    public void deleteResource(String path, UUID userId) {

        minioFacadeService.deleteResource(path, userId);
    }

    @Override
    public ResourceResponse moveResource(String fromPath, String toPath, UUID userId) {

        return minioFacadeService.moveResource(fromPath, toPath, userId);
    }

    @Override
    public List<ResourceResponse> listFiles(String path, UUID userId) {

        return minioFacadeService.listFiles(path, userId);
    }

    @Override
    public ResourceResponse createDirectory(String path, UUID userId) {

        return minioFacadeService.createDirectory(path, userId);
    }
}
