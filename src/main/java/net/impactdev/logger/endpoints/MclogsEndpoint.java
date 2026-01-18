package net.impactdev.logger.endpoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.impactdev.logger.DiscordLogger;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.impactdev.logger.Markers.PUBLISHING;

public final class MclogsEndpoint implements PublicationEndpoint {

    private final URI target;
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public MclogsEndpoint() throws URISyntaxException {
        this.target = new URI("https://api.mclo.gs/1/log");
    }

    @Override
    public MessageEmbed publish(String username, JsonObject json, EmbedBuilder builder) {
        final Logger logger = DiscordLogger.LOGGER;
        final JsonArray files = json.getAsJsonArray("files");

        logger.info(PUBLISHING, "Processing request for user {}, contains {} files...", username, files.size());
        AtomicInteger counter = new AtomicInteger(1);
        for (JsonElement file : files) {
            final JsonObject data = file.getAsJsonObject();
            final String name = data.get("name").getAsString();
            final JsonElement content = data.get("content").getAsJsonObject().get("value");

            try {
                String url = this.send(content);
                builder.addField(String.format("File %d:", counter.getAndIncrement()), url, false);
            } catch (Exception e) {
                logger.error(PUBLISHING, "Encountered a problem while uploading file {}", name, e);
            }
        }

        return builder.build();
    }

    private String send(JsonElement json) throws Exception {
        final Logger logger = DiscordLogger.LOGGER;

        HttpPost post = new HttpPost(this.target);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
        post.setEntity(new StringEntity("content=" + URLEncoder.encode(json.getAsString(), StandardCharsets.UTF_8), Charsets.UTF_8));

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse response = client.execute(post);

            int status = response.getStatusLine().getStatusCode();
            logger.debug(PUBLISHING, "Encountered status code: {}", status);

            Header header = response.getEntity().getContentType();
            if (response.getEntity().getContentType().getValue().equalsIgnoreCase(ContentType.APPLICATION_JSON.getMimeType())) {
                final JsonObject result = gson.fromJson(this.parse(response.getEntity().getContent()), JsonObject.class);

                Response data = new Response(result);
                if (!data.successful()) {
                    String reason = data.error().orElse("Unknown error response, info not available?");
                    logger.error(PUBLISHING, "Failed to publish file with reason: {}", reason);
                }

                ContentPath content = data.content().orElseThrow(() -> new IllegalStateException("Successful response, but content info missing"));
                logger.info(PUBLISHING, "Received valid response with ID: {}", content.id());

                return content.url();
            }

            throw new IllegalStateException("Response data not of expected mime type (application/json), got (" + header.getValue() + ")");
        }
    }

    private String parse(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private record Response(JsonObject json) {

        public boolean successful() {
            return json.get("success").getAsBoolean();
        }

        public Optional<ContentPath> content() {
            if (this.successful()) {
                return Optional.of(new ContentPath(json.get("id").getAsString(), json.get("url").getAsString()));
            }

            return Optional.empty();
        }

        public Optional<String> error() {
            return Optional.ofNullable(json.get("error")).map(JsonElement::getAsString);
        }

    }

    private record ContentPath(String id, String url) {}

}
