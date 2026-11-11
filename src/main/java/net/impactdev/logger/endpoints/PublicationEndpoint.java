package net.impactdev.logger.endpoints;

import net.impactdev.logger.files.TrackedFile;
import net.impactdev.logger.files.UploadedFile;

import java.util.List;

public interface PublicationEndpoint {

    List<TrackedFile> publish(String username, List<UploadedFile> files);

    boolean delete(String id, String token);

}
