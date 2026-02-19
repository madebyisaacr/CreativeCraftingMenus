package dev.chililisoup.creativecraftingmenus.util;

import com.mojang.datafixers.util.Pair;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.ParserBuilder;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.*;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.network.chat.contents.objects.ObjectInfo;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class FullTextParser {
    private static final NodeParser PARSER;

    public static Component formatText(String text) {
        Component formatted = PARSER.parseText(text, ParserContext.of());
        return formatted.copy().setStyle(formatted.getStyle().withItalic(
                formatted.getStyle().isItalic() ? null : false
        ));
    }

    public static String decompose(Component component) {
        Style style = component.getStyle();
        return decomposeInternal(component.copy().setStyle(style.withItalic(
                (style.italic == null || style.isItalic()) ? true : null
        ))).replaceFirst("(?<!\\\\)(</>)+$", "");
    }

    private static String decomposeInternal(Component component) {
        StringBuilder builder = new StringBuilder();

        Pair<String, String> styleTags = buildStyleTags(component.getStyle());
        builder.append(styleTags.getFirst());
        builder.append(decomposeContents(component.getContents()));
        component.getSiblings().forEach(sibling -> builder.append(decomposeInternal(sibling)));
        builder.append(styleTags.getSecond());

        return builder.toString();
    }

    private static String decomposeContents(ComponentContents contents) {
        if (contents instanceof PlainTextContents plainTextContents)
            return String.join("\\", plainTextContents.text().split("(?=[\\\\<*_~])"));
            // Placeholder API's legacy text formatter is inaccurate, so it's not this easy
            // return plainTextContents.text().replace("§", "&");

        if (contents instanceof TranslatableContents translatableContents) {
            StringBuilder builder = new StringBuilder();
            translatableContents.visit(text -> {
                builder.append(text);
                return Optional.empty();
            });
            return builder.toString();
        }

        if (contents instanceof ObjectContents(ObjectInfo objectInfo)) {
            if (objectInfo instanceof PlayerSprite(ResolvableProfile player, boolean hat)) {
                var profile = player.unpack();

                String value;
                boolean isId = true;
                if (profile.left().isPresent()) value = profile.left().get().id().toString();
                else if (profile.right().isPresent()) {
                    var partial = profile.right().get();
                    if (partial.name().isPresent()) {
                        value = partial.name().get();
                        isId = false;
                    } else if (partial.id().isPresent()) value = partial.id().get().toString();
                    else value = "";
                } else value = "";

                return hat ?
                        "<player '" + value + "'>" :
                        "<player " + (isId ? "uuid" : "name") + ":'" + value + "' hat:false>";
            }

            if (objectInfo instanceof AtlasSprite(Identifier atlas, Identifier sprite))
                return String.format(
                        "<atlas '%s' '%s'>",
                        atlas /*? if >= 1.21.11 {*/ .toShortString() /*?}*/,
                        sprite /*? if >= 1.21.11 {*/ .toShortString() /*?}*/
                );
        }

        return "";
    }

    private static Pair<String, String> buildStyleTags(Style style) {
        final StringBuilder open = new StringBuilder();
        final StringBuilder close = new StringBuilder();

        class Collector {
            void addTag(String tag) {
                open.append("<").append(tag).append(">");
                close.append("</>");
            }

            void addFlagString(String key, @Nullable Boolean value) {
                if (value != null) {
                    String tag = switch (key) {
                        case "bold" -> "b";
                        case "italic" -> "i";
                        case "underlined" -> "underline";
                        case "strikethrough" -> "st";
                        case "obfuscated" -> "obf";
                        default -> key;
                    };

                    this.addTag(tag + (value ? "" : ":false"));
                }
            }

            void addValueString(String key, @Nullable Object value) {
                if (value != null) this.addTag(key + ":" + value);
            }

            void addColorString(@Nullable TextColor color) {
                if (color != null) {
                    String value = color.toString();
                    this.addTag(value.startsWith("#") ? "c:" + value : value);
                }
            }
        }

        Collector lv = new Collector();
        lv.addColorString(style.getColor());
        lv.addValueString("shadowColor", style.getShadowColor());
        lv.addFlagString("bold", style.bold);
        lv.addFlagString("italic", style.italic);
        lv.addFlagString("underlined", style.underlined);
        lv.addFlagString("strikethrough", style.strikethrough);
        lv.addFlagString("obfuscated", style.obfuscated);
//        lv.addValueString("clickEvent", style.getClickEvent());
//        lv.addValueString("hoverEvent", style.getHoverEvent());
//        lv.addValueString("insertion", style.getInsertion());
        lv.addValueString("font", style.font);

        return Pair.of(open.toString(), close.toString());
    }

    static {
        PARSER = ParserBuilder.of()
                .quickText()
                .simplifiedTextFormat()
                .markdown()
                .legacyAll()
                .build();
    }
}
