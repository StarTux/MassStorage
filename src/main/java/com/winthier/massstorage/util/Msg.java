package com.winthier.massstorage.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.simple.JSONValue;

public final class Msg {
    private Msg() { }

    public static String format(String msg, Object... args) {
        if (msg == null) return "";
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (args.length > 0) {
            msg = String.format(msg, args);
        }
        return msg;
    }

    public static void send(CommandSender to, String msg, Object... args) {
        to.sendMessage(format(msg, args));
    }

    public static void info(Player player, String msg, Object... args) {
        raw(player,
            "",
            pluginTag(),
            " ",
            format(msg, args));
    }

    public static void warn(Player player, String msg, Object... args) {
        player.sendMessage(format("&r[&cMS&r] &c") + format(msg, args));
    }

    static void consoleCommand(String cmd, Object... args) {
        if (args.length > 0) cmd = String.format(cmd, args);
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd);
    }

    public static void raw(Player player, Object... obj) {
        if (obj.length == 0) return;
        if (obj.length == 1) {
            consoleCommand("minecraft:tellraw %s %s", player.getName(), JSONValue.toJSONString(obj[0]));
        } else {
            consoleCommand("minecraft:tellraw %s %s", player.getName(), JSONValue.toJSONString(Arrays.asList(obj)));
        }
    }

    public static Object pluginTag() {
        return button(ChatColor.BLUE,
                      "&r[&9MS&r]",
                      "&a/ms ?\n&oMass Storage Help",
                      "/ms ?");
    }

    public static Object button(ChatColor color, String chat, String tooltip, String command) {
        Map<String, Object> map = new HashMap<>();
        map.put("text", format(chat));
        map.put("color", color.name().toLowerCase());
        if (command != null) {
            Map<String, Object> clickEvent = new HashMap<>();
            map.put("clickEvent", clickEvent);
            clickEvent.put("action", command.endsWith(" ") ? "suggest_command" : "run_command");
            clickEvent.put("value", command);
        }
        if (tooltip != null) {
            Map<String, Object> hoverEvent = new HashMap<>();
            map.put("hoverEvent", hoverEvent);
            hoverEvent.put("action", "show_text");
            hoverEvent.put("value", format(tooltip));
        }
        return map;
    }

    public static Object button(String chat, String tooltip, String command) {
        return button(ChatColor.WHITE, chat, tooltip, command);
    }

    public static String camelCase(String msg) {
        StringBuilder sb = new StringBuilder();
        for (String tok: msg.split("_")) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(tok.substring(0, 1).toUpperCase());
            sb.append(tok.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public static String jsonToString(Object json) {
        if (json == null) {
            return "";
        } else if (json instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object o: (List) json) {
                sb.append(jsonToString(o));
            }
            return sb.toString();
        } else if (json instanceof Map) {
            Map map = (Map) json;
            StringBuilder sb = new StringBuilder();
            sb.append(map.get("text"));
            sb.append(map.get("extra"));
            return sb.toString();
        } else if (json instanceof String) {
            return (String) json;
        } else {
            return json.toString();
        }
    }
}
