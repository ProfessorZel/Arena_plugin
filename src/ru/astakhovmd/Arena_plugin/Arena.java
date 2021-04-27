package ru.astakhovmd.Arena_plugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FileUtils;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;


public class Arena {


    Random rand = new Random();

    ArrayList<Participant> participants = new ArrayList<>();
    ArrayList<Spectator> spectators = new ArrayList<>();

    ArrayList<Location> spawns = new ArrayList<>();
    ArenaStatus status = ArenaStatus.DISABLED;

    String name;
    Location lobby, spec;
    String kit_name;
    ArenaMode mode = ArenaMode.LAST_MAN_STANDING;
    GameMode gm = GameMode.SURVIVAL;
    HashMap<UUID,HashMap<Stats,Object>> statsArena = new HashMap<>();

    Region region;

    int lives = 1;
    long start_time = -1;
    private long edit_time = -1;


    Arena(File file) throws IOException, ParseException {

        String str = FileUtils.readFileToString(file, "UTF-8");
        JSONParser parser = new JSONParser();
        JSONObject arena = (JSONObject) parser.parse(str);

        name = file.getName().replace(".json","");
        //name = (String) arena.get("name");
        kit_name = (String) arena.get("kit_name");
        try {
            mode  = ArenaMode.valueOf((String) arena.get("mode"));
        }catch (IllegalArgumentException e){
            Arena_plugin.Log("["+name+"] Error parsing arena mode, fallback to LAST_MAN_STANDING");
            mode = ArenaMode.LAST_MAN_STANDING;
        }
        try {
            gm = GameMode.valueOf((String) arena.get("gm"));
        }catch (IllegalArgumentException e){
            Arena_plugin.Log("["+name+"] Error parsing gamemode, fallback to SURVIVAL");
            gm = GameMode.SURVIVAL;
        }
        try {
            region = new Region((JSONObject) arena.get("region"));
        }catch (NullPointerException e){
            Arena_plugin.Log("["+name+"] Error parsing region, fallback to null");
            region = null;
        }
        lobby = JSONtoloc((JSONObject) arena.get("lobby"));
        spec = JSONtoloc((JSONObject) arena.get("spec"));
        JSONtoSpawns((JSONArray) arena.get("spawns"));

        try{
            lives = ((Long) arena.get("lives")).intValue();
        }catch (Exception e){
            Arena_plugin.Log("["+name+"] Error parsing lives, fallback to 1");
            lives = 1;
        }
    }

    Arena(String name) throws NameTaken {
        setName(name);
    }

