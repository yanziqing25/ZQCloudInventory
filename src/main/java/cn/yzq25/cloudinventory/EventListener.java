package cn.yzq25.cloudinventory;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerLoginEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.Item;
import cn.yzq25.utils.ZQUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by Yanziqing25
 */
public class EventListener implements Listener {
    private CloudInventoryMain mainclass = CloudInventoryMain.getInstance();

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        Map<Integer, Item> playerCloudInventor = mainclass.getPlayerCloudInventory(player);

        Iterator<Entry<Integer, Item>> iterator = playerCloudInventor.entrySet().iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next().getValue();

            int count = item.getCount();
            int cishu = count % 64 == 0 ? count / 64 : count / 64 + 1;
            for (int i = 1;i <= cishu;i++, count -= 64) {
                if (count <= 64) {
                    player.getInventory().addItem(Item.get(item.getId(), ZQUtils.getItemMeta(item), count));
                    break;
                }
                player.getInventory().addItem(Item.get(item.getId(), ZQUtils.getItemMeta(item), 64));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        mainclass.savePlayerCloudInventory(player, true);
        mainclass.getLogger().info("玩家"+ player.getName() + "退出前数据已上传数据库");
    }
}
