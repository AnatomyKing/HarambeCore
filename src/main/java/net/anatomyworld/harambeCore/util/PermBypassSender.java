package net.anatomyworld.harambeCore.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public final class PermBypassSender implements CommandSender {

    private final Player player;

    public PermBypassSender(Player player) {
        this.player = player;
    }

    /* === Permission Override === */
    @Override public boolean isOp() { return true; }
    @Override public void setOp(boolean value) { /* ignore */ }
    @Override public boolean isPermissionSet(@NotNull String name) { return false; }
    @Override public boolean isPermissionSet(@NotNull Permission perm) { return false; }
    @Override public boolean hasPermission(@NotNull String name) { return true; }
    @Override public boolean hasPermission(@NotNull Permission perm) { return true; }

    /* === Permission Attachments (return dummy or empty) === */
    @Override public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
        return null;
    }

    @Override public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        return null;
    }

    @Override public void removeAttachment(@NotNull PermissionAttachment attachment) {}

    @Override public void recalculatePermissions() {}

    @Override public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return Set.of();
    }

    /* === Messaging === */
    @Override public void sendMessage(@NotNull String message) {
        player.sendMessage(message);
    }

    @Override public void sendMessage(@NotNull String[] messages) {
        for (String msg : messages) {
            player.sendMessage(msg);
        }
    }

    @Override public void sendMessage(@Nullable UUID sender, @NotNull String message) {
        player.sendMessage(message);
    }

    @Override public void sendMessage(@Nullable UUID sender, @NotNull String... messages) {
        for (String msg : messages) {
            player.sendMessage(msg);
        }
    }

    /* === CommandSender Identity === */
    @Override public @NotNull Server getServer() {
        return player.getServer();
    }

    @Override public @NotNull String getName() {
        return player.getName();
    }

    public @NotNull UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override public @NotNull Component name() {
        return Component.text(player.getName());
    }

    @Override public @NotNull Spigot spigot() {
        return player.spigot(); // safest fallback
    }
}