    void apply(){
        edit_time = System.currentTimeMillis();
        if (edit_time>System.currentTimeMillis()+1.0){
            try {
                this.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected JSONObject loctoJSON(Location loc){
        if (loc==null) return null;
        JSONObject spawn = new JSONObject();
        spawn.put("world", loc.getWorld().getName());
        spawn.put("X", loc.getX());
        spawn.put("Y", loc.getY());
        spawn.put("Z", loc.getZ());
        spawn.put("pitch", loc.getPitch());
        spawn.put("yaw", loc.getYaw());
        return spawn;
    }

    protected Location JSONtoloc(JSONObject obj){
        if (obj==null) return null;
        try {
            return new Location(Bukkit.getWorld((String) obj.get("world")), (double) obj.get("X"), (double) obj.get("Y"), (double) obj.get("Z"), ((Double) obj.get("pitch")).floatValue(), ((Double) obj.get("yaw")).floatValue());
        }catch (Exception ignored){
            return null;
        }
    }

    protected JSONArray spawnsToJSON(){
        JSONArray arr = new JSONArray();
        for (Location loc:
             spawns) {
            arr.add(loctoJSON(loc));
        }
        for (Participant participant:
                participants) {
            arr.add(loctoJSON(participant.spawn));
        }
        return arr;
    }

    protected void JSONtoSpawns(JSONArray arr){
        if (region==null) return;
        for (Object obj:
             arr) {
            Location l = JSONtoloc((JSONObject) obj);
            if (l==null) continue;
            if (region.contains(l)){
                spawns.add(l);
            }
        }
    }
    //Override
    public JSONObject toJSON() {
        JSONObject arena = new JSONObject();
        //arena.put("name", name);
        arena.put("kit_name", kit_name);
        arena.put("mode", mode.name());
        arena.put("gm", gm.name());
        arena.put("lobby", loctoJSON(lobby));
        arena.put("spec", loctoJSON(spec));
        arena.put("spawns", spawnsToJSON());
        arena.put("region", (region!=null)?region.toJson():null);
        arena.put("lives", lives);
        //Arena_plugin.Log(arena.toJSONString());
        return arena;
    }

    public JSONObject StatsToJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("mode", mode.name());
        obj.put("start_time", start_time);
        for (UUID uuid: statsArena.keySet()) {
            JSONObject user_stats = new JSONObject();
            HashMap<Stats, Object> userStats = statsArena.get(uuid);
            for (Stats s: userStats.keySet()) {
                switch (s){
                    case PLAYER_KILL:
                        user_stats.put(Stats.PLAYER_KILL.name(), (Integer)userStats.get(Stats.PLAYER_KILL));
                        break;
                    case ENTITY_KILL:
                        user_stats.put(Stats.ENTITY_KILL.name(), (Integer)userStats.get(Stats.ENTITY_KILL));
                        break;
                    case DEATH:
                        user_stats.put(Stats.DEATH.name(), (Integer)userStats.get(Stats.DEATH));
                        break;
                    case LAST_STANDING_TIME:
                        user_stats.put(Stats.LAST_STANDING_TIME.name(), (Long)userStats.get(Stats.LAST_STANDING_TIME));
                        break;
                    case COMBAT_LOGOFF:
                        user_stats.put(Stats.COMBAT_LOGOFF.name(), (Boolean)userStats.get(Stats.COMBAT_LOGOFF));
                        break;
                }
            }
            obj.put(uuid.toString(), user_stats);
        }
        //Arena_plugin.Log(arena.toJSONString());
        return obj;
    }

    public UUID getWinner(){
        //if (status != ArenaStatus.ENDED) return null;
        UUID winner = null;
        switch (mode){
            case LAST_MAN_STANDING:
                long time = -1;
                for (UUID plr:
                        statsArena.keySet()) {
                    //Long val = (Long) statsArena.get(plr).get(Stats.DEATH_TIME);
                    Long val = (Long) statsArena.get(plr).get(Stats.LAST_STANDING_TIME);
                    if (val == null) continue;
                    if ((boolean)statsArena.get(plr).getOrDefault(Stats.COMBAT_LOGOFF, false)) continue;
                    if (val > time){
                        time = val;
                        winner = plr;
                    }
                }
                break;
            case MAX_KILLS:
                Integer kills = -1;
                for (UUID plr:
                        statsArena.keySet()) {
                    Integer val = (Integer)statsArena.get(plr).get(Stats.PLAYER_KILL);
                    if (val == null) continue;
                    if ((boolean)statsArena.get(plr).getOrDefault(Stats.COMBAT_LOGOFF, false)) continue;
                    if (val > kills){
                        kills = val;
                        winner = plr;
                    }
                }
                break;
        }

        return winner;
    }

    public void setLives(int lives) throws NumberFormatException {
        if (lives<1) throw new NumberFormatException();
        this.lives = lives;
        apply();
    }

    public void setRegion(Region region) {
        this.region = region;
        apply();
    }

    public void setKit_name(String kit_name) {
        this.kit_name = kit_name;
        apply();
    }

    public Messages setLobbyLoc(Location loc){
        if (region==null) return Messages.NoRegionSet;
        if (region.contains(loc)) return Messages.InRegion;
        lobby = loc;
        apply();
        return Messages.LobbyPointSet;
    }

    public Messages setSpectatorLoc(Location loc){
        if (region==null) return Messages.NoRegionSet;
        if (!region.contains(loc)) return Messages.NotInRegion;
        spec = loc;
        apply();
        return Messages.SpecPointSet;
    }

    public void setName(String nname) throws NameTaken {
        if (Arena_plugin.instance.arenas.stream().anyMatch(item->item.name.equalsIgnoreCase(nname))){
            throw new NameTaken();
        }
        this.name = nname;
        apply();
    }

    public void setGM(GameMode s){
        gm = s;
        apply();
    }

    @Deprecated
    public void setStatus(ArenaStatus s){
        status = s;
        apply();
        //ArenaStatus.Parse("ssss");
    }

    public Messages openEvent(){
        if (status!=ArenaStatus.DISABLED || Arena_plugin.instance.config.debug) return Messages.WrongStatus;
        //toJSON();

        if (kit_name==null || kit_name.isEmpty()) return Messages.NoKitSet;
        if (lobby == null) return Messages.NoLobbySet;
        if (region == null) return Messages.NoRegionSet;
        if (spec == null) return Messages.NoSpecSpawnSet;
        if (spawns.size() < Arena_plugin.instance.config.min_spawns) return Messages.NoSpawnsSet;

        start_time = -1;
        statsArena.clear();

        status = ArenaStatus.OPEN;
        return Messages.EventOpened;
    }

    public Messages startEvent(){
        //if (participants.size()<2) return Messages.NoParticipants;
        if ((status!=ArenaStatus.OPEN && status!=ArenaStatus.PAUSED) || Arena_plugin.instance.config.debug) return Messages.WrongStatus;
        status = ArenaStatus.ACTIVE;
        for (Participant p: participants) {
            p.freeze(false);
            p.notify(Messages.EventStarted);
        }

        for (Spectator s: spectators) {
            s.notify(Messages.EventStarted);
        }
        if (start_time == -1) start_time = System.currentTimeMillis();
        return Messages.EventStarted;
    }

    @Deprecated
    public Messages pauseEvent(){
        if (status!=ArenaStatus.ACTIVE || Arena_plugin.instance.config.debug) return Messages.WrongStatus;
        status = ArenaStatus.PAUSED;
        for (Participant p: participants) {
            p.freeze(true);
            p.notify(Messages.EventPaused);
        }

        for (Spectator s: spectators) {
            s.notify(Messages.EventPaused);
        }
        return Messages.EventPaused;
    }

    public Messages endEvent(){
        if (status!=ArenaStatus.ACTIVE || Arena_plugin.instance.config.debug) return Messages.WrongStatus;
        status = ArenaStatus.DISABLED;
        while (participants.size()>0) {
            Participant p = participants.get(0);
            p. notify(Messages.EventEnded);
            leave(p);
        }
        while (spectators.size()>0) {
            Spectator s = spectators.get(0);
            s.notify(Messages.EventEnded);
            leave(s);
        }
        saveStats();
        return Messages.EventEnded;
    }


    public Participant getParticipant(UUID player){
        for (Participant p: participants) {
            if (p.player.getUniqueId() == player){
                return p;
            }
        }
        return null;
    }

    public Spectator getSpectator(UUID player){
        for (Spectator p: spectators) {
            if (p.player.getUniqueId() == player){
                return p;
            }
        }
        return null;
    }

    public void onDeath(Participant p){
        p.lives -=1;
        if (p.lives<=0){
            p.statSet(Stats.LAST_STANDING_TIME, System.currentTimeMillis());
            leave(p);
        }else{
            p.spawn();
        }

    }

    public Messages joinSpectator(Player p1){
        if (status != ArenaStatus.OPEN && status != ArenaStatus.ACTIVE && status != ArenaStatus.PAUSED) return Messages.ArenaIsNotOpen;

        Spectator p = new Spectator(p1, this);
        if (lobby!=null){
            p.tp(lobby);
        }
        p.spectate(true);
        p.tp(spec);

        if (region!=null){
            Arena_plugin.instance.controlled.put(p.player.getUniqueId(), region);
        }

        spectators.add(p);

        return Messages.ArenaJoinSpec;
    }

    public Messages join(Player p1){
        if (status != ArenaStatus.OPEN) return Messages.ArenaIsNotOpen;
        if (spawns.size()<=0)  return Messages.ArenaIsFull;

        Participant p = new Participant(p1, this);
        try {
             p.save();


             p.reset();
             p.giveIKit(kit_name);
             //p.giveEKit(kit_name);
             //p.giveIKit(kit_name);

             p.spawn = spawns.remove(rand.nextInt(spawns.size()));
             p.spawn();
             p.freeze(true);
             participants.add(p);
             //notifyMods(Messages.ArenaJoinMod, p1.getDisplayName());
             if (region!=null){
                 Arena_plugin.instance.controlled.put(p.player.getUniqueId(), region);
             }
             return Messages.ArenaJoin;
        } catch (IOException e) {
            e.printStackTrace();

            return Messages.ArenaJoinError;
        }

    }

    public int maxParticipants(){
        return spawns.size()+participants.size();
    }
    public int activeParticipants(){
        return participants.size();
    }

    public void leave(Spectator p){
        p.spectate(false);
        if (region!=null){
            Arena_plugin.instance.controlled.remove(p.player.getUniqueId());
        }

        p.arena = null;
        spectators.remove(p);

        p.tp(lobby);
    }

    public void leave(Participant p){
        p.statSet(Stats.LAST_STANDING_TIME, System.currentTimeMillis());

        spawns.add(p.spawn);
        p.spawn = null;

        p.reset();

        p.freeze(false);
        //p.spectate(false);
        if (region!=null){
            Arena_plugin.instance.controlled.remove(p.player.getUniqueId());
        }

        try {
            p.restore();
        } catch (IOException e) {
            e.printStackTrace();
        }

        p.arena = null;
        participants.remove(p);

        if (participants.size()==1){
            endEvent();
        }


        p.tp(lobby);
    }

    public Messages addSpawn(Location loc){
        if (loc==null) return Messages.SYS_Null;
        if (region==null) return Messages.NoRegionSet;
        if (!region.contains(loc)) return Messages.NotInRegion;

        if (spawns.stream().anyMatch(item->item.distance(loc)<2)){
            return Messages.SpawnPointTooClose;
        }
        spawns.add(loc);

        apply();
        return Messages.SpawnPointAdded;
    }

    public Messages removeSpawn(Location loc){
        double dist = -1;
        Location target = null;
        for (Location l:
             spawns) {
            double d = loc.distance(l);
            if (dist<0){
                dist = d;
                target = l;
            }
            if (dist > d){
                target = l;
                dist = d;
            }
        }
        if (dist>2){
            return Messages.SpawnPointNotFound;
        }
        spawns.remove(target);
        apply();
        return Messages.SpawnPointRemoved;
        //spawns = spawns.stream().filter(location -> location.distance(loc)<2);
    }

    public static String beautiful(String input) {
        int tabCount = 0;

        StringBuilder inputBuilder = new StringBuilder();
        char[] inputChar = input.toCharArray();

        for (int i = 0; i < inputChar.length; i++) {
            String charI = String.valueOf(inputChar[i]);
            if (charI.equals("}") || charI.equals("]")) {
                tabCount--;
                if (!String.valueOf(inputChar[i - 1]).equals("[") && !String.valueOf(inputChar[i - 1]).equals("{"))
                    inputBuilder.append(newLine(tabCount));
            }
            inputBuilder.append(charI);

            if (charI.equals("{") || charI.equals("[")) {
                tabCount++;
                if (String.valueOf(inputChar[i + 1]).equals("]") || String.valueOf(inputChar[i + 1]).equals("}"))
                    continue;

                inputBuilder.append(newLine(tabCount));
            }

            if (charI.equals(",")) {
                inputBuilder.append(newLine(tabCount));
            }
        }

        return inputBuilder.toString();
    }

    private static String newLine(int tabCount) {
        StringBuilder builder = new StringBuilder();

        builder.append("\n");
        for (int j = 0; j < tabCount; j++)
            builder.append("  ");

        return builder.toString();
    }

    public void saveStats()
    {
        if (statsArena.size()>0){

            JSONObject req = new JSONObject();
            req.put("stats", this.StatsToJSON());
            try {
                JSONObject resp = Arena_plugin.instance.pushToAPI(API_R.STATS,"new", req);
                if (resp.containsKey("editor_url")){
                    Arena_plugin.Log((String) resp.get("editor_url"));
                }
            }catch (IOException ignored){}
            try {
            FileUtils.writeStringToFile(
                    new File(
                            Arena_plugin.eventDataFolderPath+File.separator+name+"_"+ start_time +".json"),
                                    beautiful( this.StatsToJSON().toJSONString()),
                            "UTF-8");
            }catch (IOException ignored){}

        }

    }

    public void save() throws IOException {
        FileUtils.writeStringToFile(new File(Arena_plugin.arenaDataFolderPath+File.separator+name+".json"), beautiful( this.toJSON().toJSONString()),"UTF-8");
    }

}
