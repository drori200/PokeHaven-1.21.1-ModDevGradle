package net.havencore.pokehaven.capabilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Capability implementation that proxies calls to the {@link PlayerGuildAttachment} stored on a player.
 */
public final class PlayerGuildCapability implements PlayerGuildData {
    private final Player player;

    public PlayerGuildCapability(Player player) {
        this.player = player;
    }

    private PlayerGuildAttachment attachment() {
        return player.getData(PlayerGuildDataAttachments.PLAYER_GUILD.get());
    }

    private void update(Function<PlayerGuildAttachment, PlayerGuildAttachment> mutator) {
        PlayerGuildAttachment current = attachment();
        PlayerGuildAttachment updated = mutator.apply(current);
        if (!Objects.equals(current, updated)) {
            player.setData(PlayerGuildDataAttachments.PLAYER_GUILD.get(), updated);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.syncData(PlayerGuildDataAttachments.PLAYER_GUILD.get());
            }
        }
    }

    @Override
    public Optional<String> getGuildName() {
        return attachment().guildName();
    }

    @Override
    public void setGuildName(@Nullable String guildName) {
        update(attachment -> attachment.withGuildName(guildName));
    }

    @Override
    public Optional<String> getFactionName() {
        return attachment().factionName();
    }

    @Override
    public void setFactionName(@Nullable String factionName) {
        update(attachment -> attachment.withFactionName(factionName));
    }

    @Override
    public boolean isFactionLeader() {
        return attachment().factionLeader();
    }

    @Override
    public void setFactionLeader(boolean factionLeader) {
        update(attachment -> attachment.withFactionLeader(factionLeader));
    }

    @Override
    public void clearMembership() {
        update(attachment -> PlayerGuildAttachment.empty());
    }
}
