package cool.bot.bouncyleaves;

import com.sun.crypto.provider.HmacMD5KeyGenerator;
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


        // Apply yeeting for each leaf, and reset each leaf.
//        Boolean yeeted = false;
        
//        for (Block leaf : readyLeaves) {
//           yeeted = yeet(player, leaf);
//        }


        // Create the force vector for the yeeting
//        Vector yeetForce = new Vector(0, 0, 0); // Initialize the force vector with zero components
//        double verticalComponent = randfRange(jumpPowerVerticalMin, jumpPowerVerticalMax); // Determine the random vertical component within the specified range
//        double horizontalComponent = randfRange(jumpPowerHorizontalMin, jumpPowerHorizontalMax); // Determine the random horizontal component within the specified range
//
//        // Iterate through the list of ready leaves
//        for (int i = 0; i < readyLeaves.size(); i++) {
//            Block leaf = readyLeaves.get(i); // Get the current leaf block
//            BigDripleaf leafData = (BigDripleaf) leaf.getBlockData(); // Get the block data of the leaf
//            Vector leafVector = new Vector(); // Initialize a vector to represent the force applied to the current leaf
//
//            // Determine and add the horizontal force component to the leaf vector if horizontal jump power is enabled
//            if (jumpPowerHorizontalMax != 0.0f) {
//                leafVector.add(leafData.getFacing().getDirection().multiply(horizontalComponent)); // Calculate and add the horizontal component based on the leaf's facing direction
//                if (horizontalFlingBack) {
//                    leafVector.multiply(-1); // If fling back is enabled, reverse the direction of the horizontal force
//                }
//            }
//
//            // Apply horizontal stacking multiplier to the leaf vector if the leaf index is greater than or equal to 1 and the multiplier is not 1
//            if (i >= 1 && horizontalStackMultiplier != 1f) {
//                leafVector.multiply(Math.pow(horizontalStackMultiplier, i)); // Multiply the leaf vector by the horizontal stacking multiplier raised to the power of the leaf index
//            }
//
//            // Add the vertical force component to the leaf vector
//            leafVector.add(new Vector(0, verticalComponent, 0)); // Add the determined vertical component to the leaf vector
//
//            // Apply vertical stacking multiplier to the leaf vector if the leaf index is greater than or equal to 1 and the multiplier is not 1
//            if (i >= 1 && verticalStackMultiplier != 1f) {
//                leafVector.setY(leafVector.getY() + Math.pow(verticalStackMultiplier, i)); // Add the vertical stacking multiplier raised to the power of the leaf index to the Y component of the leaf vector
//            }
//
//            // Add the calculated force vector for the current leaf to the overall yeet force vector
//            yeetForce.add(leafVector); // Add the calculated force vector for the current leaf to the overall yeet force
//        }



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
        attachTimerTag(player,timerNSK, coolDown);
        
    }

    private Vector makeYeetForce(List<Block> readyLeaves) {
        // Create the force vector for the yeeting
        Vector yeetForce = new Vector(0, 0, 0); // Initialize the force vector with zero components
        double verticalComponent = randfRange(jumpPowerVerticalMin, jumpPowerVerticalMax); // Determine the random vertical component within the specified range
        double horizontalComponent = randfRange(jumpPowerHorizontalMin, jumpPowerHorizontalMax); // Determine the random horizontal component within the specified range

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
        // Return the resulting yeetForce vector containing the combined forces from all leaves
        return yeetForce;
    }

    // Stuff that should be done per leaf, such as applying force vectors and reseting the leaf.
    private boolean yeet(Player player,Block block) {
        BigDripleaf leafData = (BigDripleaf) block.getBlockData();
        PersistentDataContainer pdt = player.getPersistentDataContainer();

        //set tag here for occupied leaves


        player.sendMessage("yeet"); //DEBUG

        getServer().getScheduler().runTaskLater(this, () -> {

            Vector vector = new Vector();

            // directional
            if (jumpPowerHorizontalMax != 0.0f) {
                vector.add(leafData.getFacing().getDirection().multiply(randfRange(jumpPowerHorizontalMin, jumpPowerHorizontalMax)));
                if (horizontalFlingBack) {
                    vector.multiply(-1);
                }
            }


            // big jump
            vector.add(new Vector(0, randfRange(jumpPowerVerticalMin,jumpPowerVerticalMax), 0));



            player.sendMessage(String.valueOf(vector)); //DEBUG

            player.setVelocity(player.getVelocity().add(vector));

            // reset leaf
            leafData.setTilt(BigDripleaf.Tilt.NONE);
            block.setBlockData(leafData);

            // sound
            if (playSound) {
                player.playSound(player.getLocation(), Sound.ENTITY_SLIME_ATTACK, randfRange(volumeMin, volumeMax), randfRange(pitchMin, pitchMax));
            }

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

    public float randfRange(float min, float max) {
        float v = random.nextFloat();
        return min + v * (max-min);
    }

}

