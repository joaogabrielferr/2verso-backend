package com._verso._verso.articles.mappers;


import com._verso._verso.articles.DTOs.AuthorResponseDTO;
import com._verso._verso.auth.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthorMapper {
    AuthorResponseDTO userToUserResponseDTO(User user);
}