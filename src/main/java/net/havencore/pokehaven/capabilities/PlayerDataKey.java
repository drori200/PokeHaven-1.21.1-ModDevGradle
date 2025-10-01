package net.havencore.pokehaven.capabilities;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Identifies a single value stored within a {@link PlayerDataAttachment}. A key is defined by the segment it belongs to,
 * the unique path of the value within that segment, and the {@link PlayerDataType} used for (de-)serialization.
 *
 * <p>To expose new data through the player capability:</p>
 * <ol>
 *     <li>Create a {@code static} {@link PlayerDataKey} constant describing the segment, value path, and data type.</li>
 *     <li>Add getter/setter default methods to your capability view that delegate to the generic
 *     {@code get} and {@code set} helpers exposed by {@link IPlayerData}.</li>
 *     <li>Use the getter/setter methods from gameplay or networking code to read and update the value.</li>
 * </ol>
 *
 * @param segment the logical grouping this key belongs to, typically one segment per gameplay system.
 * @param path    the unique value identifier inside the segment.
 * @param type    the codec definition controlling how the value is stored.
 * @param <T>     the Java type associated with this key.
 */
public record PlayerDataKey<T>(@NotNull ResourceLocation segment,
                               @NotNull String path,
                               @NotNull PlayerDataType<T> type) {
    public static PlayerDataKey<String> string(ResourceLocation segment, String path) {
        return new PlayerDataKey<>(segment, path, PlayerDataTypes.STRING);
    }

    public static PlayerDataKey<Boolean> bool(ResourceLocation segment, String path) {
        return new PlayerDataKey<>(segment, path, PlayerDataTypes.BOOLEAN);
    }

    public static PlayerDataKey<Integer> intKey(ResourceLocation segment, String path) {
        return new PlayerDataKey<>(segment, path, PlayerDataTypes.INT);
    }

    public static PlayerDataKey<Long> longKey(ResourceLocation segment, String path) {
        return new PlayerDataKey<>(segment, path, PlayerDataTypes.LONG);
    }

    public static PlayerDataKey<Float> floatKey(ResourceLocation segment, String path) {
        return new PlayerDataKey<>(segment, path, PlayerDataTypes.FLOAT);
    }

    public static PlayerDataKey<Double> doubleKey(ResourceLocation segment, String path) {
        return new PlayerDataKey<>(segment, path, PlayerDataTypes.DOUBLE);
    }
}
