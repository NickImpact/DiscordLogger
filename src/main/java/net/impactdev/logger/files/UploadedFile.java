package net.impactdev.logger.files;

import java.io.InputStream;

public record UploadedFile(String name, InputStream stream)  { }
