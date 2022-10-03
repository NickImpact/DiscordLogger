package net.impactdev.logger.listening;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.impactdev.logger.DiscordLogger;
import net.impactdev.logger.Markers;
import org.apache.commons.codec.Charsets;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class LoggingListener extends ListenerAdapter {

    private final URI POST;
    private final Gson GSON = new Gson();
    private final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();

    public LoggingListener() throws URISyntaxException {
        POST = new URI("https://api.paste.gg/v1/pastes");
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        DiscordLogger.LOGGER.info(Markers.JOIN, "Joining Guild: " + event.getGuild().getName());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message message = event.getMessage();
        if(!message.getAttachments().isEmpty()) {
            if(event.getMember() == null) {
                DiscordLogger.LOGGER.info(Markers.TRANSLATOR, "Attachments with no associated member, ignoring...");
                return;
            }

            Member member = event.getMember();
            this.compose(message.getAttachments()).thenAccept(files -> {
                String id = "";
                if(!files.isEmpty()) {
                    JsonElement json = this.prepare(member, files);
                    try {
                        DiscordLogger.LOGGER.info(Markers.TRANSLATOR, "Attempting to post to paste.gg...");
                        JsonElement element = this.post(json);
                        if(element.getAsJsonObject().has("id")) {
                            id = element.getAsJsonObject().getAsJsonPrimitive("id").getAsString();
                            DiscordLogger.LOGGER.info(Markers.TRANSLATOR, "Attachments with ID " + id + " uploaded, deletion key: " +
                                    element.getAsJsonObject().getAsJsonPrimitive("deletion_key").getAsString());
                        } else {
                            DiscordLogger.LOGGER.error(Markers.TRANSLATOR, "Supposed successful JSON with no ID marker, ignoring...");
                        }
                    } catch (Exception e) {
                        DiscordLogger.LOGGER.error(Markers.TRANSLATOR, "Encountered an exception during post processing", e);
                    }
                }

                if(!id.isEmpty()) {
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(0x34ebde);
                    builder.setThumbnail("https://wegotthiscovered.com/wp-content/uploads/2022/01/bidoof-pokemon-company-short.jpg");
                    builder.addField("We Truly Adore Pasting", "We've uploaded your logs to https://paste.gg in an effort to aid readers who don't wish to download your logs!", false);
                    builder.addField("Your Paste.gg Link", "https://paste.gg/p/anonymous/" + id, false);
                    builder.setFooter("Translated For: " + member.getUser().getName() + "#" + member.getUser().getDiscriminator());
                    builder.setTimestamp(LocalDateTime.now());

                    MessageEmbed embed = builder.build();
                    List<MessageEmbed> embeds = new ArrayList<>();
                    embeds.add(embed);
                    event.getMessage().replyEmbeds(embeds)
                            .mentionRepliedUser(false)
                            .queue();
                }
            });
        }
    }

    private JsonElement prepare(Member member, JsonArray files) {
        User user = member.getUser();

        JsonObject json = new JsonObject();
        json.add("name", new JsonPrimitive(user.getName() + "#" + user.getDiscriminator() + "'s Logs"));
        json.add("files", files);

        return json;
    }

    private boolean valid(String extension) {
        return extension.equals("txt") ||
                extension.equals("log") ||
                extension.equals("conf") ||
                extension.equals("json") ||
                extension.equals("yml") ||
                extension.equals("info");
    }

    private CompletableFuture<JsonArray> compose(List<Message.Attachment> attachments) {
        AtomicBoolean pinged = new AtomicBoolean();
        List<CompletableFuture<JsonElement>> futures = attachments.stream()
                .filter(attachment -> {
                    String extension = attachment.getFileExtension();
                    return extension != null && this.valid(extension);
                })
                .peek(attachment -> {
                    if(!pinged.get()) {
                        DiscordLogger.LOGGER.info(Markers.TRANSLATOR, "Found valid attachments to translate, processing now...");
                        pinged.set(true);
                    }
                })
                .map(attachment -> attachment.retrieveInputStream().thenApply(file -> {
                    DiscordLogger.LOGGER.info(Markers.TRANSLATOR, "File downloaded (" + attachment.getFileName() + "), converting to JSON...");

                    try {
                        return this.file(attachment, file);
                    } catch (Exception e) {
                        DiscordLogger.LOGGER.error(Markers.TRANSLATOR, "Failed to serialize the file to JSON...", e);
                        return null;
                    }
                }))
                .filter(Objects::nonNull)
                .toList();

        if(futures.isEmpty()) {
            return CompletableFuture.completedFuture(new JsonArray());
        }

        return this.compose$combine(futures);
    }

    private CompletableFuture<JsonArray> compose$combine(List<CompletableFuture<JsonElement>> elements) {
        JsonArray json = new JsonArray();
        BiFunction<JsonElement, JsonArray, JsonArray> consumer = (element, array) -> {
            array.add(element);
            return array;
        };

        CompletableFuture<JsonArray> result = elements.get(0).thenApply(element -> consumer.apply(element, json));
        for(int i = 1; i < elements.size(); i++) {
            final int index = i;
            result = result.thenCompose(array -> elements.get(index).thenApply(element -> consumer.apply(element, json)));
        }

        return result;
    }

    private JsonElement file(Message.Attachment attachment, InputStream stream) throws IOException {
        JsonObject json = new JsonObject();
        json.add("name", new JsonPrimitive(attachment.getFileName()));

        JsonObject content = new JsonObject();
        content.add("format", new JsonPrimitive("text"));
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringJoiner joiner = new StringJoiner("\n");
            String current;
            while((current = reader.readLine()) != null) {
                joiner.add(current);
            }

            content.add("value", new JsonPrimitive(joiner.toString()));
        }

        json.add("content", content);
        return json;
    }

    private JsonElement post(JsonElement json) throws Exception {
        HttpPost post = new HttpPost(this.POST);
        post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        post.setEntity(new StringEntity(PRETTY.toJson(json), Charsets.UTF_8));

        try(CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();
            if(status != 201) {
                String result = this.returnStringFromInputStream(response.getEntity().getContent());
                DiscordLogger.LOGGER.error(result);
                JsonObject callback = GSON.fromJson(result, JsonObject.class);
                String reason;
                if(callback.has("message") && callback.get("message").isJsonPrimitive()) {
                    reason = callback.getAsJsonPrimitive("message").getAsString();
                } else if(callback.getAsJsonPrimitive("error").getAsString().equals("bad_json")) {
                    reason = "Unacceptable JSON, perhaps due to incompatible characters?";
                } else {
                    reason = "Unspecified";
                }

                throw new Exception("Received Status Code: " + status + " (" + reason + ")");
            }

            return GSON.fromJson(this.returnStringFromInputStream(response.getEntity().getContent()), JsonObject.class)
                    .getAsJsonObject("result");
        }
    }

    private String returnStringFromInputStream(InputStream in) {
        return new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining(System.lineSeparator()));
    }
}
