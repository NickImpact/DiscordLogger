package net.impactdev.logger.components;

import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.utils.FileUpload;
import net.impactdev.logger.files.TrackedFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ComponentBuilder {

    public static MessageTopLevelComponent create(List<TrackedFile> files, long userId) {
        List<ContainerChildComponent> components = new ArrayList<>();

//        var banner = MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(
//                Objects.requireNonNull(ComponentBuilder.class.getClassLoader().getResourceAsStream("banner.png")),
//                "banner.png"
//        )));
//        components.add(banner);

        var text = TextDisplay.of("## File Reupload\nAs the discord file viewer constrains the max viewable content, and to aid mobile users, your files have been reuploaded to help ease users who do not wish to download the content for a full view.");
        components.add(text);
        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        boolean first = false;
        for (final TrackedFile file : files) {
            final var component = file.toComponent(userId);

            if (first) {
                components.add(Separator.createDivider(Separator.Spacing.SMALL));
            }

            components.addAll(component);
            first = true;
        }

        return Container.of(components);
    }
}
