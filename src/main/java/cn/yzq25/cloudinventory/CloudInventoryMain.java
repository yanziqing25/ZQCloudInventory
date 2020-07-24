package cn.yzq25.cloudinventory;

import cn.nukkit.Player;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import cn.yzq25.cloudinventory.task.SaveInventoryTask;
import cn.yzq25.extension.ExtensionMain;
import cn.yzq25.extension.MySQLDatabase;
import cn.yzq25.extension.RelationalDatabase;
import cn.yzq25.extension.SQLServerDatabase;
import cn.yzq25.utils.ZQUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Yanziqing25
 */
public class CloudInventoryMain extends PluginBase {
    private static CloudInventoryMain instance;
    public int saveInterval;
    private String mode;
    public RelationalDatabase database;

    public CloudInventoryMain() {
    }

    public static CloudInventoryMain getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        saveDefaultConfig();
        this.saveInterval = (getConfig().getInt("save_interval")) * 20;
    }

    @Override
    public void onEnable() {
        if (getConfig().getBoolean("check_update", true)) {
            ZQUtils.checkPluginUpdate(this);
        }
        switch (this.mode = ExtensionMain.getDatabase().getName()) {
            case "MySQL":
                this.database = (MySQLDatabase) ExtensionMain.getDatabase();
                break;
            case "SQLServer":
                this.database = (SQLServerDatabase) ExtensionMain.getDatabase();
                break;
        }
        createInventorTable();
        getServer().getPluginManager().registerEvents(new EventListener(), this);
        getServer().getScheduler().scheduleRepeatingTask(new SaveInventoryTask(this), this.saveInterval, true);
        getLogger().info(TextFormat.GREEN + "插件加载成功! By:Yanziqing25");
    }

    @Override
    public void onDisable() {
        getLogger().info(TextFormat.RED + "插件已关闭!");
    }

    public Map<Integer, Item> getCloudInventory() {
        ResultSet rs;
        Map<Integer, Item> playerCloudInventor = new HashMap<>();
        switch (this.mode) {
            case "MySQL":
                rs = this.database.executeQuery("SELECT * FROM `inventory`;");
                try {
                    int index = 0;
                    while (rs.next()) {
                        Item item = Item.get(rs.getInt("item_id"), rs.getInt("item_meta"), rs.getInt("item_count"));
                        playerCloudInventor.put(index++, item);
                    }
                    return playerCloudInventor;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return null;
                }
            case "SQLServer":
                return null;
            default:
                return null;
        }
    }

    public Map<Integer, Item> getPlayerCloudInventory(Player player) {
        ResultSet rs;
        Map<Integer, Item> playerCloudInventor = new HashMap<>();
        switch (this.mode) {
            case "MySQL":
                rs = this.database.executeQuery("SELECT `item_id`, `item_meta`, `item_count` FROM `inventory` INNER JOIN `user` ON `inventory`.`user_id` = `user`.`id` AND `user`.`username` = '" + player.getName().toLowerCase() + "';");
                try {
                    int index = 0;
                    while (rs.next()) {
                        Item item = Item.get(rs.getInt("item_id"), rs.getInt("item_meta"), rs.getInt("item_count"));
                        playerCloudInventor.put(index++, item);
                    }
                    return playerCloudInventor;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return null;
                }
            case "SQLServer":
                return null;
            default:
                return null;
        }
    }

    public boolean hasItem(Player player, int id, int meta) {
        ResultSet rs;
        switch (this.mode) {
            case "MySQL":
                rs = this.database.executeQuery("SELECT * FROM `inventory` INNER JOIN `user` ON `inventory`.`user_id` = `user`.`id` AND `user`.`username` = '" + player.getName().toLowerCase() + "' AND `inventory`.`item_id` = " + id + " AND `inventory`.`item_meta` = " + meta + ";");
                try {
                    return rs.next();
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            case "SQLServer":
                return false;
            default:
                return false;
        }
    }

    private synchronized boolean setPlayerCloudInventory(Player player, Set<Item> items) {
        switch (this.mode) {
            case "MySQL":
                items.forEach(item -> {
                    if (!hasItem(player, item.getId(), ZQUtils.getItemMeta(item))) {
                        database.executeSQL("INSERT INTO `inventory` VALUE("+ item.getId() + ", " + ZQUtils.getItemMeta(item) + ", " + item.getCount() + ", (SELECT `id` FROM `user` WHERE `username` = '" + player.getName().toLowerCase() + "'));");
                    }else {
                        database.executeSQL("UPDATE `inventory` SET `item_count` = " + item.getCount() + " WHERE `user_id` = (SELECT `id` FROM `user` WHERE `username` = '" + player.getName().toLowerCase() + "') AND `item_id` = " + item.getId() + " AND `item_meta` = " + ZQUtils.getItemMeta(item) + ";");
                    }
                });
                return true;
            case "SQLServer":
                return false;
            default:
                return false;
        }
    }

    public synchronized boolean removePlayerCloudInventory(Player player) {
        switch (this.mode) {
            case "MySQL":
                return this.database.executeSQL("DELETE FROM `inventory` WHERE `user_id` = (SELECT `id` FROM `user` WHERE `username` = '" + player.getName().toLowerCase() + "');");
            case "SQLServer":
                return this.database.executeSQL("");
            case "local":
                return false;
            default:
                return false;
        }
    }

    public synchronized void savePlayerCloudInventory(Player player, boolean isClearPlayerInventory) {
        removePlayerCloudInventory(player);
        CopyOnWriteArraySet<Item> items = new CopyOnWriteArraySet<>();

        PlayerInventory playerInventory = player.getInventory();
        Iterator<Map.Entry<Integer, Item>> iterator = playerInventory.getContents().entrySet().iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next().getValue();
            if (items.isEmpty()) {
                items.add(item.clone());
            }else {
                Iterator<Item> setIterator = items.iterator();
                while (setIterator.hasNext()) {
                    Item item2 = setIterator.next();
                    if (item2.getId() == item.getId() && ZQUtils.getItemMeta(item2) == ZQUtils.getItemMeta(item)) {
                        item2.setCount(item2.getCount() + item.getCount());
                    }else {
                        items.add(item.clone());
                    }
                }
            }
        }
        if (isClearPlayerInventory) playerInventory.clearAll();
        setPlayerCloudInventory(player, items);
    }

    private boolean createInventorTable() {
        switch (this.mode) {
            case "MySQL":
                return this.database.executeSQL("CREATE TABLE IF NOT EXISTS `inventory` (" +
                        "`item_id` INT UNSIGNED NOT NULL COMMENT '物品ID'," +
                        "`item_meta` TINYINT UNSIGNED NOT NULL COMMENT '物品meta'," +
                        "`item_count` INT UNSIGNED NOT NULL COMMENT '持有物品数量'," +
                        "`user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户编号'," +
                        "PRIMARY KEY ( `item_id`, `item_meta`, `user_id` )," +
                        "CONSTRAINT FOREIGN KEY ( `user_id` ) REFERENCES `user` ( `id` ) ON DELETE CASCADE ON UPDATE CASCADE" +
                        ") ENGINE = INNODB DEFAULT CHARSET = utf8;");
            case "SQLServer":
                return this.database.executeSQL("");
            default:
                return false;
        }
    }
}