package com.safrapay.backend.admin.dto;

import java.util.List;

public record AdminUsuariosResponse(
        long total,
        long emailsVerificados,
        String geradoEm,
        List<AdminUsuarioResponse> usuarios) {
}
