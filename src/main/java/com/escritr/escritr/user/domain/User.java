package com.escritr.escritr.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users",schema = "escritr")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String username;
    private String email;
    private String password;
    private String name;
    private int tokenVersion = 0;

    public User(String username,String email,String password,String name){
        this.username = username;
        this.email = email;
        this.password = password;
        this.name = name;
    }


    public void incrementTokenVersion() {
        this.tokenVersion++;
    }



    @Override
    public String toString(){
        return getUsername() + "," + getEmail() + "," + getPassword() + "," + getId() + "," + getTokenVersion();

    }

}
