package fr.dossierfacile.common.model;

import org.springframework.web.multipart.MultipartFile;

/**
 * Pairs an uploaded file with its MIME type as detected by Tika (magic bytes).
 * Used to carry the detection result from the pre-processing layer into the service layer,
 * so that detection is performed once per file and never relies on the client-supplied Content-Type.
 */
public record ValidatedFile(MultipartFile file, String detectedMimeType) {}
