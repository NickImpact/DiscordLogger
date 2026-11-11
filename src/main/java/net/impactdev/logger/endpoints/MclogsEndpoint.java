package net.impactdev.logger.endpoints;

import com.google.gson.*;
import net.impactdev.logger.DiscordLogger;
import net.impactdev.logger.files.TrackedFile;
import net.impactdev.logger.files.UploadedFile;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static net.impactdev.logger.Markers.PUBLISHING;

public final class MclogsEndpoint implements PublicationEndpoint {

    private final URI target;
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public MclogsEndpoint() throws URISyntaxException {
        this.target = new URI("https://api.mclo.gs/1/log/");
    }

    @Override
    public List<TrackedFile> publish(String username, List<UploadedFile> files) {
        final Logger logger = DiscordLogger.LOGGER;
        final List<TrackedFile> results = new ArrayList<>();

        logger.info(PUBLISHING, "Processing request for user {}, contains {} files...", username, files.size());
        for (UploadedFile file : files) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.stream()))) {
                StringJoiner joiner = new StringJoiner("\n");
                String current;
                while ((current = reader.readLine()) != null) {
                    joiner.add(current);
                }

                results.add(this.send(file.name(), new JsonPrimitive(joiner.toString())));
            } catch (Exception ex) {
                logger.error(PUBLISHING, "Encountered a problem while uploading file {}", file.name(), ex);
            }

        }

        return results;
    }

    @Override
    public boolean delete(String id, String token) {
        HttpDelete request = new HttpDelete(this.target.resolve(id));
        request.setHeader("Authorization", "Bearer " + token);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(request)) {
                Header header = response.getEntity().getContentType();
                if (response.getEntity().getContentType().getValue().equalsIgnoreCase(ContentType.APPLICATION_JSON.getMimeType())) {
                    final JsonObject result = gson.fromJson(this.parse(response.getEntity().getContent()), JsonObject.class);
                    final boolean successful = result.get("success").getAsBoolean();

                    if (!successful) {
                        DiscordLogger.LOGGER.error("Deletion request failed: {}", result.get("error").getAsString());
                    }

                    return successful;
                }

                throw new IllegalStateException("Response data not of expected mime type (application/json), got (" + header.getValue() + ")");
            }
        } catch (Exception ex) {
            DiscordLogger.LOGGER.error("Encountered a problem whilst executing deletion request...", ex);
        }
        return false;
    }

    private TrackedFile send(String name, JsonElement json) throws Exception {
        final Logger logger = DiscordLogger.LOGGER;

        HttpPost post = new HttpPost(this.target);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
        post.setEntity(new StringEntity("content=" + URLEncoder.encode(json.getAsString(), StandardCharsets.UTF_8), Charsets.UTF_8));

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(post)) {
                int status = response.getStatusLine().getStatusCode();
                logger.debug(PUBLISHING, "Encountered status code: {}", status);

                Header header = response.getEntity().getContentType();
                if (response.getEntity().getContentType().getValue().equalsIgnoreCase(ContentType.APPLICATION_JSON.getMimeType())) {
                    final JsonObject result = gson.fromJson(this.parse(response.getEntity().getContent()), JsonObject.class);
                    final boolean successful = result.get("success").getAsBoolean();
                    if (successful) {
                        return TrackedFile.parse(name, result);
                    } else {
                        String reason = result.get("error").getAsString();
                        logger.error(PUBLISHING, "Failed to publish file with reason: {}", reason);
                    }
                }

                throw new IllegalStateException("Response data not of expected mime type (application/json), got (" + header.getValue() + ")");
            }
        }
    }

    private String parse(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
