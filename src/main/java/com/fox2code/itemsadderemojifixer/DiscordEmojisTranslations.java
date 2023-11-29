package com.fox2code.itemsadderemojifixer;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.vdurmont.emoji.EmojiParser;

import java.util.function.Function;

class DiscordEmojisTranslations {
    DiscordEmojisTranslations() {}

    static final HashBiMap<String, String> emojiTranslations = HashBiMap.create();
    static final BiMap<String, String> emojiTranslationsInverted = emojiTranslations.inverse();
    static final Function<String, String> unmapToDiscord = s -> {
        ItemsAdderEmojiFixer itemsAdderEmojiFixer =
                ItemsAdderEmojiFixer.getInstance();
        s = itemsAdderEmojiFixer.unmapEmojisEx(s, true,
                id -> emojiTranslations.getOrDefault(id, id));
        if (s.indexOf(':') != -1) {
            //noinspection UnnecessaryUnicodeEscape <- Not unecessary
            s = EmojiParser.parseToUnicode(s).replace("\u2764", ":heart:")
                    .replace(":yawning_face:", "\uD83E\uDD71");
        }
        final String txt = itemsAdderEmojiFixer.getEmojiExtCodePointStr();
        while (s.endsWith(txt)) s = s.substring(0, s.length() - txt.length());
        return s;
    };
    static final Function<String, String> unmapToMinecraft = s -> {
        //noinspection UnnecessaryUnicodeEscape <- Not unecessary
        s = EmojiParser.parseToAliases(s, EmojiParser.FitzpatrickAction.REMOVE)
                .replace("\uFE0E", "").replace("\uFE0F", "")
                .replace("\uD83E\uDD71", ":yawning_face:");
        ItemsAdderEmojiFixer.getInstance().getLogger().info(s);
        StringBuilder stringBuilder = new StringBuilder();
        int prevEnd = -1;
        while (true) {
            int start = s.indexOf(':', prevEnd + 1);
            if (start == -1) break;
            int end = s.indexOf(':', start + 1);
            if (end == -1) break;
            stringBuilder.append(s, prevEnd == -1 ? 0 : prevEnd, start + 1);
            String id = s.substring(start + 1, end);
            if ("injured".equals(id)) id = "head_bandage";
            String newId = emojiTranslationsInverted.get(id);
            stringBuilder.append(newId == null || !ItemsAdderEmojiFixer
                    .getInstance().hasEmoji(newId) ? id : newId);
            prevEnd = end;
        }
        if (prevEnd == -1) return s;
        stringBuilder.append(s, prevEnd, s.length());
        return stringBuilder.toString();
    };

    static {
        // Doing this by hand is pain...
        // At least it's the same for
        // every Discord implementation.
        emojiTranslations.put("0", "zero");
        emojiTranslations.put("1", "one");
        emojiTranslations.put("2", "two");
        emojiTranslations.put("3", "three");
        emojiTranslations.put("4", "four");
        emojiTranslations.put("5", "five");
        emojiTranslations.put("6", "six");
        emojiTranslations.put("7", "seven");
        emojiTranslations.put("8", "height");
        emojiTranslations.put("9", "nine");
        emojiTranslations.put("smile", "grinning");
        emojiTranslations.put("smile2", "grin");
        emojiTranslations.put("smile3", "smiley");
        emojiTranslations.put("smile4", "smile");
        emojiTranslations.put("smile5", "relaxed");
        emojiTranslations.put("smile6", "slight_smile");
        emojiTranslations.put("smilecat", "smile_cat");
        emojiTranslations.put("zip", "zipper_mouth");
        emojiTranslations.put("sneeze", "sneezing_face");
        emojiTranslations.put("neutral", "neutral_face");
        emojiTranslations.put("woozy", "woozy_face");
        emojiTranslations.put("mad", "zany_face");
        emojiTranslations.put("cry1", "sob");
        emojiTranslations.put("laugh", "joy");
        emojiTranslations.put("laugh2", "sweat_smile");
        emojiTranslations.put("laugh3", "rolling_on_the_floor_laughing");
        emojiTranslations.put("tongue", "stuck_out_tongue_closed_eyes");
        emojiTranslations.put("what", "face_with_raised_eyebrow");
        emojiTranslations.put("noexpression", "expressionless");
        emojiTranslations.put("crycat", "crying_cat_face");
        emojiTranslations.put("hot", "hot_face");
        emojiTranslations.put("hehe", "smirk");
        emojiTranslations.put("bye", "wave");
        emojiTranslations.put("callme", "call_me_hand");
        emojiTranslations.put("blush", "flushed");
        emojiTranslations.put("xd", "laughing");
        emojiTranslations.put("ok1", "thumbsup");
        emojiTranslations.put("no1", "thumbsdown");
        emojiTranslations.put("twofingers", "v");
        emojiTranslations.put("heart1", "broken_heart");
        // emojiTranslations.put("heart2", "???");
        emojiTranslations.put("heart3", "purple_heart");
        emojiTranslations.put("heart4", "green_heart");
        emojiTranslations.put("heart5", "yellow_heart");
        emojiTranslations.put("heart6", "sparkling_heart");
        emojiTranslations.put("angry", "rage");
        emojiTranslations.put("angry1", "triumph");
        emojiTranslations.put("angry2", "angry");
        emojiTranslations.put("angry3", "face_with_symbols_over_mouth");
        emojiTranslations.put("upsidedown", "upside_down");
        emojiTranslations.put("dead", "dizzy_face");
        emojiTranslations.put("dead1", "skull_crossbones");
        emojiTranslations.put("inlovecat", "heart_eyes_cat");
        emojiTranslations.put("inlove", "smiling_face_with_hearts");
        emojiTranslations.put("inlove1", "heart_eyes");
        emojiTranslations.put("pig", "pig2");
        emojiTranslations.put("unicorn", "unicorn_face");
        emojiTranslations.put("write", "writing");
        emojiTranslations.put("sun", "sunny");
        emojiTranslations.put("moon", "new_moon");
        emojiTranslations.put("party", "tada");
        emojiTranslations.put("money", "moneybag");
        emojiTranslations.put("moneyface", "money_mouth");
        emojiTranslations.put("soccer", "soccerball");
        emojiTranslations.put("cancel", "x");
        emojiTranslations.put("voltage", "zap");
        emojiTranslations.put("luck", "shamrock");
        emojiTranslations.put("ice", "snowflake");
        emojiTranslations.put("rock", "horn_sign");
        emojiTranslations.put("hashtag", "hash");
        emojiTranslations.put("redcircle", "o");
        emojiTranslations.put("bandage", "injured");
        emojiTranslations.put("stopsign", "no_entry");
        emojiTranslations.put("yinyang", "yin_yang");
        emojiTranslations.put("tree", "christmas_tree");
        emojiTranslations.put("blocked", "no_entry_sign");
        emojiTranslations.put("alarmclock", "alarm_clock");
        emojiTranslations.put("drumstick", "poultry_leg");
        emojiTranslations.put("pan", "shallow_pan_of_food");
        emojiTranslations.put("riceball", "rice_ball");
        emojiTranslations.put("kebab", "stuffed_flatbread");
        emojiTranslations.put("jackolantern", "jack_o_lantern");
        emojiTranslations.put("confirm", "white_check_mark");
    }
}
