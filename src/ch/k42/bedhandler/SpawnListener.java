package ch.k42.bedhandler;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Created by Thomas on 02.03.14.
 */
public class SpawnListener implements Listener {
    private static final String spawnSetMessage = "Your spawn was set to your bed... If it still exists.";
    private static final String bedNotReadyMessage = "Your bed wasn't ready.";
    private static final String bedClickMessage = "Your spawn will be set to this bed in %d minutes  ... If it still exists then.";
    private static final String bedClickMessageRep = "Your spawn is already set to this bed.";
    private static final String bedRespawnMessage = "You have spawned here now. Your bed will again be ready in %d minutes.";

    private Map<Player,PlayerBed> playerMap = new HashMap();
    private int deathCooldown = 300000;
    private Plugin plugin;

    public SpawnListener(int deathCooldown, Plugin plugin) {
        this.deathCooldown = deathCooldown;
        this.plugin = plugin;
    }

    private class PlayerBed
    {
        Player player;
        Location location;
        long readyTime;

        private PlayerBed(Player player, Location location, long readyTime) {
            this.player = player;
            this.location = location;
            this.readyTime = readyTime;
        }

        private PlayerBed(Player player, Location location) {
            this.player = player;
            this.location = location;
            updateTime();
        }

        private void updateTime(){
            this.readyTime = System.currentTimeMillis() + SpawnListener.this.deathCooldown;
        }

        private boolean isReady(long now){
            return readyTime<=now;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PlayerBed playerBed = (PlayerBed) o;

            if (!location.equals(playerBed.location)) return false;
            if (!player.equals(playerBed.player)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = player.hashCode();
            result = 31 * result + location.hashCode();
            return result;
        }
    }


    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        long now = System.currentTimeMillis();
        Player deadPlayer = event.getPlayer();
        PlayerBed bed = playerMap.get(deadPlayer);

        if(bed==null){ // haven't found bed, server restart? new player?
            if(deadPlayer.getBedSpawnLocation()==null) return; // no bed found
            playerMap.put(deadPlayer,new PlayerBed(deadPlayer,deadPlayer.getBedSpawnLocation()));
            deadPlayer.sendMessage(ChatColor.GRAY + String.format(bedRespawnMessage,deathCooldown /60000));
        }else if(!bed.isReady(now)){ //cooldown still running
            deadPlayer.setBedSpawnLocation(null);   //player should spawn randomly
            deadPlayer.sendMessage(ChatColor.GRAY + bedNotReadyMessage); // notify user that his spawn was reset
        }else{ // bed set, player will respawn there
            deadPlayer.sendMessage(ChatColor.GRAY + String.format(bedRespawnMessage,deathCooldown /60000));
            bed.updateTime(); // reset timer
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if(event.hasBlock())
        {
            if((event.getClickedBlock().getType().equals(Material.BED) || event.getClickedBlock().getType().equals(Material.BED_BLOCK)) && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
                //player right-clicked on a bed
                Player playerEntering = event.getPlayer();
                Location l= event.getClickedBlock().getLocation();
                if(playerMap.containsValue(new PlayerBed(playerEntering, l))){
                    playerEntering.sendMessage(ChatColor.GRAY + bedClickMessageRep);
                }else {
                    playerEntering.setBedSpawnLocation(l);
                    playerMap.put(playerEntering, new PlayerBed(playerEntering, l)); // add new Bed
                    playerEntering.sendMessage(ChatColor.GRAY + String.format(bedClickMessage,deathCooldown/60000));
                    notifyPlayer(playerEntering);
                    event.setUseInteractedBlock(Event.Result.DENY); // what is this??? This might be the xp not resetted problem
                }
            }
        }
    }

    private void notifyPlayer(final Player p){
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin,new Runnable() {
                @Override
                public void run() {
                    p.sendMessage(ChatColor.GRAY + spawnSetMessage);
                }
            },this.deathCooldown+1000); // add a second to be sure it worked
    }

}
