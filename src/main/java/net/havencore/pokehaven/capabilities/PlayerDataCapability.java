package net.havencore.pokehaven.capabilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Capability implementation backed by {@link PlayerDataAttachment} stored on the player via NeoForge attachments.
 */
public final class PlayerDataCapability implements PlayerGuildData {
    private final Player player;

    public PlayerDataCapability(Player player) {
        this.player = player;
    }

    private PlayerDataAttachment attachment() {
        return player.getData(PlayerDataAttachments.PLAYER_DATA.get());
    }

    private void update(Function<PlayerDataAttachment, PlayerDataAttachment> mutator) {
        PlayerDataAttachment current = attachment();
        PlayerDataAttachment updated = mutator.apply(current);
        if (!Objects.equals(current, updated)) {
            player.setData(PlayerDataAttachments.PLAYER_DATA.get(), updated);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.syncData(PlayerDataAttachments.PLAYER_DATA.get());
            }
        }
    }

    @Override
    public <T> Optional<T> get(PlayerDataKey<T> key) {
        return attachment().get(key);
    }

    @Override
    public <T> void set(PlayerDataKey<T> key, @Nullable T value) {
        update(attachment -> attachment.with(key, value));
    }

    @Override
    public void clearSegment(ResourceLocation segmentId) {
        update(attachment -> attachment.clear(segmentId));
    }
}
