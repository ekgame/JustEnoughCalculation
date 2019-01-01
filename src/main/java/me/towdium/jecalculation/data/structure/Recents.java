package me.towdium.jecalculation.data.structure;

import mcp.MethodsReturnNonnullByDefault;
import me.towdium.jecalculation.data.label.ILabel;
import me.towdium.jecalculation.utils.Utilities;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Author: towdium
 * Date:   18-8-28.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Recents {
    Utilities.Recent<ILabel> record = new Utilities.Recent<>((a, b) ->
            a == ILabel.EMPTY || a.equals(b), 9);

    public Recents(NBTTagList nbt) {
        List<ILabel> ls = StreamSupport.stream(nbt.spliterator(), false)
                .filter(n -> n instanceof NBTTagCompound)
                .map(n -> ILabel.SERIALIZER.deserialize((NBTTagCompound) n))
                .collect(Collectors.toList());
        new Utilities.ReversedIterator<>(ls).forEachRemaining(l -> record.push(l, false));
    }

    public Recents() {
    }

    public void push(ILabel label, boolean replace) {
        record.push(label, replace);
    }

    public ILabel getLatest() {
        return record.toList().get(0);
    }

    public List<ILabel> getRecords() {
        return record.toList();
    }

    public NBTTagList serialize() {
        NBTTagList ret = new NBTTagList();
        record.toList().forEach(l -> ret.appendTag(ILabel.SERIALIZER.serialize(l)));
        return ret;
    }
}
