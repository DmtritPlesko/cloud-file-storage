package com.cloud.storage.service.storage;

import com.cloud.storage.dto.response.ResourceResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

public interface FileStorageService {

    ResourceResponse getResource(String path, UUID userId);

    List<ResourceResponse> uploadFile(String path, MultipartFile[] file, UUID userId);

    List<ResourceResponse> searchResources(String query, UUID userId);

    StreamingResponseBody download(String path, UUID userId);

    void deleteResource(String path, UUID userId);

    ResourceResponse moveResource(String fromPath, String toPath, UUID userId);

    List<ResourceResponse> listFiles(String path, UUID userId);

    ResourceResponse createDirectory(String path, UUID userId);
}
