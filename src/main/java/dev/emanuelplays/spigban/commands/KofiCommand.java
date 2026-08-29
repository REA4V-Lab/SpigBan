package dev.emanuelplays.spigban.commands;

import dev.emanuelplays.spigban.SpigBan;
import dev.emanuelplays.spigban.commands.base.BaseCommand;
import dev.emanuelplays.spigban.utils.MessageUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * /kofi — Sends a fancy, clickable Ko-fi link for support.
 */
public class KofiCommand extends BaseCommand {

    public KofiCommand(SpigBan plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "spigban.kofi")) return true;

        // Build the message with clickable link using BungeeCord Chat
        TextComponent koFiBracketLeft = new TextComponent("[");
        koFiBracketLeft.setColor(net.md_5.bungee.api.ChatColor.BLUE);
        koFiBracketLeft.setBold(true);

        TextComponent koFiText = new TextComponent("Ko-fi");
        koFiText.setColor(net.md_5.bungee.api.ChatColor.AQUA);
        koFiText.setBold(true);

        TextComponent koFiBracketRight = new TextComponent("]");
        koFiBracketRight.setColor(net.md_5.bungee.api.ChatColor.BLUE);
        koFiBracketRight.setBold(true);

        TextComponent spaceAndTriangle = new TextComponent(" ► ");
        spaceAndTriangle.setColor(net.md_5.bungee.api.ChatColor.WHITE);

        TextComponent url = new TextComponent("https://ko-fi/emanuelplays");
        url.setColor(net.md_5.bungee.api.ChatColor.BLUE);
        url.setUnderlined(true);
        url.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://ko-fi/emanuelplays"));
        url.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[]{new TextComponent("Click to visit my Ko-fi!")}));

        TextComponent spaceAfterUrl = new TextComponent(" ");
        TextComponent triangleLeft = new TextComponent("◄");
        triangleLeft.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        TextComponent spaceAfterTriangle = new TextComponent(" ");
        TextComponent clickText = new TextComponent("(Click to support!)");
        clickText.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        clickText.setItalic(true);

        // Combine all components
        BaseComponent[] components = new BaseComponent[]{
                koFiBracketLeft,
                koFiText,
                koFiBracketRight,
                spaceAndTriangle,
                url,
                spaceAfterUrl,
                triangleLeft,
                spaceAfterTriangle,
                clickText
        };

        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(components);
        } else {
            // For console, we can't send clickable components, so we send a plain text message.
            sender.sendMessage("[Ko-fi] ► https://ko-fi/emanuelplays ◄ (Click to support!)");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}