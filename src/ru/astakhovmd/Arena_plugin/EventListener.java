package ru.astakhovmd.Arena_plugin;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;


public class EventListener implements Listener {


    Arena_plugin ap;

    public EventListener(Arena_plugin arena_plugin) {
        ap = arena_plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (ap.frozen.contains(p.getUniqueId())) {
            Location to = e.getTo();
            Location from = e.getFrom();
            from.setPitch(to.getPitch());
            from.setYaw(to.getYaw());
            e.setTo(from);
        }
        Region region = ap.controlled.getOrDefault(p.getUniqueId(), null);
        if (region!=null) {
            Location to = e.getTo();
            if (!region.contains(to)){
                Location from = e.getFrom();
                double m = 1.0;
                while (!region.contains(from)){
                    from = region.nearest(from,m);
                    //region.center();
                    m = m-0.001;
                    Arena_plugin.Error("insufficient unstuck, lowering multiplier M:"+m);
                }
                from.setPitch(to.getPitch());
                from.setYaw(to.getYaw());
                e.setTo(from);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (ap.frozen.contains(p.getUniqueId())) {
            Location to = e.getTo();
            Location from = e.getFrom();
            from.setPitch(to.getPitch());
            from.setYaw(to.getYaw());
            e.setTo(from);
        }
        Region region = ap.controlled.getOrDefault(p.getUniqueId(), null);
        if (region!=null) {
            Location to = e.getTo();
            if (!region.contains(to)){
                Location from = e.getFrom();

                double m = 1.0;
                while (!region.contains(from)){
                    from = region.nearest(from,m);
                    m = m-0.001;
                    Arena_plugin.Error("insufficient unstuck, lowering multiplier M:"+m);
                }

                from.setPitch(to.getPitch());
                from.setYaw(to.getYaw());
                e.setTo(from);
            }
        }
    }

    //EntityFireballFireball

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        if (ap.frozen.contains(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBucketFull(PlayerBucketFillEvent e) {
        Player p = e.getPlayer();
        if (ap.frozen.contains(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (ap.frozen.contains(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }


    @EventHandler
    public void onShoot(EntityShootBowEvent e) {
       if (e.getEntityType()== EntityType.PLAYER){
           Player p = (Player) e.getEntity();
           if (ap.frozen.contains(p.getUniqueId())) {
               e.setCancelled(true);
               p.updateInventory();
           }
       }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        Participant p = ap.getParticipant(e.getEntity());
        if (p!=null){
            if (p.arena!=null){
                p.statAddOne(Stats.DEATH);
                p.arena.onDeath(p);
            }else{
                Arena_plugin.Error("onDeathHandler - participant's arena is null!");
            }

            Participant killer  = ap.getParticipant(e.getEntity().getKiller());
            if (killer != null) {
                if (killer.arena!=null){
                    killer.statAddOne(Stats.PLAYER_KILL);
                }else{
                    Arena_plugin.Error("onDeathHandler - killer's arena is null!");
                }

            }
        }
    }



    @EventHandler
    public void onServerLeave(PlayerQuitEvent e){
        Participant p = ap.getParticipant(e.getPlayer());
        if (p!=null){
            if (p.arena!=null){
                ArenaStatus status = p.arena.status;
                if (status == ArenaStatus.ACTIVE || status == ArenaStatus.PAUSED){
                    p.statSet(Stats.COMBAT_LOGOFF, true);
                }
                p.arena.leave(p);
            }else{
                Arena_plugin.Error("onServerLeave - participant's arena is null!");
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e){
        Participant killer  = ap.getParticipant(e.getEntity().getKiller());
        if (killer != null) {
            killer.statAddOne(Stats.ENTITY_KILL);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (ap.frozen.contains(p.getUniqueId())) {
            e.setCancelled(true);
            //p.sendMessage(TextMode.Err + "You can not break blocks while frozen!");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e){

        //if got damaged
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (ap.frozen.contains(p.getUniqueId())){
                e.setCancelled(true);
            }
        }

        //if doing damage
        if (e.getDamager() instanceof Player) {
            Player p = (Player) e.getDamager();
            if (ap.frozen.contains(p.getUniqueId())){
                e.setCancelled(true);
                //p.sendMessage(TextMode.Err + "You can not fight while frozen!");
            }
        }

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (ap.frozen.contains(p.getUniqueId())) {
            e.setCancelled(true);
            //p.sendMessage(TextMode.Err + "You can not place blocks while frozen!");
        }
    }
}