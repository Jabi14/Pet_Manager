package mc.jabifx.pet_Manager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Objects;

public class Events implements Listener {

    private Pet_Manager plugin;

    public Events(Pet_Manager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.PlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getEntity();

            for (Pet pet : plugin.getAllPlayerPets()) {
                if (projectile.getShooter() == pet.getPet()) {
                    event.setCancelled(true);
                    plugin.getLogger().info("Cancelled spawn and sound for projectile from pet: " + pet.getPet().getName());
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getEntity();

            for (Pet pet : plugin.getAllPlayerPets()) {
                if (projectile.getShooter() == pet.getPet()) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }


    @EventHandler
    public void onEntityTargetEvent(EntityTargetEvent event) {
        for (Pet pet : plugin.getAllPlayerPets()) {
            if (pet.getPet() == event.getEntity()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        for (Pet pet : plugin.getAllPlayerPets()) {
            if (pet.getPet() == event.getEntity()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event){
        for (Pet pet: plugin.getAllPlayerPets()){
            if (pet.getPet() == event.getEntity()) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().startsWith("Add pets to")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) return;

            List<String> lore = meta.getLore();
            if (lore == null || lore.isEmpty()) return;

            String petType = null;
            for (String line : lore) {
                if (line.startsWith(ChatColor.GRAY + "Type: ")) {
                    petType = line.substring((ChatColor.GRAY + "Type: ").length());
                    break;
                }
            }

            if (petType != null) {
                String targetName = title.substring("Add pets to ".length() - 1);
                Player target = plugin.getServer().getPlayer(targetName);
                if (!player.hasPermission("pets.add")) return;
                plugin.addPet(target, petType, petType);
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                player.sendMessage(ChatColor.GREEN + "You have added a pet of type " + petType + "!");
            }
        }
        if (title.endsWith(" pets.")) {
            event.setCancelled(true);
            String targetName = title.substring(0, title.length() - 6);
            Player target = plugin.getServer().getPlayer(targetName);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null) return;

            // Manejar acciones en GREEN_WOOL y DIAMOND_BLOCK
            if (clickedItem.getType() == Material.GREEN_WOOL || clickedItem.getType() == Material.DIAMOND_BLOCK) {
                if (clickedItem.getType() == Material.GREEN_WOOL) {
                    assert target != null;
                    plugin.openAvailablePetsGUI(player, target);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }
                else {
                    if (!player.hasPermission("pets.addall")) return;
                    plugin.giveAllPets(target);
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    assert target != null;
                    player.sendMessage(ChatColor.GREEN + "You have given all pets to " + target.getName() + ".");
                }
                return;
            }

            // Detectar clic izquierdo en una mascota
            if (event.getClick() == ClickType.LEFT) {
                List<String> lore = meta.getLore();
                if (lore == null || lore.isEmpty()) return;

                String petId = null;
                for (String line : lore) {
                    if (line.startsWith(ChatColor.DARK_GRAY + "ID: ")) {
                        petId = line.substring((ChatColor.DARK_GRAY + "ID: ").length());
                        break;
                    }
                }

                if (petId != null) {
                    if (!player.hasPermission("pets.remove")) return;
                    plugin.removePet(Integer.valueOf(petId));
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    assert target != null;
                    player.sendMessage(ChatColor.RED + "You have removed pet ID: " + petId + " from " + target.getName() + ".");
                    player.closeInventory();
                }
            }
        }
        else if (title.equals(ChatColor.DARK_RED + "Admin panel")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            ItemMeta meta = clickedItem.getItemMeta();
            if (!(meta instanceof SkullMeta skullMeta)) return;

            String playerName = Objects.requireNonNull(skullMeta.getOwningPlayer()).getName();

            assert playerName != null;
            Player target = plugin.getServer().getPlayer(playerName);
            if (target == null) return;

            plugin.openPetGUI(target, player);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        }
        if (event.getView().getTitle().equals(ChatColor.RED + "Select a pet")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) return;

            ItemMeta meta = clickedItem.getItemMeta();
            if (meta == null || !meta.hasLore()) return;

            List<String> lore = meta.getLore();
            if (lore == null || lore.isEmpty()) return;

            String petName = meta.getDisplayName(); // Get pet name
            String petType = null;
            Integer petId = null;

            for (String line : lore) {
                if (line.startsWith(ChatColor.GRAY + "Type: ")) {
                    petType = line.replace(ChatColor.GRAY + "Type: ", "");
                } else if (line.startsWith(ChatColor.DARK_GRAY + "ID: ")) {
                    try {
                        petId = Integer.parseInt(line.replace(ChatColor.DARK_GRAY + "ID: ", ""));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (petType != null && petId != null) {
                ClickType clickType = event.getClick();

                switch (clickType) {
                    case LEFT:
                        plugin.EquipPet((Player) event.getWhoClicked(), petId);
                        Player equipPlayer = (Player) event.getWhoClicked();
                        equipPlayer.closeInventory();
                        break;

                    case RIGHT:
                        plugin.UnEquipPet(petId);
                        Player unequipPlayer = (Player) event.getWhoClicked();
                        unequipPlayer.sendMessage(ChatColor.RED + "You have unequipped the pet.");
                        unequipPlayer.playSound(unequipPlayer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        unequipPlayer.closeInventory();
                        break;

                    case SHIFT_LEFT:
                        player.closeInventory();
                        player.sendMessage(ChatColor.YELLOW + "Please enter the pet's name in the chat.");
                        Integer finalPetId = petId;
                        plugin.getServer().getPluginManager().registerEvents(new Listener() {
                            @EventHandler
                            public void onPlayerChat(AsyncPlayerChatEvent chatEvent) {
                                Player sender = chatEvent.getPlayer();
                                if (sender.equals(player)) {
                                    chatEvent.setCancelled(true);

                                    String newPetName = chatEvent.getMessage();

                                    Pet pet = plugin.getPet(finalPetId);
                                    if (pet != null) {
                                        pet.setName(newPetName); // Cambiar el nombre de la mascota
                                        player.sendMessage(ChatColor.GREEN + "Pet name changed to: " + newPetName);
                                    }
                                    HandlerList.unregisterAll(this);
                                }
                            }
                        }, plugin);
                        break;

                    default:
                        break;
                }
            }
        }
    }
}
