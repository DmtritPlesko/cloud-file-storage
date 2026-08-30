package com.cloud.storage.controller.imp;

import com.cloud.storage.annotation.CurrentUser;
import com.cloud.storage.controller.ResourceControllerApi;
import com.cloud.storage.dto.response.ResourceResponse;
import com.cloud.storage.service.storage.FileStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResourceControllerImpl implements ResourceControllerApi {


    FileStorageService fileStorageService;

    @Override
    @GetMapping(path = "/resource")
    public ResponseEntity<?> getResource(@RequestParam String path,
                                         @CurrentUser UUID userId) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(fileStorageService.getResource(path, userId));
    }

    @Override
    @DeleteMapping(path = "/resource")
    public ResponseEntity<Void> deleteResource(@RequestParam String path,
                                               @CurrentUser UUID userId) {

        fileStorageService.deleteResource(path, userId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @Override
    @GetMapping(path = "/resource/download")
    public ResponseEntity<StreamingResponseBody> downloadResource(
            @RequestParam String path,
            @CurrentUser UUID userId) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(fileStorageService.download(path, userId));
    }

    @Override
    @PostMapping(path = "/resource/move")
    public ResponseEntity<ResourceResponse> moveResource(@RequestParam("from") String from,
                                                         @RequestParam("to") String to,
                                                         @CurrentUser UUID userId) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(fileStorageService.moveResource(from, to, userId));
    }

    @Override
    @GetMapping(path = "/resource/search")
    public ResponseEntity<List<ResourceResponse>> searchResource(@RequestParam String query,
                                                                 @CurrentUser UUID userId) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ArrayList<>(fileStorageService.searchResources(query, userId)));
    }

    @Override
    @PostMapping(path = "/resource")
    public ResponseEntity<List<ResourceResponse>> uploadResource(@RequestParam String path,
                                                                 @RequestParam("object") MultipartFile[] files,
                                                                 @CurrentUser UUID userId) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(fileStorageService.uploadFile(path, files, userId));
    }

    @Override
    @GetMapping(path = "/directory")
    public ResponseEntity<List<ResourceResponse>> getFilesInDirectory(@RequestParam String path,
                                                                      @CurrentUser UUID userId) {


        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ArrayList<>(fileStorageService.listFiles(path, userId)));
    }

    @Override
    @PostMapping(path = "/directory")
    public ResponseEntity<ResourceResponse> createBlankRepository(@RequestParam String path,
                                                                  @CurrentUser UUID userId) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(fileStorageService.createDirectory(path, userId));
    }

}
