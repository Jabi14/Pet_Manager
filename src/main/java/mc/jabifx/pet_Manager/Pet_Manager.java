package mc.jabifx.pet_Manager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Pet_Manager extends JavaPlugin {

    private File playerFile;
    private FileConfiguration playerConfig;

    private ArrayList<Pet> PlayerPets = new ArrayList<>();
    private List<String> AvailablePets = new ArrayList<>();

    static {
        ConfigurationSerialization.registerClass(Pet.class);
    }

    @Override
    public void onEnable() {
        this.createCustomConfig();
        this.AvailablePets = this.getConfig().getStringList("available pets");
        this.PlayerPets = (ArrayList<Pet>) playerConfig.getList("pets");
        if(PlayerPets == null) PlayerPets = new ArrayList<>();
        this.saveDefaultConfig();
        this.getCommand("pets").setExecutor(new Commands(this));
        getServer().getPluginManager().registerEvents(new Events(this), this);
    }

    @Override
    public void onDisable() {
        playerConfig.set("pets", PlayerPets);

        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        saveConfig();
    }

    private void createCustomConfig() {
        playerFile = new File(getDataFolder(), "pets.yml");
        if (!playerFile.exists()) {
            playerFile.getParentFile().mkdirs();
            saveResource("pets.yml", false);
        }

        playerConfig = YamlConfiguration.loadConfiguration(playerFile);
    }

    public Pet getPet(Integer id){
        for(Pet pet: PlayerPets){
            if(pet.getId().equals(id)) return pet;
        }

        return null;
    }

    public void giveAllPets(Player player){
        PlayerPets.removeIf(pet -> {
            if (pet.getOwnerName().equals(String.valueOf(player.getUniqueId()))) {
                pet.deSpawnPet();
                return true;
            }
            return false;
        });

        for(String type: AvailablePets){
            PlayerPets.add(new Pet(player, type, type, PlayerPets.size(), false));
        }
    }

    public void addPet(Player player, String petName, String petType){
        PlayerPets.add(new Pet(player, petName, petType, PlayerPets.size(), false));
    }

    public void EquipPet(Player player, Integer id) {
        int cont = 0;
        for (Pet pet : PlayerPets) {
            String UUID = String.valueOf(player.getUniqueId());
            if (pet.getOwnerName().equals(UUID) && pet.isEquipped()) {
                cont++;
            }
        }

        if (cont >= getConfig().getInt("maximum equipped pets")) {
            player.sendMessage(ChatColor.RED + "Maximum pets equipped reached.");
            return;
        }

        for (Pet pet : PlayerPets) {
            if (pet.getId().equals(id)) {
                if(PlayerPets.contains(pet)) pet.spawnPet(player);
                break;
            }
        }
        player.sendMessage(ChatColor.GREEN + "You have equipped the pet!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    public void UnEquipPet(Integer id) {
        for(Pet pet: PlayerPets){
            if(Objects.equals(pet.getId(), id)){
                pet.deSpawnPet();
            }
        }
    }

    public List<Pet> getPlayerPets(String UUID){
        List<Pet> aux = new ArrayList<>();
        for(Pet pet: PlayerPets){
            if(pet.getOwnerName().equals(UUID)){
                aux.add(pet);
            }
        }

        return aux;
    }

    public void PlayerJoin(Player player) {
        if(!getConfig().getBoolean("spawn pets on join")) return;
        List<Pet> playerPets = getPlayerPets(player.getUniqueId().toString());
        if (playerPets.isEmpty()) return;

        for(Pet pet: playerPets){
            if(pet.isEquipped()) pet.spawnPet(player);
        }
    }

    public ArrayList<Pet> getAllPlayerPets() {
        return PlayerPets;
    }

    public boolean removePet(Integer id) {
        Pet pet = getPet(id);
        if (pet == null) return false;

        pet.deSpawnPet();
        PlayerPets.remove(pet);

        for (int i = 0; i < PlayerPets.size(); i++) {
            PlayerPets.get(i).setId(i);
        }
        return true;
    }

    public void openAvailablePetsGUI(Player player, Player target) {
        int inventorySize = Math.min(((AvailablePets.size() - 1) / 9 + 1) * 9, 54);
        Inventory gui = getServer().createInventory(null, inventorySize, "Add pets to" + target.getName());

        int slot = 0;

        for (String petType : AvailablePets) {
            if (slot >= gui.getSize()) break;
            ItemStack petItem = new ItemStack(Material.PAPER);
            ItemMeta petMeta = petItem.getItemMeta();
            if (petMeta != null) {
                petMeta.setDisplayName(ChatColor.YELLOW + petType);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Type: " + petType);
                lore.add(ChatColor.GRAY + "Left click to give");
                petMeta.setLore(lore);
                petItem.setItemMeta(petMeta);
            }

            gui.setItem(slot, petItem);
            slot++;
        }

        player.openInventory(gui);
    }

    public void openPetGUI(Player player, Player admin) {
        List<Pet> playerPets = getPlayerPets(String.valueOf(player.getUniqueId()));

        int inventorySize = Math.min(((playerPets.size() - 1) / 9 + 1) * 9, 54);
        String name;
        if (admin != null) name = player.getName() + " pets.";
        else name = ChatColor.RED + "Select a pet";
        Inventory gui = getServer().createInventory(null, inventorySize, name);

        if (playerPets.isEmpty()) {
            ItemStack noPetsItem = new ItemStack(Material.BARRIER);
            ItemMeta noPetsMeta = noPetsItem.getItemMeta();
            if (noPetsMeta != null) {
                noPetsMeta.setDisplayName(ChatColor.RED + "No pets available.");
                noPetsItem.setItemMeta(noPetsMeta);
            }
            gui.setItem(inventorySize / 2, noPetsItem); // Coloca el ítem en el centro del inventario
        }
        {
            int slot = 0;

            for (Pet pet : playerPets) {
                if (slot >= gui.getSize()) break;

                ItemStack petItem = new ItemStack(Material.BONE);
                ItemMeta petMeta = petItem.getItemMeta();
                if (petMeta != null) {
                    petMeta.setDisplayName(ChatColor.YELLOW + pet.getName());
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Type: " + pet.getType());
                    lore.add(ChatColor.DARK_GRAY + "ID: " + pet.getId());
                    if (pet.isEquipped()) {
                        lore.add(ChatColor.AQUA + "Status: Equipped");
                    }
                    else {
                        lore.add(ChatColor.RED + "Status: Unequipped");
                    }
                    if (admin != null) {
                        lore.add(ChatColor.RED + "Left click to remove");
                    }
                    else{
                        lore.add(ChatColor.GRAY + "Left click to equip");
                        lore.add(ChatColor.GRAY + "Right click to unequip");
                        lore.add(ChatColor.GRAY + "Shift + left click to change name");
                    }

                    petMeta.setLore(lore);
                    petItem.setItemMeta(petMeta);
                }

                gui.setItem(slot, petItem);
                slot++;
            }
        }

        if (admin != null) {
            ItemStack addItem = new ItemStack(Material.GREEN_WOOL);
            ItemMeta addMeta = addItem.getItemMeta();
            if (addMeta != null) {
                addMeta.setDisplayName("Add pet");
                addItem.setItemMeta(addMeta);
            }
            gui.setItem(gui.getSize() - 2, addItem);

            ItemStack addAllItem = new ItemStack(Material.DIAMOND_BLOCK);
            ItemMeta addAllMeta = addAllItem.getItemMeta();
            if (addAllMeta != null) {
                addAllMeta.setDisplayName("Add all pets");
                addAllItem.setItemMeta(addAllMeta);
            }
            gui.setItem(gui.getSize() - 1, addAllItem);

            admin.openInventory(gui);
        } else {
            player.openInventory(gui);
        }
    }

    public List<String> getAvailablePets() {
        return AvailablePets;
    }
}
