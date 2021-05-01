package me.towdium.jecalculation.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import me.towdium.jecalculation.JustEnoughCalculation;
import me.towdium.jecalculation.polyfill.NBTHelper;
import me.towdium.jecalculation.utils.wrappers.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraftforge.fluids.Fluid;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.BreakIterator;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Author: Towdium
 * Date:   2016/6/25.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@ParametersAreNonnullByDefault
public class Utilities {
    // FLOAT FORMATTING
    public static char[] suffix = new char[]{'K', 'M', 'B', 'G', 'T'};
    public static DecimalFormat[] format = new DecimalFormat[]{
            new DecimalFormat("#."), new DecimalFormat("#.#"), new DecimalFormat("#.##"),
            new DecimalFormat("#.###"), new DecimalFormat("#.####")
    };

    public static String cutNumber(float f, int size) {
        BiFunction<Float, Integer, String> form = (fl, len) -> format[len - 1 - (int) Math.log10(fl)].format(fl);
        int scale = (int) Math.log10(f) / 3;
        if (scale == 0) return form.apply(f, size);
        else return form.apply(f / (float) Math.pow(1000, scale), size - 1) + suffix[scale - 1];
    }


    // MOD NAME
    @Nullable
    public static String getModName(Item item) {
        String name = GameData.getItemRegistry().getNameForObject(item);
        String id = name.substring(0, name.indexOf(":"));
        return id.equals("minecraft") ? "Minecraft" : Loader.instance().getIndexedModList().get(id).getName();
    }

    public static String getModName(Fluid fluid) {
        String name = fluid.getName();
        if (name.equals("lava") || name.equals("water"))
            return "Minecraft";
        else
            return Loader.instance().getIndexedModList().get(fluid.getStillIcon().getIconName().split(":")[0])
                         .getName();
    }

    public static NBTTagCompound getTag(ItemStack is) {
        return NBTHelper.getOrCreateSubCompound(is, JustEnoughCalculation.Reference.MODID);
    }

    public static class Timer {
        long time = System.currentTimeMillis();
        boolean running = false;

        public void setState(boolean b) {
            if (!b && running)
                running = false;
            if (b && !running) {
                running = true;
                time = System.currentTimeMillis();
            }
        }

        public long getTime() {
            return running ? System.currentTimeMillis() - time : 0;
        }

    }

    @SuppressWarnings("UnusedReturnValue")
    public static class Circulator {
        int total, current;

        public Circulator(int total) {
            this(total, 0);
        }

        public Circulator(int total, int current) {
            this.total = total;
            this.current = current;
        }

        public int next() {
            return current + 1 == total ? 0 : current + 1;
        }


        public int prev() {
            return current == 0 ? total - 1 : current - 1;
        }

        public Circulator move(int steps) {
            current += steps;
            if (current < 0)
                current += (-current) / total * total + total;
            else
                current = current % total;
            return this;
        }

        public int current() {
            return current;
        }

        public Circulator set(int index) {
            if (index >= 0 && index < total)
                current = index;
            else
                throw new RuntimeException(String.format("Expected: [0, %d), given: %d.", total, index));
            return this;
        }

        public Circulator copy() {
            return new Circulator(total).set(current);
        }
    }

    public static class ReversedIterator<T> implements Iterator<T> {
        ListIterator<T> i;

        public ReversedIterator(List<T> l) {
            i = l.listIterator(l.size());
        }

        public ReversedIterator(ListIterator<T> i) {
            while (i.hasNext())
                i.next();
            this.i = i;
        }

        @Override
        public boolean hasNext() {
            return i.hasPrevious();
        }

        @Override
        public T next() {
            return i.previous();
        }

