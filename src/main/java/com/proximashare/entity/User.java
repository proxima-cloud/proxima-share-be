package com.proximashare.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String googleId;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    private String emailVerificationToken;

    private LocalDateTime tokenExpiryDate; // Token expiration timestamp

    private String profilePictureUrl;

    @Column(nullable = false)
    private String authProvider; // "LOCAL", "GOOGLE", etc.

    //    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
//    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Role> roles;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileMetadata> uploadedFiles;
}
