package com.itimizer.jena.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;

/** Request body to register an application user (username, password, roles). */
public record UserCreateDto(
        @NotNull @Size(max = ColumnLengths.USERNAME) String username,
        @NotNull @Size(max = ColumnLengths.PASSWORD) char[] password,
        @NotNull Collection<@Size(max = ColumnLengths.AUTHORITY) String> roles) {
}
