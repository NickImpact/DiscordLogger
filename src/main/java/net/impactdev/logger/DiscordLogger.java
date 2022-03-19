package net.impactdev.logger;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.impactdev.logger.listening.LoggingListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscordLogger {

    public static final Logger LOGGER = LoggerFactory.getLogger("DiscordLogger");

    private static final String PUBLIC_KEY = "OTUxMDA1MzgyMDQwNDkwMDA0.YihKxw.Om0yJX7fqOXS5LKFO3Dmu_dLz-c";

    public static void main(String[] args) {
        LOGGER.info(Markers.BOOT, "Booting logger service...");
        JDABuilder builder = JDABuilder.createDefault(PUBLIC_KEY);
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.CLIENT_STATUS);
        builder.setActivity(Activity.playing("Gisting Logs"));
        builder.setMemberCachePolicy(MemberCachePolicy.NONE);
        builder.setChunkingFilter(ChunkingFilter.NONE);
        builder.disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING);
        builder.setLargeThreshold(50);

        try {
            JDA jda = builder.addEventListeners(new LoggingListener()).build();
            jda.awaitReady();
        } catch (Exception e) {
            LOGGER.error(Markers.BOOT, "Failed to login to Discord Bot...", e);
        }
    }

}
