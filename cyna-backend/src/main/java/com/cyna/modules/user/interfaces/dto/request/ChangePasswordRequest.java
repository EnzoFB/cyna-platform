package com.cyna.modules.user.interfaces.dto.request;

public record ChangePasswordRequest(String currentPassword, String newPassword) {}
