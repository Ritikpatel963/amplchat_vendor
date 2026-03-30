package com.agromarket.ampl_chat.models.api;

public class VendorRegisterResponse {

    private boolean status;
    private String message;
    private String approval_status; // ✅ ADDED

    public boolean isStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getApprovalStatus() {
        return approval_status;
    } // ✅ ADDED
}
// ✅ Removed token, user, vendor — backend no longer returns them on register
