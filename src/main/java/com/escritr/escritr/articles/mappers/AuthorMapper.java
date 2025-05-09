package com.escritr.escritr.articles.mappers;


import com.escritr.escritr.articles.DTOs.AuthorResponseDTO;
import com.escritr.escritr.user.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthorMapper {
    AuthorResponseDTO userToUserResponseDTO(User user);
}