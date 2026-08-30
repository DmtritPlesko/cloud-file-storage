package com.cloud.storage.controller;

import com.cloud.storage.dto.response.ResourceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

@Tag(name = "Resource Management", description = "API for managing files and directories")
@RequestMapping("/api")
public interface ResourceControllerApi {

    @GetMapping("/resource")
    @Operation(summary = "Get resource info", description = "Returns information about a file or directory")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resource found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid path",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    ResponseEntity<?> getResource(
            @Parameter(description = "Path to resource", required = true)
            @RequestParam String path,
            @Parameter(hidden = true) UUID userId
    );

    @DeleteMapping("/resource")
    @Operation(summary = "Delete resource", description = "Deletes a file or directory")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Resource deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid path",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    ResponseEntity<Void> deleteResource(
            @Parameter(description = "Path to resource", required = true)
            @RequestParam String path,
            @Parameter(hidden = true) UUID userId
    );

    @GetMapping("/resource/download")
    @Operation(summary = "Download resource", description = "Downloads a file or directory as ZIP")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resource downloaded successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid path",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    ResponseEntity<StreamingResponseBody> downloadResource(
            @Parameter(description = "Path to resource", required = true)
            @RequestParam String path,
            @Parameter(hidden = true) UUID userId
    );

    @PostMapping("/resource/move")
    @Operation(summary = "Move resource", description = "Moves a file from one location to another")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resource moved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid path or destination",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Source resource not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Resource already exists at destination",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    ResponseEntity<ResourceResponse> moveResource(
            @Parameter(description = "Source path", required = true)
            @RequestParam("from") String from,
            @Parameter(description = "Destination path", required = true)
            @RequestParam("to") String to,
            @Parameter(hidden = true) UUID userId
    );

    @GetMapping("/resource/search")
    @Operation(summary = "Search resources", description = "Searches for files by name")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Search completed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Empty search query",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    ResponseEntity<List<ResourceResponse>> searchResource(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query,
            @Parameter(hidden = true) UUID userId
    );

    @PostMapping("/resource")
    @Operation(summary = "Upload resources", description = "Uploads one or more files")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Files uploaded successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or file too large",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "File already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    ResponseEntity<List<ResourceResponse>> uploadResource(
            @Parameter(description = "Destination path", required = true)
            @RequestParam String path,
            @Parameter(description = "Files to upload", required = true)
            @RequestParam("object") MultipartFile[] files,
            @Parameter(hidden = true) UUID userId
    );

    @GetMapping("/directory")
    @Operation(summary = "List directory contents", description = "Returns list of files and directories in a path")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Directory contents listed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid path",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Directory not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    ResponseEntity<List<ResourceResponse>> getFilesInDirectory(
            @Parameter(description = "Directory path", required = true)
            @RequestParam String path,
            @Parameter(hidden = true) UUID userId
    );

    @PostMapping("/directory")
    @Operation(summary = "Create directory", description = "Creates a new empty directory")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Directory created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResourceResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid path",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Directory already exists",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    ResponseEntity<ResourceResponse> createBlankRepository(
            @Parameter(description = "Directory path", required = true)
            @RequestParam String path,
            @Parameter(hidden = true) UUID userId
    );
}
