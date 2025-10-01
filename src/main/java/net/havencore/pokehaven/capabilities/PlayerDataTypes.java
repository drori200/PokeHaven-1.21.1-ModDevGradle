package net.havencore.pokehaven.capabilities;

import com.mojang.serialization.Codec;
import net.havencore.pokehaven.PokeHaven;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of codecs that can be referenced by {@link PlayerDataKey} instances.
 */
public final class PlayerDataTypes {
    private static final Map<ResourceLocation, PlayerDataType<?>> TYPES = new ConcurrentHashMap<>();

    public static final PlayerDataType<String> STRING = register("string", Codec.STRING);
    public static final PlayerDataType<Boolean> BOOLEAN = register("boolean", Codec.BOOL);
    public static final PlayerDataType<Integer> INT = register("int", Codec.INT);
    public static final PlayerDataType<Long> LONG = register("long", Codec.LONG);
    public static final PlayerDataType<Float> FLOAT = register("float", Codec.FLOAT);
    public static final PlayerDataType<Double> DOUBLE = register("double", Codec.DOUBLE);

    private PlayerDataTypes() {
    }

    public static <T> PlayerDataType<T> register(String path, Codec<T> codec) {
        return register(ResourceLocation.fromNamespaceAndPath(PokeHaven.MODID, path), codec);
    }

    public static <T> PlayerDataType<T> register(ResourceLocation id, Codec<T> codec) {
        PlayerDataType<T> type = new PlayerDataType<>(id, codec);
        PlayerDataType<?> existing = TYPES.putIfAbsent(id, type);
        if (existing != null) {
            throw new IllegalStateException("A player data type with id " + id + " is already registered.");
        }
        return type;
    }

    public static Optional<PlayerDataType<?>> find(ResourceLocation id) {
        return Optional.ofNullable(TYPES.get(id));
    }
}
