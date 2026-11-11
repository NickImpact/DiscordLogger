package net.impactdev.discord.test;

import com.google.common.collect.Lists;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.impactdev.logger.components.ComponentBuilder;
import net.impactdev.logger.endpoints.MclogsEndpoint;
import net.impactdev.logger.endpoints.PublicationEndpoint;
import net.impactdev.logger.files.TrackedFile;
import net.impactdev.logger.files.UploadedFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

public final class DiscordTest {

    @Test
    @DisplayName("Discord - Try Sending the Component Design")
    public void tryComponentSend() throws Exception {
        String webhookUrl = System.getenv("DISCORD_WEBHOOK_URL");
        String botToken = System.getenv("discord-bot-api-key");
        Assumptions.assumeTrue(
                webhookUrl != null && !webhookUrl.isBlank() && botToken != null && !botToken.isBlank(),
                "Set DISCORD_WEBHOOK_URL and the bot-api-key system property to run this integration test"
        );

        try (InputStream stream = this.stream(Path.of("egg-redis-7.json"))) {
            Assertions.assertNotNull(stream);

            final PublicationEndpoint endpoint = new MclogsEndpoint();
            final UploadedFile file = new UploadedFile("egg-redis-7.json", stream);

            List<TrackedFile> tracked = endpoint.publish("System Test", Lists.newArrayList(file));
            Assertions.assertFalse(tracked.isEmpty());

            var component = ComponentBuilder.create(tracked, 180085138162450432L);
            var message = new MessageCreateBuilder()
                    .addComponents(component)
                    .useComponentsV2(true)
                    .build();

            JDA jda = JDABuilder.createLight(botToken).build();
            try {
                jda.awaitReady();

                TextChannel channel = jda.getTextChannelById("625206927152119818");
                Assertions.assertNotNull(channel);

                channel.sendMessage(message).submit().join();
            } finally {
                jda.shutdownNow();
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("URI Validation")
    public void validateUri() throws Exception {
        URI target = new URI("https://api.mclo.gs/1/log/").resolve("HsM2e78");
        Assertions.assertEquals("https://api.mclo.gs/1/log/HsM2e78", target.toString());
    }

    private InputStream stream(Path target) {
        return this.getClass().getClassLoader().getResourceAsStream(target.toString().replace("\\", "/"));
    }
}
