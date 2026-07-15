package com.cinnamonmiracle.auth.dto;

/** Body of POST /api/auth/register (authController.register). */
public class RegisterRequest {
    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public String role;
    public String phone;
    public String address;
    public String profilePicture;
}
