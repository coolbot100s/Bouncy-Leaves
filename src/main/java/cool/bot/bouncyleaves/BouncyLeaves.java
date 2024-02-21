package cool.bot.bouncyleaves;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.BigDripleaf;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


import java.util.ArrayList;
import java.util.List;


public final class BouncyLeaves extends JavaPlugin implements Listener {


    final NamespacedKey timerNSK = new NamespacedKey(this, "yeetTimer");
    

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        // Nothing to do here
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer pdt = player.getPersistentDataContainer();
        Location location = player.getLocation();
        Block block = location.getBlock();
        BoundingBox playerBox = player.getBoundingBox();
        List<Block> readyLeaves = new ArrayList<>();

        // Ignore event if player was yeeted in the last few ticks
        if (pdt.getOrDefault(timerNSK, PersistentDataType.INTEGER, 0) > 0) {
            return;
        }

        // Get a list of blocks the player's feet are colliding with
        //TODO: mayhaps remove the flooring?
        List<Block> blocksUnderFeet = getBlocksInArea(new Location(location.getWorld(),Math.floor(playerBox.getMinX()),location.getY(),Math.floor(playerBox.getMinZ())),new Location(location.getWorld(),Math.floor(playerBox.getMaxX()),location.getY(),Math.floor(playerBox.getMaxZ())));
        // Check that list for any BIG_DRIPLEAF at full tilt
        for (Block curBlock : blocksUnderFeet) {
            if (curBlock.getType() == Material.BIG_DRIPLEAF) {
                BlockData curBlockData = curBlock.getBlockData();
                if (curBlockData instanceof BigDripleaf) {
                    BigDripleaf leaf = (BigDripleaf) curBlockData;
                    if (leaf.getTilt() == BigDripleaf.Tilt.FULL) {
                        readyLeaves.add(curBlock);
                    }
                }
            }
        }

        // if there are none ignore the event
        if (readyLeaves.isEmpty()) {
            return;
        }

        // check to see if the player is standing in air, or a big leaf, if not ignore the event.
        if (!(block.getType() == Material.BIG_DRIPLEAF || block.getType() == Material.AIR)) {
            return;
        }

        // Apply yeeting for each leaf, and reset each leaf.
        Boolean yeeted = false;
        
        for (Block leaf : readyLeaves) {
           yeeted = yeet(player, leaf);
        }
        
        if (yeeted) {
            attachTimerTag(player, 2);
        }
        
    }

    // Stuff that should be done per leaf, such as applying force vectors and reseting the leaf.
    private boolean yeet(Player player,Block block) {
        BigDripleaf leafData = (BigDripleaf) block.getBlockData();
        
        getServer().getScheduler().runTaskLater(this, () -> {
            // big jump
            Vector vector = new Vector(0, 1.4, 0);

            // directional
            vector.add(leafData.getFacing().getDirection().multiply(-1));

            player.setVelocity(player.getVelocity().add(vector));

            // reset leaf
            leafData.setTilt(BigDripleaf.Tilt.NONE);
            block.setBlockData(leafData);

            // sound
            // TODO: maybe this should only play once when the player is yeeted, rather than by each block? if for each block then we should randomize pitch and volume a bit
            player.playSound(player.getLocation(), Sound.ENTITY_SLIME_ATTACK, 2.0f, 1.0f);

        }, 1);
        return true;
    }

    // Returns an array with a list of blocks in a designated area
    public List<Block> getBlocksInArea(Location minLocation, Location maxLocation) {
        List<Block> blocks = new ArrayList<>();

        for (int x = minLocation.getBlockX(); x <= maxLocation.getBlockX(); x++) {
            for (int y = minLocation.getBlockY(); y <= maxLocation.getBlockY(); y++) {
                for (int z = minLocation.getBlockZ(); z <= maxLocation.getBlockZ(); z++) {
                    Location currentLocation = new Location(minLocation.getWorld(), x, y, z);
                    Block currentBlock = currentLocation.getBlock();

                    blocks.add(currentBlock);
                }
            }
        }
        return blocks;
    }

    public void attachTimerTag(Player player, int ticks) {
        
        // Set the timer tag in the player's persistent data container
        player.getPersistentDataContainer().set(timerNSK, PersistentDataType.INTEGER, ticks);

        // Start a BukkitRunnable to decrement the timer
        new BukkitRunnable() {
            @Override
            public void run() {
                int ticksLeft = player.getPersistentDataContainer().getOrDefault(timerNSK, PersistentDataType.INTEGER, 0);
                if (ticksLeft > 0) {
                    // Decrement the timer
                    ticksLeft--;
                    player.getPersistentDataContainer().set(timerNSK, PersistentDataType.INTEGER, ticksLeft);
                } else {
                    // Remove the timer tag when it reaches 0
                    player.getPersistentDataContainer().remove(timerNSK);
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

}

