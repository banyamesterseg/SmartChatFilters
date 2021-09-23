package hu.banyamesterseg.regexfilter;

import me.clip.placeholderapi.PlaceholderAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFilter {

  private final RegexFilterPlugin plugin;

  Pattern pattern;
  boolean deny;
  String warn;
  String replacement;
  String command;
  String alertGroup;
  String alertMessage;
  String exemptGroup;

  public ChatFilter(RegexFilterPlugin plugin, Map<?, ?> map) {
    this.plugin = plugin;
    //match
    this.pattern = Pattern.compile((String) map.get("pattern"));
    Bukkit.getLogger().info("Filter added: /"+this.pattern.toString()+"/");
    //texts
    this.alertMessage = CCUtils.addColor((String) map.getOrDefault("alert-text", null));
    this.warn = CCUtils.addColor((String) map.getOrDefault("warn-text", null));
    this.command = (String) map.getOrDefault("exec", null);
    this.replacement = (String) map.getOrDefault("replace", null);
    //behavior
    this.deny = map.containsKey("deny") && (boolean) map.get("deny");
    this.alertGroup = (String) "regexfilter.notify."+map.getOrDefault("alert-group", null);
    this.exemptGroup = (String) "regexfilter.exempt."+map.getOrDefault("exempt-group", null);
  }

  public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
    Player sender = event.getPlayer();
    String originalMessage = event.getMessage();
    Matcher matcher = pattern.matcher(originalMessage);
    //MATCH
    if (matcher.find()) {
      if (plugin.isDebugOn()) {
        Bukkit.getLogger().info("MATCH: /"+pattern.pattern()+"/");
      }
      //EXEMPT
      if (exemptGroup != null && permissionPredicate(exemptGroup).test(sender)) {
        if (plugin.isDebugOn()) {
          Bukkit.getLogger().info("  EXEMPTED by "+exemptGroup+", stopped processing rule");
        }
        return;
      }
      //NOTIFY
      if (alertGroup != null) {
        String alert;
        if (alertMessage != null) {
          alert = PlaceholderAPI.setPlaceholders(sender, alertMessage)
                                .replace("{MESSAGE}", originalMessage)
                                .replace("{PATTERN}", pattern.pattern())
                                .replace("{MATCH}", matcher.group());
        } else {
          alert = CCUtils.addColor("&cA message has been filtered");
        }
        if (plugin.isDebugOn()) {
          Bukkit.getLogger().info("  NOTIF "+alertGroup+" with \""+alert+"\"");
        }
        final String falert = alert;
        try {
          Bukkit.getOnlinePlayers().stream().filter(permissionPredicate(alertGroup)).forEach(player -> player.sendMessage(plugin.getPrefix()+falert));
        } catch(NullPointerException e) {
          Bukkit.getLogger().info("Notification recipient group "+alertGroup+" is empty, no messages sent");
        }
      }
      //WARN
      if (warn != null) {
        String replacedWarn = PlaceholderAPI.setPlaceholders(sender, warn)
                                            .replace("{MESSAGE}", originalMessage)
                                            .replace("{PATTERN}", pattern.pattern())
                                            .replace("{MATCH}", matcher.group());
        if (plugin.isDebugOn()) {
          Bukkit.getLogger().info("  WARN "+sender.getName()+" with \""+replacedWarn+"§r\"");
        }
        sender.sendMessage(plugin.getPrefix() + ChatColor.RED + ChatColor.translateAlternateColorCodes('&', replacedWarn));
      }
      //EXEC
      if (command != null) {
        String replacedCommand = PlaceholderAPI.setPlaceholders(sender, this.command)
                                 .replace("{MESSAGE}", originalMessage)
                                 .replace("{PATTERN}", pattern.pattern())
                                 .replace("{MATCH}", matcher.group());
        if (plugin.isDebugOn()) {
          Bukkit.getLogger().info("  EXEC \""+replacedCommand+"§r\"");
        }
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacedCommand));
      }
      //DENY
      if (deny) {
        if (plugin.isDebugOn()) {
          Bukkit.getLogger().info("  DENY \""+originalMessage+"§r\"");
        }
        event.setCancelled(true);
      //REPLACE
      } else if (replacement != null) {
        if (plugin.isDebugOn()) {
          Bukkit.getLogger().info("  REPLACING");
        }
        String replacedReplacement = PlaceholderAPI.setPlaceholders(sender, replacement)
                                                   .replace("{MESSAGE}", originalMessage)
                                                   .replace("{PATTERN}", pattern.pattern())
                                                   .replace("{MATCH}", matcher.group());
        event.setMessage(matcher.replaceAll(replacedReplacement));
      }
    }
  }

  public static Predicate<Player> permissionPredicate(String permission) {
    return player -> player.hasPermission(permission);
  }
}