package com.itimizer.jena.dto;

/** Max input lengths matching the varchar column sizes in {@code changelog-init.yaml}. */
public final class ColumnLengths {

    public static final int DEFAULT = 255;
    public static final int USERNAME = 50;
    public static final int AUTHORITY = 50;
    public static final int ZONE = 64;
    /** BCrypt input limit, not a column size (the stored hash is 68 chars). */
    public static final int PASSWORD = 72;

    private ColumnLengths() {
    }
}