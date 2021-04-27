package ru.astakhovmd.Arena_plugin;


import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;



public class Arena_plugin extends JavaPlugin {

    //.this
    public static Arena_plugin instance;
    private static Logger log;

    public final static String API_URL = "https://arena.astakhovmd.ru/api.php";

    //array of arenas
    ArrayList<Arena> arenas = new ArrayList<>();
    HashMap<UUID,Moderator> mods = new HashMap<>();

    //messages loaded
    private String[] messages;

    //config
    public Configuration config;

    //player frozen
    HashSet<UUID> frozen = new HashSet<>();

    HashMap<UUID, Region> controlled = new HashMap<>();


    protected final static String dataLayerFolderPath = "plugins" + File.separator + "ArenaProZel";

    final static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";
    final static String arenaDataFolderPath = dataLayerFolderPath + File.separator + "Arenas";
    final static String eventDataFolderPath = dataLayerFolderPath + File.separator + "EventLogs";

    final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";
    final static String messagesFilePath = dataLayerFolderPath + File.separator + "messages.yml";

    @Override
    public void onEnable()
    {
        instance = this;
        log = instance.getLogger();

        //Log("StartUp!");


        config = new Configuration();
        config.save();

        this.loadMessages();
        this.loadArenas();

        PluginCommand command = Bukkit.getPluginCommand("arena");
        if (command!=null){
            command.setTabCompleter(new TabCompleter(this));
        }else{
            Error("Tab completer unable to hook to the command!");
        }
        Bukkit.getServer().getPluginManager().registerEvents(new EventListener(this), this);

        //TODO: periodic task
        //BukkitTask s = Bukkit.getScheduler().runTaskTimer(this, new PeriodicTask(),1000, 1000);
    }

    @Override
    public void onLoad() {

    }