        public Stream<T> stream() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }
    }

    @SideOnly(Side.CLIENT)
    public static class I18n {
        public static boolean contains(String s1, String s2) {
            return s1.contains(s2);
        }

        public static Pair<String, Boolean> search(String translateKey, Object... parameters) {
            Pair<String, Boolean> ret = new Pair<>(null, null);
            translateKey = "jecalculation." + translateKey;
            String buffer = net.minecraft.client.resources.I18n.format(translateKey, parameters);
            ret.two = !buffer.equals(translateKey);
            buffer = StringEscapeUtils.unescapeJava(buffer);
            ret.one = buffer.replace("\t", "    ");
            return ret;
        }

        public static String format(String translateKey, Object... parameters) {
            return search(translateKey, parameters).one;
        }

        public static List<String> wrap(String s, int width) {
            return new TextWrapper()
                    .wrap(s, Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode(),
                          i -> TextWrapper.renderer.getCharWidth(i), width);
        }

        static class TextWrapper {
            static FontRenderer renderer = Minecraft.getMinecraft().fontRenderer;

            String str;
            BreakIterator it;
            List<String> temp = new ArrayList<>();
            Function<Character, Integer> func;
            int start, end, section, space, cursor, width;

            private void cut() {
                char c = str.charAt(cursor);
                if (c == '\f')
                    cursor++;
                temp.add(str.substring(start, cursor));
                if (c == ' ' || c == '　' || c == '\n')
                    cursor++;
                start = cursor;
                end = cursor;
                space = width;
                section = func.apply(str.charAt(cursor));
            }

            private void move() {
                temp.add(str.substring(start, end));
                start = end;
                space = width;
            }

            private List<String> wrap(String s, String languageCode, Function<Character, Integer> func, int width) {
                Locale l = getLocaleFromString(languageCode);
                temp.clear();
                start = 0;
                end = 0;
                cursor = 0;
                space = width;
                str = s;
                it = BreakIterator.getLineInstance(l);
                it.setText(s);
                this.width = width;
                this.func = func;
                for (int i = it.next(); i != BreakIterator.DONE; i = it.next()) {
                    for (cursor = end; cursor < i; cursor++) {
                        char ch = str.charAt(cursor);
                        section += func.apply(str.charAt(cursor));
                        if (ch == '\n' || ch == '\f')
                            cut();
                        else if (section > space) {
                            if (start == end)
                                cut();
                            else
                                move();
                        }
                    }
                    space -= section;
                    section = 0;
                    end = cursor;
                }
                move();
                return temp;
            }
        }
    }

    public static class Recent<T> {
        LinkedList<T> data = new LinkedList<>();
        BiPredicate<T, T> tester;
        int limit;

        public Recent(BiPredicate<T, T> tester, int limit) {
            this.tester = tester;
            this.limit = limit;
        }

        public Recent(int limit) {
            this.limit = limit;
        }

        public void push(T obj) {
            data.removeIf(t -> tester != null ? tester.test(t, obj) : t.equals(obj));
            data.push(obj);
            if (data.size() > limit)
                data.pop();
        }

        public List<T> toList() {
            //noinspection unchecked
            return (List<T>) data.clone();
        }

        public void clear() {
            data.clear();
        }
    }

    public static class Json {
        @Nullable
        public static NBTTagCompound read(File f) {
            try {
                String s = FileUtils.readFileToString(f, "UTF-8");
                return read(s);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Nullable
        public static NBTTagCompound read(String s) {
            JsonParser parser = new JsonParser();
            try {
                JsonElement element = parser.parse(s);
                return (NBTTagCompound) NBTJson.toNbt(element);
            } catch (IllegalArgumentException | JsonSyntaxException e) {
                e.printStackTrace();
                JustEnoughCalculation.logger.error("Failed to load json to nbt");
                return null;
            }
        }

        public static void write(NBTTagCompound nbt, File f) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
                fos.write(write(nbt).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public static String write(NBTTagCompound nbt) {
            Writer w = new Writer();
            write(nbt, w);
            w.enter();
            return w.build();
        }

        private static void write(NBTBase nbt, Writer w) {
            if (nbt instanceof NBTTagCompound) {
                NBTTagCompound tags = (NBTTagCompound) nbt;
                //noinspection unchecked
                Set<String> keySet = tags.func_150296_c();
                boolean wrap = keySet.size() > 1;
                boolean first = true;
                w.sb.append('{');
                if (wrap)
                    w.indent++;
                for (String i : keySet) {
                    if (first)
                        first = false;
                    else
                        w.sb.append(',');
                    if (wrap)
                        w.enter();
                    w.sb.append('"');
                    w.sb.append(i);
                    w.sb.append("\": ");
                    write(tags.getTag(i), w);
                }
                if (wrap) {
                    w.indent--;
                    w.enter();
                }
                w.sb.append('}');
            } else if (nbt instanceof NBTTagList) {
                NBTTagList tags = (NBTTagList) nbt;
                boolean wrap = tags.tagCount() > 1;
                boolean first = true;
                w.sb.append('[');
                if (wrap)
                    w.indent++;
                for (NBTBase i : (List<NBTBase>) tags.tagList) {
                    if (first)
                        first = false;
                    else
                        w.sb.append(',');
                    if (wrap)
                        w.enter();
                    write(i, w);
                }
                if (wrap) {
                    w.indent--;
                    w.enter();
                }
                w.sb.append(']');
            } else
                w.sb.append(nbt);
        }

        private static class Writer {
            public int indent = 0;
            StringBuilder sb = new StringBuilder();

            public void enter() {
                sb.append('\n');
                char[] tmp = new char[4 * indent];
                Arrays.fill(tmp, ' ');
                sb.append(tmp);
            }

            public String build() {
                return sb.toString();
            }
        }
    }

    public static Locale getLocaleFromString(String languageCode) {
        String[] parts = languageCode.split("_", -1);
        if (parts.length == 1)
            return new Locale(parts[0]);
        else if (parts.length == 2 || (parts.length == 3 && parts[2].startsWith("#")))
            return new Locale(parts[0], parts[1]);
        else
            return new Locale(parts[0], parts[1], parts[2]);
    }
}
