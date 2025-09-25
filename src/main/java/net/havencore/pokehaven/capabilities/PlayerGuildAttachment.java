package net.havencore.pokehaven.capabilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Immutable payload stored in the player data attachment backing {@link PlayerGuildData}.
 */
public record PlayerGuildAttachment(Optional<String> guildName,
                                    Optional<String> factionName,
                                    boolean factionLeader) {
    public static final MapCodec<PlayerGuildAttachment> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("guild_name").forGetter(PlayerGuildAttachment::guildName),
            Codec.STRING.optionalFieldOf("faction_name").forGetter(PlayerGuildAttachment::factionName),
            Codec.BOOL.fieldOf("is_faction_leader").orElse(false).forGetter(PlayerGuildAttachment::factionLeader)
    ).apply(instance, PlayerGuildAttachment::new));

    public static PlayerGuildAttachment empty() {
        return new PlayerGuildAttachment(Optional.empty(), Optional.empty(), false);
    }

    public PlayerGuildAttachment withGuildName(@Nullable String guildName) {
        if (guildName == null) {
            return empty();
        }
        return new PlayerGuildAttachment(Optional.of(guildName), Optional.empty(), false);
    }

    public PlayerGuildAttachment withFactionName(@Nullable String factionName) {
        if (factionName == null) {
            return new PlayerGuildAttachment(guildName, Optional.empty(), false);
        }
        return new PlayerGuildAttachment(guildName, Optional.of(factionName), factionLeader);
    }

    public PlayerGuildAttachment withFactionLeader(boolean factionLeader) {
        return new PlayerGuildAttachment(guildName, factionName, factionLeader && factionName.isPresent());
    }
}
