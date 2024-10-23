package com.cavetale.ms.dialogue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.TextColor;
import static net.kyori.adventure.text.format.TextColor.color;

@Getter
@RequiredArgsConstructor
public enum Denomination {
    NONE(1, null),
    SILVER(100, 0xD8D8D8),
    GOLD(10_000, 0xFDF55F),
    RUBY(1_000_000, 0xDA4358),
    OPAL(100_000_000, 0xa8c3bc),
    ;

    private final int value;
    private final TextColor color;

    Denomination(final int value, final int colorHex) {
        this.value = value;
        this.color = color(colorHex);
    }

    public static Denomination ofAmount(int amount) {
        final Denomination[] array = values();
        for (int i = array.length - 1; i >= 0; i -= 1) {
            final Denomination it = array[i];
            if (amount >= it.value) return it;
        }
        return NONE;
    }
}