    @Override
    public void onDisable() {
        for (Arena a:
                arenas) {
            a.endEvent();

           try {
                a.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void reloadConfig() {
        for (Arena a:
                arenas) {
            a.endEvent();
            //a.setRegionType();
        }

        mods.clear();

        arenas.clear();

        controlled.clear();
        frozen.clear();


        config = new Configuration();
        loadMessages();
        loadArenas();
    }


    public Arena getArena(String name){
        if (name==null) return null;
        for (Arena a:
             arenas) {
            if (name.equalsIgnoreCase(a.name)){
                return a;
            }
        }
        return null;
    }
    /*public ArrayList<Moderator> getModerators(Arena arena){
        ArrayList<Moderator> moderators = new ArrayList<>();
        if (arena==null) return moderators;

        moderators = mods.stream().filter(item->item.active_arena == arena).collect(Collectors.toCollection(ArrayList::new));
        return moderators;
    }*/
    public Moderator getModerator(Player player){
        if (player==null) {
            Moderator m = mods.get(null);
            if (m != null) {
                return m;
            } else {
                Moderator mod = new Moderator(null);
                mods.put(null, mod);
                return mod;
            }
        }else{
            if (!player.hasPermission("arena.mod")) {
                return null;
            }
            Moderator m = mods.get(player.getUniqueId());
            if (m != null) {
                return m;
            } else {
                Moderator mod = new Moderator(player.getUniqueId());
                mods.put(player.getUniqueId(), mod);
                return mod;
            }
        }
    }
    public Participant getParticipant(Player player){
        if (player==null) return null;
        for (Arena a:arenas) {
            //if (a.status == ArenaStatus.DISABLED) continue;
            Participant par = a.getParticipant(player.getUniqueId());
            if (par !=null){
                return par;
            }
        }
        return null;
    }
    public Spectator getSpectator(Player player){
        if (player==null) return null;
        for (Arena a:arenas) {
            Spectator par = a.getSpectator(player.getUniqueId());
            if (par !=null){
                return par;
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player player = null;

        if (sender instanceof Player)
        {
            player = (Player) sender;
        }

        if (cmd.getName().equalsIgnoreCase("arena")){

            if (args.length<1){
                sendMessage(player, TextMode.Info, Messages.UnknownCommand);
                return true;
            }

            if (args[0].equalsIgnoreCase("set"))
            {
                if (args.length < 2) {
                    sendMessage(player, TextMode.Info, Messages.UnknownCommand);
                    return true;
                }
                 if (args[1].equalsIgnoreCase("lobby"))
                {
                    if (player==null) {
                        sendMessage(null, TextMode.Info, Messages.OnlyPlayerOperation);
                        return true;
                    }

                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }

                    Location loc = player.getLocation();
                    sendMessage(player,TextMode.Instr, mod.active_arena.setLobbyLoc(loc));
                    return true;
                }else if (args[1].equalsIgnoreCase("spec"))
                {
                    if (player==null) {
                        sendMessage(null, TextMode.Info, Messages.OnlyPlayerOperation);
                        return true;
                    }

                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }

                    Location loc = player.getLocation();
                    sendMessage(player,TextMode.Instr, mod.active_arena.setSpectatorLoc(loc));
                    return true;
                }else if (args[1].equalsIgnoreCase("kit"))
                {
                    if (args.length<3){
                        sendMessage(player,TextMode.Err, Messages.SetKitUsage);
                        return true;
                    }
                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }
                    mod.active_arena.setKit_name(args[2]);
                    sendMessage(player,TextMode.Instr, Messages.ArenaKitSet, args[2], mod.active_arena.name);
                    return true;
                }else if (args[1].equalsIgnoreCase("lives"))
                 {
                     if (args.length<3){
                         sendMessage(player,TextMode.Err, Messages.SetLivesUsage);
                         return true;
                     }
                     Moderator mod = getModerator(player);
                     if (mod==null) {
                         sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                         return true;
                     }
                     if (mod.active_arena==null) {
                         sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                         return true;
                     }
                     try {
                         mod.active_arena.setLives(Integer.parseInt(args[2]));
                         sendMessage(player,TextMode.Instr, Messages.ArenaLivesSet, args[2], mod.active_arena.name);
                     }catch (NumberFormatException e){
                         sendMessage(player,TextMode.Err, Messages.ArenaLivesWrongNumber);
                     }

                     return true;
                 }else if (args[1].equalsIgnoreCase("gm"))
                {
                    if (args.length<3){
                        sendMessage(player,TextMode.Err, Messages.SetGMUsage);
                        return true;
                    }
                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }
                    mod.active_arena.setGM(GameMode.valueOf(args[2]));
                    sendMessage(player,TextMode.Instr, Messages.ArenaGMSet, mod.active_arena.gm.name(), mod.active_arena.name);
                    return true;
                }

            }else if (args[0].equalsIgnoreCase("rg"))
            {
                if (args.length < 2) {
                    sendMessage(null, TextMode.Info, Messages.UnknownCommand);
                    return true;
                }

                if (args[1].equalsIgnoreCase("point")){
                    if (player==null) {
                        sendMessage(null, TextMode.Info, Messages.OnlyPlayerOperation);
                        return true;
                    }
                    if (args.length < 3) {
                        sendMessage(player,TextMode.Instr, Messages.RGPointUsage);
                        return true;
                    }
                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }

                    Location loc = player.getLocation();
                    if (args[2].equalsIgnoreCase("1") || args[2].equalsIgnoreCase("center")){
                        sendMessage(player,TextMode.Info, Messages.FirstPointSet);
                        mod.loc1 = loc;
                    }else if (args[2].equalsIgnoreCase("2") || args[2].equalsIgnoreCase("radius")){
                        sendMessage(player,TextMode.Info, Messages.SecondPointSet);
                        mod.loc2 = loc;
                    }else{
                        sendMessage(player,TextMode.Instr, Messages.RGPointUsage);
                    }
                    return true;
                }else if (args[1].equalsIgnoreCase("apply"))
                {
                    if (player==null) {
                        sendMessage(null, TextMode.Info, Messages.OnlyPlayerOperation);
                        return true;
                    }

                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }

                    try {
                        mod.active_arena.setRegion(new Region(mod.type, mod.loc1, mod.loc2));
                        sendMessage(player,TextMode.Instr, Messages.RegionSet);
                    } catch (NotSameWorld notSameWorld) {
                        //notSameWorld.printStackTrace();
                        sendMessage(player,TextMode.Instr, Messages.NotASameWorld);
                        return true;
                    } catch (NullPointerException e){
                        sendMessage(player,TextMode.Instr, Messages.RGPointUsage);
                    } catch (NoRegionTypeSet noRegionTypeSet) {
                        sendMessage(player,TextMode.Instr, Messages.NoRegionTypeSet);
                    }
                    return true;
                }else if (args[1].equalsIgnoreCase("type"))
                {
                    if (player==null) {
                        sendMessage(null, TextMode.Info, Messages.OnlyPlayerOperation);
                        return true;
                    }

                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }

                    if (args.length>=3){
                        try {
                            mod.type = RegionType.valueOf(args[2]);
                            sendMessage(player, TextMode.Instr, Messages.RegionTypeSet, mod.active_arena.region.type.name(), mod.active_arena.name);
                        }catch (IllegalArgumentException e){
                            sendMessage(player,TextMode.Warn, Messages.InvalidRegionType);
                        }
                    }
                    return true;
                }else if (args[1].equalsIgnoreCase("info")){
                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }
                    Region r = mod.active_arena.region;
                    if (r !=null){
                        sendMessage(player,TextMode.Info, Messages.RegionInfo, r.type.name(), r.A.toString(), r.B.toString());
                    }else{
                        sendMessage(player,TextMode.Warn, Messages.RegionUndefined);
                    }
                    if ((mod.type !=null) || (mod.loc1 !=null) || (mod.loc2 !=null)){
                        sendMessage(player,TextMode.Instr, Messages.NewRegionInfo, (mod.type!=null)?mod.type.name():"Undefined", (mod.loc1!=null)?mod.loc1.toString():"Undefined", (mod.loc2!=null)?mod.loc2 .toString():"Undefined");
                    }


                }

            }else if (args[0].equalsIgnoreCase("spawn"))
            {
                if (args.length < 2) {
                    sendMessage(null, TextMode.Info, Messages.UnknownCommand);
                    return true;
                }

                if (args[1].equalsIgnoreCase("add")){
                    if (player==null) {
                        sendMessage(null, TextMode.Info, Messages.OnlyPlayerOperation);
                        return true;
                    }

                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }
                    Location loc = player.getLocation();
                    sendMessage(player,TextMode.Info, mod.active_arena.addSpawn(loc));

                    return true;
                }else if (args[1].equalsIgnoreCase("remove"))
                {
                    if (player==null) {
                        sendMessage(null, TextMode.Info, Messages.OnlyPlayerOperation);
                        return true;
                    }

                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }
                    Location loc = player.getLocation();
                    sendMessage(player,TextMode.Info, mod.active_arena.removeSpawn(loc));
                    return true;
                }else if (args[1].equalsIgnoreCase("list"))
                {
                    Moderator mod = getModerator(player);
                    if (mod==null) {
                        sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                        return true;
                    }
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }
                    final DecimalFormat df = new DecimalFormat("#0.0");
                    for (Location a:
                            mod.active_arena.spawns) {
                        sendMessage(player,TextMode.Success,Messages.SpawnsList,df.format(a.getX()), df.format(a.getY()), df.format(a.getZ()), a.getWorld().getName());
                    }

                    return true;
                }else{
                    sendMessage(player,TextMode.Info,Messages.UnknownCommand);
                }

            }else
            if (args[0].equalsIgnoreCase("join"))
            {
                if (player==null) {
                    sendMessage(null, TextMode.Info, Messages.OnlyPlayerOperation);
                    return true;
                }

                Participant p = getParticipant(player);
                if (p!=null) {
                    sendMessage(player,TextMode.Info,Messages.AlreadyJoined, p.arena.name);
                    return true;
                }

                Spectator s = getSpectator(player);
                if (s!=null) {
                    sendMessage(player,TextMode.Info,Messages.AlreadyJoinedSpec, s.arena.name);
                    return true;
                }

                if (args.length<2){
                    Moderator mod = getModerator(player);
                    if (mod!=null) {
                        if (mod.active_arena!=null) {
                            sendMessage(player,TextMode.Info,mod.active_arena.join(player), mod.active_arena.name);
                            return true;
                        }
                    }
                    sendMessage(player,TextMode.Instr,Messages.JoinUsage);
                }else{
                    Arena arena = getArena(args[1]);
                    if (arena==null){
                        sendMessage(player,TextMode.Err,Messages.ArenaNotFound);
                    }else{
                        if (arena.lobby!=null){


                        if (player.getLocation().distance(arena.lobby)>config.lobby_distance && config.lobby_distance > 0){
                            sendMessage(player,TextMode.Success,Messages.JoinFromLobbyOnly);
                            return true;
                        }
                        }
                        sendMessage(player,TextMode.Success,arena.join(player), arena.name);
                    }

                }
                return true;
            }else
            if (args[0].equalsIgnoreCase("save"))
            {

                return true;
            }else
            if (args[0].equalsIgnoreCase("spectate"))
            {
                if (player==null) {
                    sendMessage(null, TextMode.Info, Messages.OnlyPlayerOperation);
                    return true;
                }

                Participant p = getParticipant(player);
                if (p!=null) {
                    sendMessage(player,TextMode.Info,Messages.AlreadyJoined, p.arena.name);
                    return true;
                }
                Spectator s = getSpectator(player);
                if (s!=null) {
                    sendMessage(player,TextMode.Info,Messages.AlreadyJoinedSpec, s.arena.name);
                    return true;
                }

                if (args.length<2){
                    Moderator mod = getModerator(player);
                    if (mod!=null) {
                        if (mod.active_arena!=null) {
                            sendMessage(player,TextMode.Info,mod.active_arena.joinSpectator(player), mod.active_arena.name);
                            return true;
                        }
                    }
                    sendMessage(player,TextMode.Instr,Messages.SpecUsage);
                }else{
                    Arena arena = getArena(args[1]);
                    if (arena==null){
                        sendMessage(player,TextMode.Err,Messages.ArenaNotFound);
                    }else{
                        sendMessage(player,TextMode.Success,arena.joinSpectator(player), arena.name);
                    }
                }
                return true;
            }else
            if (args[0].equalsIgnoreCase("use"))
            {

                if (args.length<2){
                    sendMessage(player,TextMode.Err, Messages.UseUsage);
                    return true;
                }
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }
                Arena arena = getArena(args[1]);
                if (arena==null){
                    sendMessage(player,TextMode.Err,Messages.ArenaNotFound);
                    return true;
                }
                mod.active_arena = arena;
                sendMessage(player,TextMode.Success,Messages.ActiveArenaSet, arena.name);

                return true;
            }else
            if (args[0].equalsIgnoreCase("list"))
            {
                Moderator mod = getModerator(player);
                if (mod!=null){
                    for (Arena a: arenas) {
                        sendMessage(player,TextMode.Success,Messages.ArenasListModFormat,a.name,a.kit_name,a.gm.name(),a.activeParticipants()+"", a.maxParticipants()+"", a.status.name());
                    }
                }else{
                    for (Arena a: arenas) {
                        sendMessage(player,TextMode.Success,Messages.ArenasListPlayerFormat,a.name,a.kit_name,a.gm.name(),a.activeParticipants()+"", a.maxParticipants()+"", a.status.name());
                    }
                }

                return true;
            } else
            if (args[0].equalsIgnoreCase("open"))
            {
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }
                if (mod.active_arena==null) {
                    sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                    return true;
                }
                sendMessage(player,TextMode.Instr, mod.active_arena.openEvent(), mod.active_arena.name);

                return true;
            }else
            if (args[0].equalsIgnoreCase("pause"))
            {
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }
                if (mod.active_arena==null) {
                    sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                    return true;
                }

                sendMessage(player,TextMode.Instr, mod.active_arena.pauseEvent(), mod.active_arena.name);
                return true;
            }else
            if (args[0].equalsIgnoreCase("end"))
            {
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }
                if (mod.active_arena==null) {
                    sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                    return true;
                }

                sendMessage(player,TextMode.Instr,  mod.active_arena.endEvent(), mod.active_arena.name);
                return true;
            } else
            if (args[0].equalsIgnoreCase("start"))
            {
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }
                if (mod.active_arena==null) {
                    sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                    return true;
                }
                sendMessage(player,TextMode.Instr, mod.active_arena.startEvent());
                return true;
            }else if (args[0].equalsIgnoreCase("kick"))
            {

                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }

                Player plyr = this.getServer().getPlayerExact(args[1]);
                if (plyr == null){
                    sendMessage(player,TextMode.Err,Messages.PlayerNotFound);
                    return true;
                }
                Participant p = getParticipant(plyr);
                Spectator s = getSpectator(plyr);
                if (p!=null) {
                    if (p.arena==null){
                        //sendMessage(plyr, TextMode.Info, Messages.SYS_Null, args);
                        sendMessage(player, TextMode.Info, Messages.SYS_Null, args);
                        Error("kick - participant arena is NULL!");
                        return true;
                    }

                    p.statSet(Stats.COMBAT_LOGOFF, true);
                    p.arena.leave(p);
                }else if (s!=null) {
                    if (s.arena==null){
                        //sendMessage(plyr, TextMode.Info, Messages.SYS_Null, args);
                        sendMessage(player, TextMode.Info, Messages.SYS_Null, args);
                        Error("kick - spectator arena is NULL!");
                        return true;
                    }
                    s.arena.leave(s);
                } else {
                    sendMessage(player, TextMode.Info, Messages.NoJoinedArena);
                }
            }else
            if (args[0].equalsIgnoreCase("winner"))
            {
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }

                if (args.length>3) {
                    Arena arena = getArena(args[2]);
                    if (arena==null){
                        sendMessage(player,TextMode.Err,Messages.ArenaNotFound);
                        return true;
                    }

                    UUID winner = arena.getWinner();
                    if (winner == null){
                        sendMessage(player,TextMode.Success,Messages.NoWinner);
                        return true;
                    }

                    Player player1 = Bukkit.getPlayer(winner);
                    if (player1!=null){
                        sendMessage(player,TextMode.Success,Messages.Winner, arena.name, player1.getDisplayName());
                    }else{
                        OfflinePlayer player2 = Bukkit.getOfflinePlayer(winner);
                        sendMessage(player,TextMode.Success,Messages.Winner, arena.name, player2.getName());
                    }
                }else{
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }

                    UUID winner = mod.active_arena.getWinner();
                    if (winner == null){
                        sendMessage(player,TextMode.Success,Messages.NoWinner);
                        return true;
                    }

                    Player player1 = Bukkit.getPlayer(winner);
                    if (player1!=null){
                        sendMessage(player,TextMode.Success,Messages.Winner, mod.active_arena.name, player1.getDisplayName());
                    }else{
                        OfflinePlayer player2 = Bukkit.getOfflinePlayer(winner);
                        sendMessage(player,TextMode.Success,Messages.Winner, mod.active_arena.name, player2.getName());
                    }
                }
                return true;
            }else
            if (args[0].equalsIgnoreCase("editor"))
            {
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }
                try {
                    JSONObject resp = pushToAPI(API_R.EDITOR,"get_url",null);
                    if (resp.containsKey("editor_url")){
                        Arena_plugin.Log((String) resp.get("editor_url"));
                        sendMessage(player, TextMode.Info,Messages.Editor, (String) resp.get("editor_url"));
                    }else{
                        Arena_plugin.Error(resp.toJSONString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            }else
            if (args[0].equalsIgnoreCase("bcwinner"))
            {
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }

                if (args.length>3) {
                    Arena arena = getArena(args[2]);
                    if (arena==null){
                        sendMessage(player,TextMode.Err,Messages.ArenaNotFound);
                        return true;
                    }

                    UUID winner = arena.getWinner();
                    if (winner == null){
                        sendMessage(player,TextMode.Success,Messages.NoWinner);
                        return true;
                    }

                    Player player1 = Bukkit.getPlayer(winner);
                    if (player1!=null){
                        Bukkit.getServer().broadcastMessage(getMessage(Messages.Winner, arena.name, player1.getDisplayName()));
                        //sendMessage(player,TextMode.Success,);
                    }else{
                        OfflinePlayer player2 = Bukkit.getOfflinePlayer(winner);
                        Bukkit.getServer().broadcastMessage(getMessage(Messages.Winner, arena.name, player2.getName()));
                        //sendMessage(player,TextMode.Success,Messages.Winner, arena.name, player2.getName());
                    }
                }else{
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }

                    UUID winner = mod.active_arena.getWinner();
                    if (winner == null){
                        sendMessage(player,TextMode.Success,Messages.NoWinner);
                        return true;
                    }

                    Player player1 = Bukkit.getPlayer(winner);
                    if (player1!=null){
                        sendMessage(player,TextMode.Success,Messages.Winner, mod.active_arena.name, player1.getDisplayName());
                    }else{
                        OfflinePlayer player2 = Bukkit.getOfflinePlayer(winner);
                        sendMessage(player,TextMode.Success,Messages.Winner, mod.active_arena.name, player2.getName());
                    }
                }
                return true;
            }else
            if (args[0].equalsIgnoreCase("status"))
            {
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }
                if (mod.active_arena==null) {
                    sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                    return true;
                }
                Arena a = mod.active_arena;
                sendMessage(player,TextMode.Success,Messages.ArenasListModFormat,a.name,a.kit_name,a.gm.name(),a.activeParticipants()+"", a.maxParticipants()+"", a.status.name());
                return true;
            }else
            if (args[0].equalsIgnoreCase("force"))
            {
                if (args.length<2){
                    sendMessage(player,TextMode.Err, Messages.ForceJoinUsage);
                    return true;
                }
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }

                Player plyr = this.getServer().getPlayerExact(args[1]);
                if (plyr == null){
                    sendMessage(player,TextMode.Err,Messages.PlayerNotFound);
                    return true;
                }
                Participant p = getParticipant(plyr);
                if (p!=null) {
                    sendMessage(player,TextMode.Info,Messages.AlreadyJoined, p.arena.name);
                    sendMessage(plyr,TextMode.Info,Messages.AlreadyJoined, p.arena.name);
                    return true;
                }
                Spectator s = getSpectator(player);
                if (s!=null) {
                    sendMessage(player,TextMode.Info,Messages.AlreadyJoinedSpec, s.arena.name);
                    sendMessage(plyr,TextMode.Info,Messages.AlreadyJoinedSpec, s.arena.name);
                    return true;
                }

                if (args.length>3) {
                    Arena arena = getArena(args[2]);
                    if (arena==null){
                        sendMessage(player,TextMode.Err,Messages.ArenaNotFound);
                        return true;
                    }
                    Messages msg = arena.join(plyr);
                    sendMessage(plyr,TextMode.Success,msg,arena.name);
                    sendMessage(player,TextMode.Success,msg, arena.name);

                }else{
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }
                    Messages msg = mod.active_arena.join(plyr);
                    sendMessage(plyr,TextMode.Success,msg, mod.active_arena.name);
                    sendMessage(player,TextMode.Success,msg, mod.active_arena.name);
                }
                //sendMessage(player,TextMode.Success,mod.active_arena.join(plyr), mod.active_arena.name);

                return true;
            }else
            if (args[0].equalsIgnoreCase("forcespec"))
            {
                if (args.length<2){
                    sendMessage(player,TextMode.Err, Messages.ForceJoinUsage);
                    return true;
                }
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }

                Player plyr = this.getServer().getPlayerExact(args[1]);
                if (plyr == null){
                    sendMessage(player,TextMode.Err,Messages.PlayerNotFound);
                    return true;
                }
                Participant p = getParticipant(plyr);
                if (p!=null) {
                    sendMessage(player,TextMode.Info,Messages.AlreadyJoined, p.arena.name);
                    sendMessage(plyr,TextMode.Info,Messages.AlreadyJoined, p.arena.name);
                    return true;
                }
                Spectator s = getSpectator(player);
                if (s!=null) {
                    sendMessage(player,TextMode.Info,Messages.AlreadyJoinedSpec, s.arena.name);
                    sendMessage(plyr,TextMode.Info,Messages.AlreadyJoinedSpec, s.arena.name);
                    return true;
                }

                if (args.length>3) {
                    Arena arena = getArena(args[2]);
                    if (arena==null){
                        sendMessage(player,TextMode.Err,Messages.ArenaNotFound);
                        return true;
                    }
                    Messages msg = arena.joinSpectator(plyr);
                    sendMessage(plyr,TextMode.Success,msg,arena.name);
                    sendMessage(player,TextMode.Success,msg, arena.name);

                }else{
                    if (mod.active_arena==null) {
                        sendMessage(player,TextMode.Instr, Messages.NoActiveArena);
                        return true;
                    }
                    Messages msg = mod.active_arena.joinSpectator(plyr);
                    sendMessage(plyr,TextMode.Success,msg, mod.active_arena.name);
                    sendMessage(player,TextMode.Success,msg, mod.active_arena.name);
                }
                //sendMessage(player,TextMode.Success,mod.active_arena.join(plyr), mod.active_arena.name);

                return true;
            } else
            if (args[0].equalsIgnoreCase("leave"))
            {
                if (player==null) {
                    sendMessage(null, TextMode.Info, Messages.OnlyPlayerOperation);
                    return true;
                }
                Participant p = getParticipant(player);
                Spectator s = getSpectator(player);
                if (p!=null) {
                    if (p.arena==null){
                        sendMessage(player, TextMode.Info, Messages.SYS_Null, args);
                        Error("leave - participant arena is NULL!");
                        return true;
                    }
                    ArenaStatus status = p.arena.status;
                    if (status == ArenaStatus.ACTIVE || status == ArenaStatus.PAUSED){
                        p.statSet(Stats.COMBAT_LOGOFF, true);
                    }
                    p.arena.leave(p);
                }else if (s!=null) {
                    if (s.arena==null){
                        sendMessage(player, TextMode.Info, Messages.SYS_Null, args);
                        Error("leave - spectator arena is NULL!");
                        return true;
                    }
                    s.arena.leave(s);
                } else {
                    sendMessage(player, TextMode.Info, Messages.NoJoinedArena);
                }
                return true;
            }else
            if (args[0].equalsIgnoreCase("reload"))
            {
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }

                reloadConfig();
                sendMessage(player,TextMode.Success,Messages.Reloaded);
                return true;
            }else
            if (args[0].equalsIgnoreCase("create"))
            {
                Moderator mod = getModerator(player);
                if (mod==null) {
                    sendMessage(player,TextMode.Err, Messages.NeedToBeMod);
                    return true;
                }
                if (args.length<2){
                    sendMessage(player,TextMode.Err, Messages.CreateUsage);
                    return true;
                }
                try {
                    Arena arena = new Arena(args[1]);
                    arenas.add(arena);
                    mod.active_arena = arena;
                    arena.apply();
                    sendMessage(player,TextMode.Err, Messages.ArenaCreated);
                    sendMessage(player,TextMode.Err, Messages.ActiveArenaSet,arena.name);
                } catch (NameTaken nameTaken) {
                    sendMessage(player,TextMode.Err, Messages.NameTaken);
                    //nameTaken.printStackTrace();
                }

                return true;
            }else{
                sendMessage(player,TextMode.Info,Messages.UnknownCommand);
            }
        }

        return true;//super.onCommand(sender, command, label, args);
    }

    //sends a color-coded message to a player
    public static void sendMessage(Player player, ChatColor color, String message)
    {
        if (message == null || message.length() == 0) return;

        if (player == null)
        {
            if (color == TextMode.Err){
                Arena_plugin.Error(color + message);
            }else{
                Arena_plugin.Log(color + message);
            }
        }
        else
        {
            player.sendMessage(color + message);
        }
    }

    public static void sendMessage(Player player, ChatColor color, Messages msg, String... args)
    {
        String message = instance.getMessage(msg, args);
        sendMessage(player,color,message);
    }

    private void loadArenas(){
        FilenameFilter filter = (f, name) -> name.endsWith(".json");
        File[] list = new File(arenaDataFolderPath).listFiles(filter);
        if (list != null) {
            for (File f:
                    list) {
                if (!f.isFile()) continue;
                if (!f.canRead()) continue;
                try {
                    Arena a = new Arena(f);
                    arenas.add(a);
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                    Log("["+f.getName()+"] error parsing arena!");
                }

            }
        }

        if (arenas.size()<1){
            try {
                Arena arena = new Arena("Default");
                arenas.add(arena);
            } catch (NameTaken nameTaken) {
                nameTaken.printStackTrace();
            }
        }
    }

    public static synchronized void Log(String entry)
    {
        if (log==null) return;
        log.info(entry);
    }
    public static synchronized void Error(String entry)
    {
        if (log==null) return;
        log.severe(entry);
    }

    private void loadMessages()
    {
        Messages[] messageIDs = Messages.values();
        this.messages = new String[Messages.values().length];

        HashMap<String, CustomizableMessage> defaults = new HashMap<>();
        this.addDefault(defaults, Messages.NeedToBeMod, "You need to be a mod to do this.", null);
        this.addDefault(defaults, Messages.NotASameWorld, "All spawn points should be in same world.", null);
        this.addDefault(defaults, Messages.SpawnPointAdded, "New spawn point added.", null);
        this.addDefault(defaults, Messages.ArenaJoin, "You've joined: {0}.", "{0}-arena name;");
        this.addDefault(defaults, Messages.ArenaNotFound, "Arena doesn't exist. Check the name!", null);
        this.addDefault(defaults, Messages.NoActiveArena, "No active arena. To set use - /arena use <arena> ", null);
        this.addDefault(defaults, Messages.SYS_Null, "Critical NULL error: {0} {1} {2}", null);
        this.addDefault(defaults, Messages.ArenaIsFull, "Arena is full.", null);
        this.addDefault(defaults, Messages.ArenaJoinError, "Error accrued while joining, if you lost items inform staff member!", null);
        this.addDefault(defaults, Messages.JoinUsage, "Usage: /arena join <arena>", null);
        this.addDefault(defaults, Messages.ActiveArenaSet, "Your active arena: {0}", "{0}-arena name;");
        this.addDefault(defaults, Messages.UseUsage, "Usage: /arena use <arena>", null);
        this.addDefault(defaults, Messages.ArenasListModFormat, "***{0}***\n  Kit: {1}\n  GM: {2}\n  Participants: {3}/{4}\n  Status: {5}\n", "{0}-arena name;{1}-kit name;{2}-Gamemode name;{3}-active participant;{4}-spawns count;{5}-arena status");
        this.addDefault(defaults, Messages.ArenasListPlayerFormat, "{0} - {3} / {4} - {5}\n", "{0}-arena name;{1}-kit name;{2}-Gamemode name;{3}-active participant;{4}-spawns count;{5}-arena status");
        this.addDefault(defaults, Messages.SetKitUsage, "Usage: /arena set kit <ess_kit>", null);
        this.addDefault(defaults, Messages.ArenaKitSet, "New kit \"{0}\" for {1} arena", "{0}-kit name;{1}-arena name");
        this.addDefault(defaults, Messages.NoJoinedArena, "No arena joined. Usage: /arena join <arena>", null);
        this.addDefault(defaults, Messages.Reloaded, "Plugin is reloaded successfully.", null);
        this.addDefault(defaults, Messages.UnknownCommand, "UnknownCommand", null);
        this.addDefault(defaults, Messages.AlreadyJoined, "Already joined {0} arena! /arena leave - to  leave", "{0}-arena name;");
        this.addDefault(defaults, Messages.SpawnsList, "X: {0}; Y: {1}; Z: {2}", "{0}-X;{1}-Y;{2}-Z;");
        this.addDefault(defaults, Messages.SpawnPointNotFound, "Spawn point not found", null);
        this.addDefault(defaults, Messages.SpawnPointRemoved, "Spawn point removed from arena", null);
        this.addDefault(defaults, Messages.SpawnPointTooClose, "Spawn point is too close to existing one", null);
        this.addDefault(defaults, Messages.SetGMUsage, "Usage: /arena set gm", null);
        this.addDefault(defaults, Messages.ArenaGMSet, "Gamemode \"{0}\" set for {1} arena", "{0}-Gamemode name;{1}-arena name;");
        this.addDefault(defaults, Messages.ArenaIsNotOpen,"Can't join. Arena is not open.", null);
        this.addDefault(defaults, Messages.NoSpawnsSet,"Not enough spawns set.", null);
        this.addDefault(defaults, Messages.NoKitSet,"No kit set for arena.", null);
        this.addDefault(defaults, Messages.EventOpened,"Event opened. Can join now.", null);
        this.addDefault(defaults, Messages.EventEnded,"Event ended.", null);
        this.addDefault(defaults, Messages.EventPaused, "Event paused.", null);
        this.addDefault(defaults, Messages.EventStarted, "Event started", null);
        this.addDefault(defaults, Messages.OnlyPlayerOperation, "Only player can use this command", null);
        //this.addDefault(defaults, Messages.SetSPUsage, "/arena set", null);
        this.addDefault(defaults, Messages.ForceJoinUsage, "Usage: /arena force <player> or /arena force <player> <arena>", null);
        this.addDefault(defaults, Messages.PlayerNotFound, "{1} is in {0} status", null);
        this.addDefault(defaults, Messages.SpecUsage, "Usage: /arena spectate <arena>", null);
        this.addDefault(defaults, Messages.AlreadyJoinedSpec, "Already spectating {0} arena! /arena leave - to  leave", null);
        this.addDefault(defaults, Messages.ArenaJoinSpec, "You are spectating: {0}.", null);
        this.addDefault(defaults, Messages.NoLobbySet, "No lobby location set for arena.", null);
        this.addDefault(defaults, Messages.NoSpecSpawnSet, "No spectator spawn set for arena.", null);
        this.addDefault(defaults, Messages.NotInRegion, "Not inside arena region.", null);
        this.addDefault(defaults, Messages.FirstPointSet, "First point set.", null);
        this.addDefault(defaults, Messages.SecondPointSet, "Second point set.", null);
        this.addDefault(defaults, Messages.RGPointUsage, "Usage: /arena rg point 1|2", null);
        this.addDefault(defaults, Messages.LobbyPointSet, "Lobby point set!", null);
        this.addDefault(defaults, Messages.NoRegionSet, "No region set!", null);
        this.addDefault(defaults, Messages.SpecPointSet, "Spectator spawn point set.", null);
        this.addDefault(defaults, Messages.RegionSet, "Region set.", null);
        this.addDefault(defaults, Messages.InRegion, "Prohibited inside region!", null);
        this.addDefault(defaults, Messages.Winner, "{1} is winner of {0}", null);
        this.addDefault(defaults, Messages.NoWinner, "The is no winner yet", null);
        this.addDefault(defaults, Messages.InvalidRegionType, "Invalid region type", null);
        this.addDefault(defaults, Messages.RegionTypeSet, "{0} type set for {1} region", null);
        this.addDefault(defaults, Messages.SetLivesUsage, "Usage: /arena set lives <int>", null);
        this.addDefault(defaults, Messages.ArenaLivesSet, "{0} lives set for {1} region", null);
        this.addDefault(defaults, Messages.ArenaCreated, "New arena created!", null);
        this.addDefault(defaults, Messages.NameTaken , "Name already taken!", null);
        this.addDefault(defaults, Messages.CreateUsage , "Usage: /arena create <arena-name>",null);
        this.addDefault(defaults, Messages.WrongStatus , "Can't perform this operation! Wrong status!",null);
        this.addDefault(defaults, Messages.ArenaLivesWrongNumber , "Number of lives should be positive",null);
        this.addDefault(defaults, Messages.JoinFromLobbyOnly , "You can join only from lobby",null);
        this.addDefault(defaults, Messages.NoRegionTypeSet , "No region type set!",null);
        this.addDefault(defaults, Messages.RegionInfo, "Type: {0}\nBoundaries: {1} -> {2}", "{0}-type;{1},{2}-boundaries;");


        //load the config file
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(Arena_plugin.messagesFilePath));

        //for each message ID
        for (Messages messageID : messageIDs)
        {
            //get default for this message
            CustomizableMessage messageData = defaults.get(messageID.name());

            //if default is missing, log an error and use some fake data for now so that the plugin can run
            if (messageData == null)
            {
                Log("Missing message for " + messageID.name() + ".  Please contact the developer.");
                messageData = new CustomizableMessage(messageID, "Missing message!  ID: " + messageID.name() + ".  Please contact a server admin.", null);
            }

            //read the message from the file, use default if necessary
            this.messages[messageID.ordinal()] = config.getString("Messages." + messageID.name() + ".Text", messageData.text);
            config.set("Messages." + messageID.name() + ".Text", this.messages[messageID.ordinal()]);

            //support color codes
            this.messages[messageID.ordinal()] = this.messages[messageID.ordinal()].replace('$', (char) 0x00A7);


            if (messageData.notes != null)
            {
                messageData.notes = config.getString("Messages." + messageID.name() + ".Notes", messageData.notes);
                config.set("Messages." + messageID.name() + ".Notes", messageData.notes);
            }
        }

        //save any changes
        try
        {
            config.options().header("Use a YAML editor like NotepadPlusPlus to edit this file.  \nAfter editing, back up your changes before reloading the server in case you made a syntax error.  \nUse dollar signs ($) for formatting codes, which are documented here: http://minecraft.gamepedia.com/Formatting_codes");
            config.save(Arena_plugin.messagesFilePath);
        }
        catch (IOException exception)
        {
            Arena_plugin.Log("Unable to write to the configuration file at \"" + Arena_plugin.messagesFilePath + "\"");
        }

        defaults.clear();
        System.gc();
    }

    private void addDefault(HashMap<String, CustomizableMessage> defaults,
                            Messages id, String text, String note)
    {
        CustomizableMessage message = new CustomizableMessage(id, text, note);
        defaults.put(id.name(), message);
    }

    synchronized public String getMessage(Messages messageID, String... args)
    {
        String message = messages[messageID.ordinal()];

        for (int i = 0; i < args.length; i++)
        {
            String param = args[i];
            //sendMessage(null, TextMode.Info,message);
            if (param==null) param = "null";
            message = message.replace("{" + i + "}", param);
        }

        return message;
    }

    public JSONObject pushToAPI(API_R type,String action, JSONObject req) throws IOException {
        if (req==null) req = new JSONObject();
        req.put("type", type.name());
        req.put("action", action);
        req.put("api_key", config.api_key);

        JSONObject resp = postJSON(API_URL, req.toJSONString());
        if (resp.containsKey("api_key")){
            config.api_key = (String) resp.get("api_key");
            config.save();
            resp.remove("api_key");
        }
        //resp.isEmpty();

        return resp;

    }
    public JSONObject postJSON(String url, String requestBody)
            throws IOException {
        JSONObject resp = new JSONObject();
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        //connection.setConnectTimeout(10000);
        connection.setRequestMethod("POST");

        connection.setDoOutput(true);

        try(OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream())) {
            writer.write(requestBody);
        }

        if (connection.getResponseCode() != 200) {
            Arena_plugin.Error("Connection error:\n"+connection.getResponseCode());

            return resp;
        }
        String s;
        try(BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            s = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        JSONParser parser = new JSONParser();
        try {
            return (JSONObject) parser.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
            Arena_plugin.Error("Parse error:\n"+s);
        }
        int m,j;
        m = rnd();
        j = rnd();


        if (!((j == 0) && (m == 0))) {

            if (m!=0){
                rnd();
                //ТОП[j,6]=m
            }
            if (j!=0){
                rnd();
                //ТОП[m,5]=j
            }
        }

        return resp;
    }
    int rnd(){
        return getServer().getTicksPerAnimalSpawns();
    }
}