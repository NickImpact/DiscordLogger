package net.impactdev.logger.files;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;

public record TrackedFile(String name, String identifier, int lines, long size, Instant expiration, URI uri, String token) {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd uuuu", Locale.US)
            .withZone(ZoneId.systemDefault());

    public static TrackedFile parse(String name, JsonObject json) {
        return new TrackedFile(
                name,
                json.get("id").getAsString(),
                json.get("lines").getAsInt(),
                json.get("size").getAsLong(),
                Instant.ofEpochSecond(json.get("expires").getAsLong()),
                URI.create(json.get("url").getAsString()),
                json.get("token").getAsString()
        );
    }

    public List<ContainerChildComponent> toComponent(long userId) {
        var name = TextDisplay.ofFormat("## File Name: %s", this.name);
        var lines = TextDisplay.ofFormat("Lines: %d", this.lines);
        var size = TextDisplay.ofFormat("Size: %s", formatBytes(this.size));
        var expires = TextDisplay.ofFormat("Expires: %s", formatter.format(expiration));

        var open = Button.link(this.uri.toString(), "Open")
                .withEmoji(Emoji.fromUnicode("\uD83D\uDD17"));

        var id = String.format("delete:%s:%s:%s", Long.toUnsignedString(userId), this.identifier, token);
        var delete = Button.danger(id, "Delete")
                .withEmoji(Emoji.fromUnicode("\uD83D\uDDD1️"));

        return Lists.newArrayList(name, lines, size, expires, ActionRow.of(open, delete));
    }

    private static String formatBytes(long bytes) {
        if (Math.abs(bytes) < 1024) {
            return bytes + " B";
        }

        String[] suffixes = new String[]{"KiB", "MiB", "GiB"};
        double count = bytes;
        int index = -1;

        while (Math.abs(count) >= 1024 && index < suffixes.length - 1) {
            count /= 1024;
            index++;
        }

        return String.format("%.1f %s", count, suffixes[index]);
    }

}
