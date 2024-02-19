package cool.bot.bouncyleaves;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.BigDripleaf;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;


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
        event.getPlayer().sendMessage(event.getPlayer().getLocation().getBlock().getType().toString());
        if (event.getPlayer().getLocation().getBlock().getType() == Material.BIG_DRIPLEAF) {
            Player player = event.getPlayer();
            Location location = player.getLocation();
            Block block = location.getBlock();
            BigDripleaf leafData = (BigDripleaf) block.getBlockData();

            if (leafData.getTilt() == BigDripleaf.Tilt.FULL) {

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
        }
    }
}
