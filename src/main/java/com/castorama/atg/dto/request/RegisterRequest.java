package com.castorama.atg.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ATG analogy: the form fields submitted to ProfileFormHandler.create()
 * for new customer registration.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit comporter entre 3 et 50 caractères")
    private String login;

    @Email(message = "Format d'adresse e-mail invalide")
    @NotBlank(message = "L'adresse e-mail est obligatoire")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit comporter au moins 8 caractères")
    private String password;

    @Size(max = 80)
    private String firstName;

    @Size(max = 80)
    private String lastName;

    @Size(max = 20)
    private String phoneNumber;
}
