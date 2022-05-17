package com.cavetale.ms.session;

import com.cavetale.mytems.util.BlockColor;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public enum FavoriteSlot {
    SLOT01(37, BlockColor.BLACK),
    SLOT02(38, BlockColor.RED),
    SLOT03(39, BlockColor.GREEN),
    SLOT04(40, BlockColor.BROWN),
    SLOT05(41, BlockColor.BLUE),
    SLOT06(42, BlockColor.PURPLE),
    SLOT07(43, BlockColor.CYAN),
    SLOT08(45, BlockColor.LIGHT_GRAY),
    SLOT09(46, BlockColor.GRAY),
    SLOT10(47, BlockColor.PINK),
    SLOT11(48, BlockColor.LIME),
    SLOT12(49, BlockColor.YELLOW),
    SLOT13(50, BlockColor.LIGHT_BLUE),
    SLOT14(51, BlockColor.MAGENTA),
    SLOT15(52, BlockColor.ORANGE),
    SLOT16(53, BlockColor.WHITE);

    public final int guiSlot;
    public final BlockColor blockColor;

    public ItemStack createIcon() {
        return new ItemStack(blockColor.getMaterial(BlockColor.Suffix.SHULKER_BOX));
    }

    public ItemStack createDisabledIcon() {
        return new ItemStack(blockColor.getMaterial(BlockColor.Suffix.STAINED_GLASS));
    }

    public Component getDisplayName() {
        return text(blockColor.niceName + " Favorites", blockColor.textColor);
    }
}
