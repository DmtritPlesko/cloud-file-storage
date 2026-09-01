package com.cloud.storage.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ответка в случае ошибок/проблем")
public class ErrorResponse {

    @Schema(description = "время оишбки")
    private LocalDateTime timestamp;

    @Schema(description = "статус")
    private Integer status;

    @Schema(description = "что за ошибка")
    private String error;

    @Schema(description = "сообение подробное")
    private String message;

    @Schema(description = "где именно прикол")
    private String path;

    @Schema(description = "статус код ошибки")
    private String code;

    @Schema(description = "детали которые привели в ошибке")
    private Object details;
}