package com.cloud.storage.service.storage.minio;

import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.service.storage.minio.usecase.actions.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MinioFacade {

    CreateDirectoryUseCase createDirectoryUseCase;
    DeleteResourceUseCase deleteResourceUseCase;
    DownloadResourceUseCase downloadResourceUseCase;
    GetResourceUseCase getResourceUseCase;
    ListFilesUseCase listFilesUseCase;
    MoveResourceUseCase moveResourceUseCase;
    SearchResourcesUseCase searchResourcesUseCase;
    UploadResourceUseCase uploadResourceUseCase;

    public ResourceResponse getResource(String path, UUID userId) {

        log.info("Facade: Выполняю получения по пути {}", path);
        return getResourceUseCase.execute(path, userId);
    }

    public List<ResourceResponse> uploadFile(String path, MultipartFile[] file, UUID userId) {

        log.info("Facade: выполяню загрузку файлов {}", file.length);
        return uploadResourceUseCase.execute(path, file, userId);
    }

    public List<ResourceResponse> searchResources(String query, UUID userId) {

        log.info("Facade: Поиск ресурсов по запросу: {} ", query);
        return searchResourcesUseCase.execute(query, userId);
    }

    public StreamingResponseBody download(String path, UUID userId) {

        log.info("Facade: пытается скачать ресурс {}", path);
        return downloadResourceUseCase.execute(path, userId);
    }

    public void deleteResource(String path, UUID userId) {

        log.info("Facade: пользователь удаляет ресурс {}", path);
        deleteResourceUseCase.execute(path, userId);
    }

    public ResourceResponse moveResource(String fromPath, String toPath, UUID userId) {

        log.info("Facade: свап ресурса {} -> {}",
                fromPath, toPath);
        return moveResourceUseCase.execute(fromPath, toPath, userId);
    }

    public List<ResourceResponse> listFiles(String path, UUID userId) {

        log.info("Facade: список файлов в директории {}", path);
        return listFilesUseCase.execute(path, userId);
    }

    public ResourceResponse createDirectory(String path, UUID userId) {

        log.info("Facade: Создание директории {} ", path);
        return createDirectoryUseCase.execute(path, userId);
    }
}
