package cool.bot.bouncyleaves;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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


import java.util.*;


public final class BouncyLeaves extends JavaPlugin implements Listener {

    // Vars
    final NamespacedKey timerNSK = new NamespacedKey(this, "yeetTimer");
    Random random = new Random();

    // Settings
    public boolean playSound = true;
    public float volumeMin = 1.6f;
    public float volumeMax = 1.8f;
    public float pitchMin = 0.96f;
    public float pitchMax = 1.04f;
    public boolean logicalCollisions = true;
    public int tiltLevel = 2;
    public int coolDown = 2;
    public int yeetDelay = 1;
    public float jumpPowerVerticalMin = 1.2f;
    public float jumpPowerVerticalMax = 1.2f;
    public float jumpPowerHorizontalMin = 0.8f;
    public float jumpPowerHorizontalMax = 0.8f;
    public boolean horizontalFlingBack = true;
    public float verticalStackMultiplier = 0.8f;
    public float horizontalStackMultiplier = 0.8f;
    public boolean noYeetWhenSneaking = true;

    

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
        if (coolDown > 0) {
            if (pdt.getOrDefault(timerNSK, PersistentDataType.INTEGER, 0) > 0) {
                player.sendMessage("not ready"); //DEBUG
                return;
            }
        }

        // Ignore event if player is sneaking (if enabled)
        if (noYeetWhenSneaking) {
            if (player.isSneaking()) {
                return;
            }
        }

        // check to see if the player is standing in air, or a big leaf, if not ignore the event.
        if (logicalCollisions) {
            if (!(block.getType() == Material.BIG_DRIPLEAF || block.getType() == Material.AIR)) {
                return;
            }
        }


        // Get a list of blocks the player's feet are colliding with
        List<Block> blocksUnderFeet = getBlocksInArea(new Location(location.getWorld(),playerBox.getMinX(),location.getY(),playerBox.getMinZ()),new Location(location.getWorld(),playerBox.getMaxX(),location.getY(),playerBox.getMaxZ()));
        // Check that list for any BIG_DRIPLEAF at appropriate tilt
        for (Block curBlock : blocksUnderFeet) {
            if (curBlock.getType() == Material.BIG_DRIPLEAF) {
                BlockData curBlockData = curBlock.getBlockData();
                if (curBlockData instanceof BigDripleaf) {
                    BigDripleaf leaf = (BigDripleaf) curBlockData;
                    if (leaf.getTilt().ordinal() == tiltLevel) {
                        readyLeaves.add(curBlock);
                    }
                }
            }
        }

        // if there are none ignore the event
        if (readyLeaves.isEmpty()) {
            return;
        }

        Vector yeetForce = makeYeetForce(readyLeaves);

        // Schedule the player to be yeeted
        getServer().getScheduler().runTaskLater(this, () -> {
           player.setVelocity(player.getVelocity().add(yeetForce));

            // Reset the leafs and do other per leaf logic at time of yeeting
            for (Block leaf : readyLeaves) {
                BigDripleaf leafData = (BigDripleaf) leaf.getBlockData();
                // reset leaf
                leafData.setTilt(BigDripleaf.Tilt.NONE);
                leaf.setBlockData(leafData);

                // sound
                if (playSound) {
                    player.playSound(player.getLocation(), Sound.ENTITY_SLIME_ATTACK, randfRange(volumeMin, volumeMax), randfRange(pitchMin, pitchMax));
                }
            }

        }, yeetDelay);

        // add cooldown timer to the player that got yeeted
        attachTimerTag(player,timerNSK, coolDown);
        
    }

    private Vector makeYeetForce(List<Block> readyLeaves) {
        // Create the force vector for the yeeting
        Vector yeetForce = new Vector(0, 0, 0);
        double verticalComponent = randfRange(jumpPowerVerticalMin, jumpPowerVerticalMax);
        double horizontalComponent = randfRange(jumpPowerHorizontalMin, jumpPowerHorizontalMax);

        // multiplier per cardinal
        float nCount = 1;
        float sCount = 1;
        float eCount = 1;
        float wCount = 1;

        int i = 0;

        for (Block leaf : readyLeaves) {
            BigDripleaf leafData = (BigDripleaf) leaf.getBlockData();

            if (horizontalComponent != 0.0f) {
                BlockFace facing = leafData.getFacing();
                Vector horVec = facing.getDirection();
                horVec.add(facing.getDirection().multiply(horizontalComponent));

                if (horizontalStackMultiplier != 1 && i >= 1) {
                    if (facing == BlockFace.NORTH) {
                        horVec.multiply(Math.pow(horizontalComponent, nCount));
                        nCount++;
                    }
                    if (facing == BlockFace.SOUTH) {
                        horVec.multiply(Math.pow(horizontalComponent, sCount));
                        sCount++;
                    }
                    if (facing == BlockFace.EAST) {
                        horVec.multiply(Math.pow(horizontalComponent, eCount));
                        eCount++;
                    }
                    if (facing == BlockFace.WEST ) {
                        horVec.multiply(Math.pow(horizontalComponent, wCount));
                        wCount++;
                    }
                }
                if (horizontalFlingBack) {
                    horVec.multiply(-1);
                }
                yeetForce.add(horVec);
            }
            Vector vertVec = new Vector(0,verticalComponent,0);
            if (verticalStackMultiplier != 1 && i >= 1) {
                vertVec.multiply(Math.pow(verticalComponent, i));
            }
            yeetForce.add(vertVec);
            i++;
        }

        return yeetForce;
    }

    // Attatch a timer to the player
    public void attachTimerTag(Player player, NamespacedKey nsk, int ticks) {

        PersistentDataContainer pdc = player.getPersistentDataContainer();

        pdc.set(nsk, PersistentDataType.INTEGER, ticks);


        new BukkitRunnable() {
            @Override
            public void run() {
                int ticksLeft = pdc.getOrDefault(nsk, PersistentDataType.INTEGER, 0);
                if (ticksLeft > 0) {
                    // Decrement the timer
                    ticksLeft--;
                    pdc.set(nsk, PersistentDataType.INTEGER, ticksLeft);
                } else {
                    // Remove the timer tag when it reaches 0
                    pdc.remove(nsk);
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    // Utilities
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

    // Return a random float from a range
    public float randfRange(float min, float max) {
        float v = random.nextFloat();
        return min + v * (max-min);
    }

}

