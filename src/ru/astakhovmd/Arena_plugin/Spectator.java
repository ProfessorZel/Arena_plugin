package ru.astakhovmd.Arena_plugin;


import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Spectator {
    public Player player;
    public Arena arena;
    GameMode gm;



    Spectator(Player player, Arena arena){
        this.player = player;
        this.arena = arena;
    }

    public void freeze(boolean state) {
        //if (status!=ParticipantStatus.ACTIVE) return;
        if (state){

            player.setAllowFlight(true);
            player.setFlying(true);
            Arena_plugin.instance.frozen.add(player.getUniqueId());
        }else{

            player.setAllowFlight(false);
            player.setFlying(false);
            Arena_plugin.instance.frozen.remove(player.getUniqueId());
        }
    }
    public void spectate(boolean state) {
        if (state){
            gm  = player.getGameMode();
            player.setGameMode(GameMode.SPECTATOR);
        }else{
            player.setGameMode(gm);
        }

        /*//if (status!=ParticipantStatus.ACTIVE) return;
        SpectatorMode spectmode = (SpectatorMode) getServer().getPluginManager().getPlugin("SpectatorMode");
        if (spectmode==null) {
            Arena_plugin.Log("No SpectatorMode plugin!");
            return;
        }

        Spectator sp =  spectmode.getSpectatorCommand();

        if (sp.inState(player.getUniqueId().toString()) == state) return;

        if (state) {
            sp.goIntoSpectatorMode(player);
        } else {
            sp.goIntoSurvivalMode(player, true);
        }*/

    }
    public void tp(Location location) {
        player.teleport(location);
    }

    public void notify(Messages l, String... args) {
        Arena_plugin.sendMessage(player,TextMode.Info,l,args);
        //player.teleport(location);
    }
}