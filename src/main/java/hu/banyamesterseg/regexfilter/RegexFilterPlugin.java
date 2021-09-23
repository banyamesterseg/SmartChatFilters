package hu.banyamesterseg.regexfilter;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RegexFilterPlugin extends JavaPlugin implements CommandExecutor {

    private List<ChatFilter> filters;
    public String prefix;
    public boolean debug;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new AsyncPlayerChatListener(this), this);
        this.getCommand("regexfilter").setExecutor(this);
        saveDefaultConfig();
        reload();
    }

    public void reload() {
        filters = new ArrayList<>();
        reloadConfig();
        Configuration config = getConfig();
        prefix = CCUtils.addColor(config.getString("prefix", "&c[ChatFilters] "));
        debug = config.getBoolean("debug", false);
        for (Map<?, ?> filterMap: config.getMapList("filters")) {
            ChatFilter filter;
            try {
                filter = new ChatFilter(this, filterMap);
            } catch (NullPointerException e) {
              getLogger().warning("Filter found without pattern, ignoring");
              continue;
            } catch (Exception e) {
              e.printStackTrace();
              continue;
            }
            filters.add(filter);
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (args[0].equals("reload")) {
        if (sender.hasPermission("regexfilter.reload")) {
          sender.sendMessage("reinitializing filterset");
          getLogger().info("reinitializing filterset");
          this.reload();
          return true;
        } else {
          sender.sendMessage("no you don't");
          return true;
        }
      } else {
        getLogger().info("wtf");
        return true;
      }
    }

    public Iterable<ChatFilter> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isDebugOn() {
        return debug;
    }
}