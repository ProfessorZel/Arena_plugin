package ru.astakhovmd.Arena_plugin;


import com.earth2me.essentials.*;


import com.earth2me.essentials.craftbukkit.InventoryWorkaround;
import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.KeywordReplacer;
import com.earth2me.essentials.textreader.SimpleTextInput;
import com.earth2me.essentials.utils.NumberUtil;
import net.ess3.api.events.KitClaimEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getServer;

public class Participant {
    public Player player;
    public Arena arena;
    public Location spawn;

    int lives;
    //HashMap<Stats,Integer> statsHM = new HashMap<>();
    File file;


    Participant(Player player, Arena arena){
        this.player = player;
        this.arena = arena;

        lives = arena.lives;

        file = new File(Arena_plugin.playerDataFolderPath, player.getUniqueId().toString() + "_inv" + ".yml");

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

    @Deprecated
    public void spectate(boolean state) {
        /*
        //if (status!=ParticipantStatus.ACTIVE) return;
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
        }
        */

    }
    public void statAddOne(Stats stat){
        statAdd(stat, 1);
    }

    public void statAdd(Stats stat, int i){
        HashMap<Stats,Object> statsHM = arena.statsArena.getOrDefault(player.getUniqueId(), new HashMap<>());
        int v = (int) statsHM.getOrDefault(stat, 0) + i;
        statsHM.put(stat,v);
        arena.statsArena.put(player.getUniqueId(), statsHM);
    }

    public void statSet(Stats stat, Object i){
        HashMap<Stats,Object> statsHM = arena.statsArena.getOrDefault(player.getUniqueId(), new HashMap<>());
        statsHM.put(stat,i);
        arena.statsArena.put(player.getUniqueId(), statsHM);
    }

    public Object getstats(Stats stat){
        HashMap<Stats,Object> statsHM = arena.statsArena.getOrDefault(player.getUniqueId(), new HashMap<>());
        return statsHM.get(stat);
    }

    public void reset(){
        player.closeInventory();
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setSaturation(0);
        player.setFoodLevel(20);
        player.setHealthScale(20);
        player.setHealth(20);
        player.setGameMode(arena.gm);
        //player.getActivePotionEffects().clear();
        for (PotionEffect pef: player.getActivePotionEffects()) {
            player.removePotionEffect(pef.getType());
        }

    }


    public void save() throws IOException {
        YamlConfiguration c = new YamlConfiguration();

        //saving inv
        Inventory inventory = player.getInventory();
        ItemStack[] inv = inventory.getContents();
        c.set("inventory", inv);

        Inventory einventory = player.getEnderChest();
        ItemStack[] einv = einventory.getContents();
        c.set("e_inventory", einv);

        Location loc = player.getLocation();
        c.set("location", loc);

        boolean fl = player.getAllowFlight();
        c.set("allow_flight", fl);

        Location bed_location = player.getBedSpawnLocation();
        c.set("bed_spawn_loc", bed_location);


        double health_scale = player.getHealthScale();
        c.set("health_scale", health_scale);
        double health = player.getHealth();
        c.set("health", health);

        float exp = player.getExp();
        c.set("exp", exp);

        int food_lvl = player.getFoodLevel();
        c.set("food_lvl", food_lvl);

        float satu = player.getSaturation();
        c.set("saturation", satu);

        Collection<PotionEffect> eff =  player.getActivePotionEffects();
        c.set("pots_effects",eff);
        //player.addPotionEffects(eff);

        String gm = player.getGameMode().name();
        c.set("gamemode",gm);

        c.save(file);
    }


    public void restore(/*Location tp_prev*/) throws IOException {
        YamlConfiguration c = YamlConfiguration.loadConfiguration(file);

        ArrayList<ItemStack> inv = (ArrayList<ItemStack>) c.get("inventory");
        for (int l = 0; l < inv.size(); l++) {
            ItemStack i = inv.get(l);
            if (i==null){
                i = new ItemStack(Material.AIR);
            }
            player.getInventory().setItem(l,i);
        }

        ArrayList<ItemStack> einv = (ArrayList<ItemStack>) c.get("e_inventory");
        for (int l = 0; l < einv.size(); l++) {
            ItemStack ei = einv.get(l);
            if (ei==null){
                ei = new ItemStack(Material.AIR);
            }
            player.getEnderChest().setItem(l,ei);
        }

        //player.updateInventory();

        /*if (tp_prev==null){
            Location loc = (Location) c.get("location");
            if (loc != null) {
                tp(loc);
            }else{
                Arena_plugin.Error("player's location is null! Fallback to world spawn!");
                tp(player.getWorld().getSpawnLocation());
            }
        }else{
            tp(tp_prev);
        }*/


        boolean fl = (boolean) c.get("allow_flight");
        player.setAllowFlight(fl);

        Location bed_location = (Location) c.get("bed_spawn_loc");
        player.setBedSpawnLocation(bed_location);


        double health_scale = (double) c.get("health_scale");
        player.setHealthScale(health_scale);


        double health = (double) c.get("health");
        player.setHealth(health);

        Double exp = (Double) c.get("exp");
        player.setExp(exp.floatValue());

        int food_lvl = (int) c.get("food_lvl");
        player.setFoodLevel(food_lvl);

        Double satu = (Double) c.get("saturation");
        player.setSaturation(satu.floatValue());

        Collection<PotionEffect> eff = (Collection<PotionEffect>) c.get("pots_effects");
        player.addPotionEffects(eff);

        GameMode gm = GameMode.valueOf((String) c.get("gamemode"));
        player.setGameMode(gm);

        file.delete();
    }
    public void giveIKit(String name) {
        Essentials essentials = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
        if (essentials==null) {
            Arena_plugin.Log("No essentials! Kit has not been given!");
            return;
        }
        User euser = essentials.getUser(player);
        try {
            Kit kit = new Kit(name, essentials);
            kit.expandItems(euser);

        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
    public void giveEKit(String name) {
        Essentials essentials = (Essentials) getServer().getPluginManager().getPlugin("Essentials");
        if (essentials==null) {
            Arena_plugin.Log("No essentials! Kit has not been given!");
            return;
        }
        User euser = essentials.getUser(player);
        try {
            Kit kit = new Kit(name, essentials);
            List<String> d = kit.getItems();
            for (String l:
                 d) {
                Arena_plugin.sendMessage(null,TextMode.Info, l);
            }
            giveItems(essentials, euser,  kit, player.getEnderChest());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void spawn(){
        //player.setBedSpawnLocation(spawn);
        tp(spawn);
    }
    public void tp(Location location) {
        player.teleport(location);
    }

    public void notify(Messages l, String... args) {
        Arena_plugin.sendMessage(player,TextMode.Info,l,args);
        //player.teleport(location);
    }

    public boolean giveItems(Essentials ess, User user, Kit kit, Inventory inventory) throws Exception {
        return giveItems( ess, user,  kit,  inventory, kit.getItems(user));
    }

    public boolean giveItems(Essentials ess, User user, Kit kit, Inventory inventory, List<String> items) throws Exception {
        try {
            IText input = new SimpleTextInput(items);
            IText output = new KeywordReplacer(input, user.getSource(), ess, true, true);
            KitClaimEvent event = new KitClaimEvent(user, kit);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            } else {
                boolean spew = false;
                boolean allowUnsafe = ess.getSettings().allowUnsafeEnchantments();
                boolean currencyIsSuffix = ess.getSettings().isCurrencySymbolSuffixed();
                List<ItemStack> itemList = new ArrayList();
                List<String> commandQueue = new ArrayList();
                List<String> moneyQueue = new ArrayList();
                Iterator var12 = output.getLines().iterator();

                while(true) {
                    String kitItem;
                    while(true) {
                        if (!var12.hasNext()) {
                            boolean allowOversizedStacks = user.isAuthorized("essentials.oversizedstacks");
                            boolean isDropItemsIfFull = ess.getSettings().isDropItemsIfFull();
                            Map overfilled;
                            Iterator var25;
                            if (isDropItemsIfFull) {
                                if (allowOversizedStacks) {
                                    overfilled = InventoryWorkaround.addOversizedItems(inventory, ess.getSettings().getOversizedStackSize(), (ItemStack[])itemList.toArray(new ItemStack[0]));
                                } else {
                                    overfilled = InventoryWorkaround.addItems(inventory, (ItemStack[])itemList.toArray(new ItemStack[0]));
                                }

                                for(var25 = overfilled.values().iterator(); var25.hasNext(); spew = true) {
                                    ItemStack itemStack = (ItemStack)var25.next();
                                    int spillAmount = itemStack.getAmount();
                                    if (!allowOversizedStacks) {
                                        itemStack.setAmount(Math.min(spillAmount, itemStack.getMaxStackSize()));
                                    }

                                    while(spillAmount > 0) {
                                        user.getWorld().dropItemNaturally(user.getLocation(), itemStack);
                                        spillAmount -= itemStack.getAmount();
                                    }
                                }
                            } else {
                                if (allowOversizedStacks) {
                                    overfilled = InventoryWorkaround.addAllOversizedItems(inventory, ess.getSettings().getOversizedStackSize(), (ItemStack[])itemList.toArray(new ItemStack[0]));
                                } else {
                                    overfilled = InventoryWorkaround.addAllItems(inventory, (ItemStack[])itemList.toArray(new ItemStack[0]));
                                }

                                if (overfilled != null) {
                                    user.sendMessage(I18n.tl("kitInvFullNoDrop", new Object[0]));
                                    return false;
                                }
                            }

                            user.getBase().updateInventory();
                            var25 = moneyQueue.iterator();

                            String cmd;
                            while(var25.hasNext()) {
                                cmd = (String)var25.next();
                                BigDecimal value = new BigDecimal(cmd.trim());
                                Trade t = new Trade(value, ess);
                                t.pay(user, Trade.OverflowType.DROP);
                            }

                            var25 = commandQueue.iterator();

                            while(var25.hasNext()) {
                                cmd = (String)var25.next();
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            }

                            if (spew) {
                                user.sendMessage(I18n.tl("kitInvFull", new Object[0]));
                            }

                            return true;
                        }

                        kitItem = (String)var12.next();
                        if (!currencyIsSuffix) {
                            if (kitItem.startsWith(ess.getSettings().getCurrencySymbol())) {
                                break;
                            }
                        } else if (kitItem.endsWith(ess.getSettings().getCurrencySymbol())) {
                            break;
                        }

                        if (kitItem.startsWith("/")) {
                            String command = kitItem.substring(1);
                            String name = user.getName();
                            command = command.replace("{player}", name);
                            commandQueue.add(command);
                        } else {
                            String[] parts = kitItem.split(" +");
                            ItemStack parseStack = ess.getItemDb().get(parts[0], parts.length > 1 ? Integer.parseInt(parts[1]) : 1);
                            if (parseStack.getType() != Material.AIR) {
                                MetaItemStack metaStack = new MetaItemStack(parseStack);
                                if (parts.length > 2) {
                                    metaStack.parseStringMeta((CommandSource)null, allowUnsafe, parts, 2, ess);
                                }

                                itemList.add(metaStack.getItemStack());
                            }
                        }
                    }

                    moneyQueue.add(NumberUtil.sanitizeCurrencyString(kitItem, ess));
                }
            }
        } catch (Exception var19) {
            user.getBase().updateInventory();
            ess.getLogger().log(Level.WARNING, var19.getMessage());
            throw new Exception(I18n.tl("kitError2", new Object[0]), var19);
        }
    }
}