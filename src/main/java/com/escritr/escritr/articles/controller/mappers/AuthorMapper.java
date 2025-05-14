package com.escritr.escritr.articles.controller.mappers;


import com.escritr.escritr.articles.controller.DTOs.AuthorResponseDTO;
import com.escritr.escritr.user.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthorMapper {
    AuthorResponseDTO userToUserResponseDTO(User user);
}