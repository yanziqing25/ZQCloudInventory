package cn.yzq25.cloudinventory.task;

import cn.nukkit.Player;
import cn.nukkit.scheduler.PluginTask;
import cn.yzq25.cloudinventory.CloudInventoryMain;

import java.util.*;
import java.util.Map.Entry;

public class SaveInventoryTask extends PluginTask<CloudInventoryMain> {

    public SaveInventoryTask(CloudInventoryMain plugin) {
        super(plugin);
    }

    @Override
    public void onRun(int currentTick) {
        Map<UUID, Player> players = getOwner().getServer().getOnlinePlayers();
        Iterator<Entry<UUID, Player>> iterator = players.entrySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next().getValue();
            getOwner().savePlayerCloudInventory(player, false);
            getOwner().getLogger().info("玩家" + player.getName() + "的数据已上传数据库!");
        }
    }
}