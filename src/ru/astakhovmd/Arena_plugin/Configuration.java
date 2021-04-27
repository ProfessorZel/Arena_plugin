package ru.astakhovmd.Arena_plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Configuration {

    boolean debug;
    double lobby_distance;
    int min_spawns;
    String api_key;

    Configuration(){
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(Arena_plugin.configFilePath));
        min_spawns = config.getInt("Event.MinSpawns",2);
        api_key = config.getString("API.Key", "");
        lobby_distance = config.getDouble("Event.LobbySize", 0);
        debug = config.getBoolean("Plugin.Debug", false);
    }

    public void save(){
        FileConfiguration outConfig = new YamlConfiguration();
        outConfig.options().header("Default values are perfect for most servers.");
        outConfig.set("Event.MinSpawns",min_spawns);
        outConfig.set("API.Key",api_key);
        outConfig.set("Event.LobbySize",lobby_distance);
        outConfig.set("Plugin.Debug", debug);

        try
        {
            outConfig.save(Arena_plugin.configFilePath);
        }
        catch (IOException exception)
        {
            Arena_plugin.Log("Unable to write to the configuration file at \"" + Arena_plugin.configFilePath + "\"");
        }
    }
}
