package com.cloud.storage.mapper;

import com.cloud.storage.dto.response.AuthResponse;
import com.cloud.storage.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AuthMapper {

    AuthResponse toResponse(User user);
}
