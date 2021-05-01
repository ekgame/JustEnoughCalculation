package me.towdium.jecalculation.gui.guis.pickers;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import me.towdium.jecalculation.data.label.labels.LPlaceholder;
import me.towdium.jecalculation.gui.JecaGui;
import me.towdium.jecalculation.gui.Resource;
import me.towdium.jecalculation.gui.guis.IGui;
import me.towdium.jecalculation.gui.widgets.*;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Author: towdium
 * Date:   17-9-29.
 */
@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class PickerPlaceholder extends IPicker.Impl implements IGui {
    public PickerPlaceholder() {
        WLabelScroll scroll = new WLabelScroll(7, 69, 8, 5, WLabel.enumMode.PICKER, true)
                .setLabels(LPlaceholder.getRecent()).setLsnrUpdate(this::notifyLsnr);
        WTextField create = new WTextField(26, 7, 69);
        create.setLsnrText(s -> create.setColor(s.equals("") ? JecaGui.COLOR_TEXT_RED : JecaGui.COLOR_TEXT_WHITE));
        add(new WSearch(26, 45, 90, scroll));
        add(new WIcon(7, 45, 20, 20, Resource.ICN_TEXT, "picker_placeholder.text_search"));
        add(new WIcon(7, 7, 20, 20, Resource.ICN_TEXT, "picker_placeholder.text_create"));
        add(new WLine(36));
        add(new WButtonIcon(95, 7, 20, 20, Resource.BTN_YES).setLsnrLeft(() -> {
            if (!create.getText().equals("")) callback.accept(new LPlaceholder(create.getText(), 1));
        }));
        addAll(scroll, create);
    }
}
