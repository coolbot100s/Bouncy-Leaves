package cool.bot.bouncyleaves;

// Imports
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.BigDripleaf;
import org.bukkit.configuration.file.FileConfiguration;
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
import java.util.Random;

// Main Class
public final class BouncyLeaves extends JavaPlugin implements Listener {

    // Vars
    final NamespacedKey timerNSK = new NamespacedKey(this, "yeetTimer");
    Random random = new Random();

    // Settings
    FileConfiguration config = this.getConfig();

    public boolean playSound = config.getBoolean("playSoundOnBounce");
    public float volumeMin = (float) config.getDouble("minimumVolume");
    public float volumeMax = (float) config.getDouble("maximumVolume");
    public float pitchMin = (float) config.getDouble("minimumPitch");
    public float pitchMax = (float) config.getDouble("maximumPitch");
    public boolean logicalCollisions = config.getBoolean("logicalCollisions");
    public int tiltLevel = config.getInt("tiltLevel");
    public int coolDown = config.getInt("bounceCooldown");
    public int yeetDelay = config.getInt("bounceDelay");
    public float jumpPowerVerticalMin = (float) config.getDouble("minimumVerticalJumpPower");
    public float jumpPowerVerticalMax = (float) config.getDouble("maximumVerticalJumpPower");
    public float jumpPowerHorizontalMin = (float) config.getDouble("minimumHorizontalJumpPower");
    public float jumpPowerHorizontalMax = (float) config.getDouble("maximumHorizontalJumpPower");
    public boolean horizontalFlingBack = config.getBoolean("horizontalFlingBack");
    public float verticalStackMultiplier = (float) config.getDouble("verticalStackMultiplier");
    public float horizontalStackMultiplier = (float) config.getDouble("horizontalStackMultiplier");
    public boolean noYeetWhenSneaking = config.getBoolean("disableBounceWhenSneaking");
    public boolean allowMultiBounce = config.getBoolean("allowMultiBounce");


    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);
        this.saveDefaultConfig();
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
                return;
            }
        }

        // Ignore event if player is sneaking
        if (noYeetWhenSneaking && player.isSneaking()) {
            return;
        }

        // check to see if the player is standing in air, or a big leaf, if not ignore the event
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

        Vector yeetForce = makeYeetForce(readyLeaves, player);

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

    // Create the force vector for the yeeting
    private Vector makeYeetForce(List<Block> readyLeaves, Player player) {
        Vector yeetForce = new Vector(0, 0, 0);
        // Determine RNG
        double verticalComponent = randfRange(jumpPowerVerticalMin, jumpPowerVerticalMax);
        double horizontalComponent = randfRange(jumpPowerHorizontalMin, jumpPowerHorizontalMax);

        // multiplier per cardinal
        float nCount = 0;
        float sCount = 0;
        float eCount = 0;
        float wCount = 0;
        int i = 0;

        // per block vectors
        for (Block leaf : readyLeaves) {
            BigDripleaf leafData = (BigDripleaf) leaf.getBlockData();

            // Horizontal movement
            if (horizontalComponent != 0.0f) {
                double hpow = horizontalComponent;
                BlockFace facing = leafData.getFacing();
                Vector horVec = facing.getDirection();

                // Horizontal stacking
                if (horizontalStackMultiplier != 1) {
                    switch (facing) {
                        case NORTH:
                            hpow = hpow * Math.pow(horizontalComponent, nCount);
                            nCount++;
                            break;
                        case SOUTH:
                            hpow = hpow * Math.pow(horizontalComponent, sCount);
                            sCount++;
                            break;
                        case EAST:
                            hpow = hpow * Math.pow(horizontalComponent, eCount);
                            eCount++;
                            break;
                        case WEST:
                            hpow = hpow * Math.pow(horizontalComponent, wCount);
                            wCount++;
                            break;
                        default:
                            break;
                    }
                }

                horVec.multiply(hpow);
                if (horizontalFlingBack) {
                    horVec.multiply(-1);
                }
                yeetForce.add(horVec);
            }

            // Vertical
            Double ypow = verticalComponent;
            // Vertical Multiplier
            if (verticalStackMultiplier != 1 && i >= 1) {
                ypow = ypow * Math.pow(verticalStackMultiplier, i);
            }

            // End of Loop
            yeetForce.add(new Vector(0,ypow,0));
            i++;
        }

        if (!(allowMultiBounce)) {
            // Cap vertical power
            if (yeetForce.getY() > verticalComponent) {
                yeetForce.setY(verticalComponent);
            }

            // Cap horizontal power
            if (Math.abs(yeetForce.getX()) > horizontalComponent) {
                yeetForce.setX(Math.signum(yeetForce.getX()) * horizontalComponent);
            }
            if (Math.abs(yeetForce.getZ()) > horizontalComponent) {
                yeetForce.setZ(Math.signum(yeetForce.getZ()) * horizontalComponent);
            }
        }

        return yeetForce;
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

    // Attach a timer to the player
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

    // Return a random float from a range
    public float randfRange(float min, float max) {
        float v = random.nextFloat();
        return min + v * (max-min);
    }
}
