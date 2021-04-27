package ru.astakhovmd.Arena_plugin;

import com.earth2me.essentials.Essentials;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bukkit.Bukkit.getServer;

public class TabCompleter implements org.bukkit.command.TabCompleter {

    Arena_plugin instance;
    TabCompleter(Arena_plugin arena_plugin){
        this.instance = arena_plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String s, String[] args) {
        //boolean is_player = false;
        Player player = null;
        if (sender instanceof Player)
        {
            player = (Player) sender;
        }
        ArrayList<String> fill = null;
        if (cmd.getName().equalsIgnoreCase("arena")){
            fill = new ArrayList<>();
            fill.add("list");
            fill.add("join");
            fill.add("leave");

            fill.add("spectate");

            Moderator mod =  instance.getModerator(player);
            if (mod!=null){
                if (mod.active_arena!=null){

                    //fill.add("pause");
                    fill.add("open");
                    fill.add("start");
                    fill.add("end");
                    fill.add("winner");
                    fill.add("editor");
                    fill.add("kick");
                    fill.add("status");
                    if (mod.active_arena.status == ArenaStatus.DISABLED){
                        fill.add("set");
                        fill.add("rg");
                        fill.add("spawn");
                    }

                }

                fill.add("force");
                fill.add("forcespec");
                fill.add("reload");
                fill.add("use");
            }

            if (args.length>1){
                //Arena_plugin.Log(args[0]);
                if(args[0].equalsIgnoreCase("list")){
                    fill.clear();
                } else if(args[0].equalsIgnoreCase("join")){
                    fill.clear();
                    for (Arena a:
                            instance.arenas) {
                        if (a.status==ArenaStatus.OPEN) fill.add(a.name);
                    }
                    if (args.length>2){
                        fill = null;
                    }
                }else if(args[0].equalsIgnoreCase("use") && mod!=null){
                    fill.clear();
                    for (Arena a:
                            instance.arenas) {
                        fill.add(a.name);
                    }
                    if (args.length>2){
                        fill = null;
                    }
                }else if(args[0].equalsIgnoreCase("winner") && mod!=null){
                    fill.clear();
                    fill.add("announce");
                    if (args.length>2){
                        fill = null;
                    }
                }else if(args[0].equalsIgnoreCase("spectate")){
                    fill.clear();
                    for (Arena a:
                            instance.arenas) {
                        if (a.status==ArenaStatus.OPEN || a.status==ArenaStatus.ACTIVE || a.status ==ArenaStatus.PAUSED)                        fill.add(a.name);
                    }
                    if (args.length>2){
                        fill = null;
                    }
                } else if(args[0].equalsIgnoreCase("kick") && mod!=null){
                    fill.clear();
                    if (mod.active_arena!=null){
                        for (Participant p:
                             mod.active_arena.participants) {
                            fill.add(p.player.getName());
                        }
                    }
                } else if(args[0].equalsIgnoreCase("rg") && mod!=null){
                    fill.clear();
                    if (mod.type!=null){
                        fill.add("point");
                    }
                    if ((mod.loc1!=null) && (mod.loc2!=null)){
                        fill.add("apply");
                    }
                    fill.add("type");
                    fill.add("info");
                    if (args.length>2){
                        if (args[1].equalsIgnoreCase("point")){
                            fill.clear();
                            if (mod.type!=null){
                                switch (mod.type){
                                    case CUBOID:
                                        fill.add("1");
                                        fill.add("2");
                                        break;
                                    case SPHERE:
                                    case CYLINDER:
                                        fill.add("center");
                                        fill.add("radius");
                                        break;
                                }
                            }

                        }else if (args[1].equalsIgnoreCase("type")){
                            fill.clear();
                            for (RegionType type:
                                    RegionType.values()) {
                                fill.add(type.name());
                            }
                        }else{
                            fill = null;
                        }
                    }
                } else if(args[0].equalsIgnoreCase("spawn") && mod!=null){
                    fill.clear();
                    fill.add("add");
                    fill.add("remove");
                    fill.add("list");
                    if (args.length>2){
                            fill = null;
                    }
                }else if(args[0].equalsIgnoreCase("set") && mod!=null){
                    fill.clear();
                    fill.add("lobby");
                    fill.add("spec");
                    fill.add("kit");
                    fill.add("gm");
                    fill.add("mode");
                    fill.add("lives");
                    if (args.length>2){
                        if (args[1].equalsIgnoreCase("gm")){
                            fill.clear();
                            for (GameMode gm:
                                 GameMode.values()) {
                                fill.add(gm.name());
                            }
                        }else if (args[1].equalsIgnoreCase("lives")){
                            fill.clear();
                            for (int i =1; i<5; i++) {
                                fill.add(i+"");
                            }
                        } else if (args[1].equalsIgnoreCase("mode")){
                            fill.clear();
                            for (ArenaMode mode:
                                    ArenaMode.values()) {
                                fill.add(mode.name());
                            }
                        }else if(args[1].equalsIgnoreCase("kit")){
                            fill.clear();
                            Essentials essentials = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
                            if (essentials!=null) {
                                try {
                                    String[] a = essentials.getKits().listKits(essentials,null).split(" ");
                                    fill.addAll(Arrays.asList(a));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }else{
                            fill = null;
                        }
                    }
                } else{
                    fill = null;
                }
            }
        }
        return fill;
    }
}
