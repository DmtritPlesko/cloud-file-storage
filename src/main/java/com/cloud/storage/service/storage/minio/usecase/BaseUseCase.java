package com.cloud.storage.service.storage.minio.usecase;

import io.minio.MinioClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class BaseUseCase {

    final static String USER_FOLDER_PREFIX = "user-";
    final static String USER_FOLDER_SUFFIX = "-files";
    final MinioClient minioClient;
    final String bucketName;

    protected String userFolder(UUID uuid) {

        return USER_FOLDER_PREFIX + uuid + USER_FOLDER_SUFFIX;
    }

    protected String fullPath(UUID uuid, String path) {

        return userFolder(uuid) + "/" + path;
    }

    protected String normalizePath(String path) {

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

    protected String extractName(String fullPath, String basePath) {

        String relativePath = fullPath.substring(basePath.length());

        if (relativePath.endsWith("/")) {

            relativePath = relativePath.substring(0, relativePath.length() - 1);
        }
        return relativePath;
    }
}
