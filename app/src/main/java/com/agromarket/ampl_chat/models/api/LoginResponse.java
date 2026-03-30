package com.agromarket.ampl_chat.models.api;

public class LoginResponse {

    public boolean status;
    public String message;
    public String token;
    public String approval_status; // ✅ ADDED — "pending" | "approved" | "rejected"
    public User user;
    public int agent_id;

    public class User {
        public int id;
        public String name;
        public String email;
        public String role;
    }
}