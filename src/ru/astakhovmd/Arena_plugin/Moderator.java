package ru.astakhovmd.Arena_plugin;

import org.bukkit.Location;

import java.util.UUID;

public class Moderator {
    public UUID player;
    public Arena active_arena;

    public Location loc1, loc2;
    public RegionType type;

    Moderator(UUID p){
        player = p;
    }
}
