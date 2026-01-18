package net.impactdev.logger.listening;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import net.impactdev.logger.endpoints.MclogsEndpoint;
import net.impactdev.logger.endpoints.PublicationEndpoint;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

public class LoggingListener extends ListenerAdapter {

    private final PublicationEndpoint endpoint;

    public LoggingListener() throws Exception {
        this.endpoint = new MclogsEndpoint();
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
                DiscordLogger.LOGGER.info(Markers.PUBLISHING, "Attachments with no associated member, ignoring...");
                return;
            }

            Member member = event.getMember();
            this.compose(message.getAttachments()).thenAccept(files -> {
                if (!files.isEmpty()) {
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(0x34ebde);
                    builder.setThumbnail("https://wegotthiscovered.com/wp-content/uploads/2022/01/bidoof-pokemon-company-short.jpg");
                    builder.addField("We Truly Adore Pasting", "Your logs have been uploaded to mclo.gs to aid viewers who wish not to download the files!", false);
                    builder.setFooter("Translated For: " + member.getUser().getName() + "#" + member.getUser().getDiscriminator());
                    builder.setTimestamp(LocalDateTime.now());

                    JsonObject payload = this.prepare(member, files);
                    MessageEmbed embed = this.endpoint.publish(member.getEffectiveName(), payload, builder);

                    event.getMessage()
                            .replyEmbeds(Collections.singleton(embed))
                            .mentionRepliedUser(false)
                            .queue(
                                    success -> DiscordLogger.LOGGER.info(Markers.PUBLISHING, "Detailed response sent successfully"),
                                    fatal -> DiscordLogger.LOGGER.error(Markers.PUBLISHING, "Failed to send message", fatal));

                }
            });
        }
    }

    private JsonObject prepare(Member member, JsonArray files) {
        User user = member.getUser();

        JsonObject json = new JsonObject();
        json.add("name", new JsonPrimitive(user.getEffectiveName() + "'s Logs"));
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
                        DiscordLogger.LOGGER.info(Markers.PUBLISHING, "Found valid attachments to translate, processing now...");
                        pinged.set(true);
                    }
                })
                .map(attachment -> attachment.getProxy().download().thenApplyAsync(file -> {
                    DiscordLogger.LOGGER.info(Markers.PUBLISHING, "File downloaded (" + attachment.getFileName() + "), converting to JSON...");

                    try {
                        return this.file(attachment, file);
                    } catch (Exception e) {
                        DiscordLogger.LOGGER.error(Markers.PUBLISHING, "Failed to serialize the file to JSON...", e);
                        return null;
                    }
                }, DiscordLogger.EXECUTOR))
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
}
