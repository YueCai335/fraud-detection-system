package com.yuecai.fraud.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Backing object for the register page; validated with Bean Validation. */
public class RegistrationForm {

    @NotBlank @Size(max = 64)
    private String firstName;

    @NotBlank @Size(max = 64)
    private String lastName;

    @NotBlank @Email @Size(max = 255)
    private String email;

    @NotBlank @Size(min = 3, max = 64)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "may only contain letters, digits, dot, underscore and dash")
    private String username;

    @NotBlank @Size(min = 6, max = 72, message = "must be 6–72 characters")
    private String password;

    @NotBlank
    private String password2;

    public boolean passwordsMatch() {
        return password != null && password.equals(password2);
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPassword2() { return password2; }
    public void setPassword2(String password2) { this.password2 = password2; }
}
