package com.escritr.escritr.auth.controller.mappers;

import com.escritr.escritr.articles.controller.DTOs.AuthorResponseDTO;
import com.escritr.escritr.auth.controller.DTOs.UserLoginDTO;
import com.escritr.escritr.user.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserLoginDTO userToUserUserLoginDTO(User user);
}