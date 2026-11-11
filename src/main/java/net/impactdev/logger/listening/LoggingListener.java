package net.impactdev.logger.listening;

import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.impactdev.logger.DiscordLogger;
import net.impactdev.logger.Markers;
import net.impactdev.logger.components.ComponentBuilder;
import net.impactdev.logger.endpoints.MclogsEndpoint;
import net.impactdev.logger.endpoints.PublicationEndpoint;
import net.impactdev.logger.files.TrackedFile;
import net.impactdev.logger.files.UploadedFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LoggingListener extends ListenerAdapter {

    private final PublicationEndpoint endpoint;

    public LoggingListener() throws Exception {
        this.endpoint = new MclogsEndpoint();
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
            this.download(message.getAttachments()).thenAccept(files -> {
                if (!files.isEmpty()) {
                    List<TrackedFile> tracks = this.endpoint.publish(member.getEffectiveName(), files);
                    MessageTopLevelComponent component = ComponentBuilder.create(tracks, member.getIdLong());
                    event.getChannel()
                            .sendMessageComponents(component)
                            .useComponentsV2()
                            .queue(
                                    success -> DiscordLogger.LOGGER.info(Markers.PUBLISHING, "Detailed response sent successfully"),
                                    fatal -> DiscordLogger.LOGGER.error(Markers.PUBLISHING, "Failed to send message", fatal)
                            );

                }
            }).whenComplete((ignored, failure) -> {
                if (failure != null) {
                    DiscordLogger.LOGGER.error(Markers.PUBLISHING, "Failed to process attachments", failure);
                }
            });
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        DiscordLogger.LOGGER.info("Detected button interaction, processing...");

        String component = event.getComponentId();
        DiscordLogger.LOGGER.info("Component ID: {}", component);
        if (component.contains(":")) {
            String[] parts = component.split(":");
            String action = parts[0];
            String user = parts[1];

            if (!event.getUser().getId().equals(user)) {
                event.reply("This action is only allowed for the original file uploader")
                        .setEphemeral(true)
                        .queue();

                return;
            }

            if (action.equals("delete")) {
                String id = parts[2];
                String token = parts[3];
                if (this.endpoint.delete(id, token)) {
                    event.reply("The uploaded file content has been deleted, future viewers will be unable to use the existing link")
                            .setEphemeral(true)
                            .queue();
                } else {
                    event.reply("Failed to delete the target file...")
                            .setEphemeral(true)
                            .queue();
                }
            }
        }
    }

    private boolean valid(String extension) {
        return extension.equals("txt") ||
                extension.equals("log") ||
                extension.equals("conf") ||
                extension.equals("json") ||
                extension.equals("yml") ||
                extension.equals("info");
    }

    private CompletableFuture<List<UploadedFile>> download(List<Message.Attachment> attachments) {
        List<CompletableFuture<UploadedFile>> downloads = attachments.stream()
                .filter(attachment -> {
                    String extension = attachment.getFileExtension();
                    return extension != null && this.valid(extension);
                })
                .map(attachment -> attachment.getProxy().download()
                        .thenApply(stream -> {
                            DiscordLogger.LOGGER.info(Markers.PUBLISHING, "File downloaded ({}), preparing for publication...", attachment.getFileName());
                            return new UploadedFile(attachment.getFileName(), stream);
                        }))
                .toList();

        if (downloads.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        return CompletableFuture.allOf(downloads.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> downloads.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }
}
