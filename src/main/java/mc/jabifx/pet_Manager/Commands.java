package mc.jabifx.pet_Manager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;


public class Commands implements CommandExecutor {

    private Pet_Manager plugin;

    public Commands(Pet_Manager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 0) {
                if (player.hasPermission("pets.user")) plugin.openPetGUI(player, null);
            }
            else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("admin")) {
                    if (player.hasPermission("pets.admin")) openAdminGUI(player);
                }
            }
            else if (args.length == 2) {
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("get")) {
                    if (!player.hasPermission("pets.get")) return true;
                    List<Pet> Pets = plugin.getPlayerPets(String.valueOf(target.getUniqueId()));
                    if (Pets.isEmpty()) {
                        sender.sendMessage(ChatColor.RED + "This player has no pets.");
                        return true;
                    }
                    StringBuilder message = new StringBuilder("Pets of " + target.getName() + ":");
                    for (Pet pet : Pets) message.append("\n- Type: ").append(pet.getType()).append(", ID: ").append(pet.getId());
                    sender.sendMessage(ChatColor.YELLOW + message.toString());
                }
                else if (args[0].equalsIgnoreCase("addall")) {
                    if (!player.hasPermission("pets.addall")) return true;
                    plugin.giveAllPets(target);
                    player.sendMessage(ChatColor.GREEN + "You have given all pets to " + target.getName() + ".");
                }
            }
            else if (args.length == 3) {
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                if (args[0].equalsIgnoreCase("add")) {
                    if (!player.hasPermission("pets.add")) return true;
                    List<String> AllTypes = plugin.getAvailablePets();
                    String type = args[2];
                    if (!AllTypes.contains(type.toUpperCase())) {
                        player.sendMessage(ChatColor.RED + "Pet type not found.");
                        return true;
                    }
                    plugin.addPet(target, type.toUpperCase(), type.toUpperCase());
                    player.sendMessage(ChatColor.GREEN + "Pet successfully added to " + target.getName() + ".");
                }
                else if (args[0].equalsIgnoreCase("remove")) {
                    try {
                        if (!player.hasPermission("pets.remove")) return true;
                        Integer id = Integer.valueOf(args[2]);
                        if (plugin.removePet(id)) player.sendMessage(ChatColor.GREEN + "Pet successfully removed.");
                        else player.sendMessage(ChatColor.RED + "No pet found with the specified ID.");
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid pet ID. Please enter a valid number.");
                    }
                }
            }
        }
        return true;
    }

    private void openAdminGUI(Player player) {
        Inventory gui = plugin.getServer().createInventory(null, 54, ChatColor.DARK_RED + "Admin panel");
        int slot = 0;
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            ItemStack playerSkull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) playerSkull.getItemMeta();

            if (skullMeta == null) continue;

            skullMeta.setDisplayName(ChatColor.YELLOW + onlinePlayer.getName());
            skullMeta.setOwningPlayer(onlinePlayer);
            playerSkull.setItemMeta(skullMeta);
            gui.setItem(slot, playerSkull);
            slot++;
        }

        player.openInventory(gui);
    }

}