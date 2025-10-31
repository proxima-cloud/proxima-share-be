package com.proximashare.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proximashare.entity.Role;
import com.proximashare.entity.User;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserData {
    private final Long id;
    private final String username;
    private final String email;
    private final Set<String> roles;

    @JsonInclude(JsonInclude.Include.NON_NULL)  // for ignoring the null fields
    private String profilePicture;

    // Constructor to convert User entity to UserData
    public UserData(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.profilePicture = user.getProfilePictureUrl();
        this.roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    // Static factory method for convenience
    public static UserData fromUser(User user) {
        return new UserData(user);
    }

}
