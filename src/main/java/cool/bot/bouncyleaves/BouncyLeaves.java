package cool.bot.bouncyleaves;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.BigDripleaf;
import org.bukkit.block.data.type.Dripleaf;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


import java.util.ArrayList;
import java.util.List;

import static org.bukkit.Bukkit.getPluginManager;


public final class BouncyLeaves extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location location = player.getLocation();
        Block block = location.getBlock();
        BoundingBox playerBox = player.getBoundingBox();
        List<Block> readyLeaves = new ArrayList<>();

        // get the block the player is in, and all adjacent blocks, if any are a dripleaf continue
        // check if the player is inside a dripleaf, or air, if so continue
        // check if the player's hitbox, at their feet, is colliding with any dripleafs that are at full tilt
        // if so, execute the jump code below



        // populate list with leaves at full tilt from a 3x3 area around the player
//        for (Block curBlock : getBlocksInArea(location.clone().add(-1,0,-1),location.clone().add(1,0,1))) {
//            if (curBlock.getType() == Material.BIG_DRIPLEAF) {
//                BigDripleaf leaf = (BigDripleaf) block.getBlockData();
//                if (leaf.getTilt() == BigDripleaf.Tilt.FULL) {
//                    readyLeaves.add(leaf);
//                }
//            }
//        }

        // ignore event if player was just flung in the last tick or so

        // populate list with leaves at fulltilt that the players feet are colliding with
        //player.sendMessage(String.valueOf(location.getY())); //DEBUG
        List<Block> blocksUnderFeet = getBlocksInArea(new Location(location.getWorld(),Math.floor(playerBox.getMinX()),location.getY(),Math.floor(playerBox.getMinZ())),new Location(location.getWorld(),Math.floor(playerBox.getMaxX()),location.getY(),Math.floor(playerBox.getMaxZ())));


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

       // player.sendMessage(String.valueOf(readyLeaves.size())); //DEBUG

        // check to see if the player is standing in air, or a big leaf, if not ignore the event.
        if (!(block.getType() == Material.BIG_DRIPLEAF || block.getType() == Material.AIR)) {
            return;
        }

        // Yeet the player for each leaf
        for (Block leaf : readyLeaves) {
            BigDripleaf leafData = (BigDripleaf) leaf.getBlockData();
            yeet(player, leaf, leafData);
        }
    }

    private void yeet(Player player,Block block,BigDripleaf leafData) {
        getServer().getScheduler().runTaskLater(this, () -> {
            // big jump
            Vector vector = new Vector(0, 1.4, 0);

            // directional
            vector.add(leafData.getFacing().getDirection().multiply(-1));

            player.setVelocity(player.getVelocity().add(vector));

            // reset leaf
            leafData.setTilt(BigDripleaf.Tilt.NONE);
            block.setBlockData(leafData);

            // sound?
            player.playSound(player.getLocation(), Sound.ENTITY_SLIME_ATTACK, 2.0f, 1.0f);

        }, 1);
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


}

