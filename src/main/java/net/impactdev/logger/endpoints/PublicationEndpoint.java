package net.impactdev.logger.endpoints;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public interface PublicationEndpoint {

    MessageEmbed publish(String username, JsonObject json, EmbedBuilder builder);

}
