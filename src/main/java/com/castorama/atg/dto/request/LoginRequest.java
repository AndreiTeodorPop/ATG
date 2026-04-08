package com.castorama.atg.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ATG analogy: credentials submitted to the
 * ProfileAuthenticationPipelineServlet / LoginFormHandler.
 *
 * <p>Accepts either email or login username as the identifier,
 * mirroring ATG's flexible credential lookup.</p>
 */
@Data
public class LoginRequest {

    /** E-mail address or login username. */
    @NotBlank(message = "L'identifiant est obligatoire")
    private String credential;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
