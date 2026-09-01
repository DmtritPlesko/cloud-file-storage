package com.cloud.storage.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ответка от ресурса")
public class ResourceResponse {

    @Schema(description = "путь до ресурса")
    String path;

    @Schema(description = "имя ресурса")
    String name;

    @Schema(description = "размер ресурса")
    Long size;

    @Schema(description = "тип ресурса")
    String type;
}
