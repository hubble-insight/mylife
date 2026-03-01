package com.mylife.model;

/**
 * File Type Enumeration
 */
public enum FileType {
    DOCUMENT("document"),
    VIDEO("video"),
    AUDIO("audio"),
    IMAGE("image"),
    OTHER("other");

    private final String displayName;

    FileType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static FileType fromExtension(String extension) {
        if (extension == null) {
            return OTHER;
        }
        String ext = extension.toLowerCase();
        return switch (ext) {
            case "doc", "docx", "pdf", "txt", "xls", "xlsx", "ppt", "pptx", "csv", "md" -> DOCUMENT;
            case "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> VIDEO;
            case "mp3", "wav", "flac", "aac", "ogg", "wma" -> AUDIO;
            case "jpg", "jpeg", "png", "gif", "bmp", "webp" -> IMAGE;
            default -> OTHER;
        };
    }
}
