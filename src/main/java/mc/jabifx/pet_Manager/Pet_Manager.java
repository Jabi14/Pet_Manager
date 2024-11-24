package mc.jabifx.pet_Manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public final class Pet_Manager extends JavaPlugin {

    private File playerFile;
    private FileConfiguration playerConfig;

    private ArrayList<Pet> PlayerPets = new ArrayList<>();
    private List<String> AvailablePets = new ArrayList<>();
    private Team noCollision;

    static {
        ConfigurationSerialization.registerClass(Pet.class);
    }

    @Override
    public void onEnable() {
        this.createCustomConfig();
        this.AvailablePets = this.getConfig().getStringList("available pets");
        this.PlayerPets = (ArrayList<Pet>) playerConfig.getList("pets");
        if (PlayerPets == null) PlayerPets = new ArrayList<>();
        this.saveDefaultConfig();
        this.getCommand("pets").setExecutor(new Commands(this));
        getServer().getPluginManager().registerEvents(new Events(this), this);
        setupNoCollisionTeam();
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

    private void setupNoCollisionTeam() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getMainScoreboard();

        this.noCollision = scoreboard.getTeam("noCollision");
        if (noCollision == null) noCollision = scoreboard.registerNewTeam("noCollision");

        noCollision.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        noCollision.setDisplayName("NoCollisionTeam");
    }

    public Pet getPet(Integer id){
        for(Pet pet: PlayerPets) if(pet.getId().equals(id)) return pet;
        return null;
    }

    public void giveAllPets(Player player){
        PlayerPets.removeIf(pet -> {
            if (pet.getOwnerName().equals(String.valueOf(player.getUniqueId()))) {
                pet.deSpawnPet(false);
                return true;
            }
            return false;
        });

        for(String type: AvailablePets) PlayerPets.add(new Pet(player, type, type, PlayerPets.size(), false));
    }

    public void addPet(Player player, String petName, String petType){
        PlayerPets.add(new Pet(player, petName, petType, PlayerPets.size(), false));
    }

    public void EquipPet(Player player, Integer id) {
        int cont = 0;
        for (Pet pet : PlayerPets) {
            String UUID = String.valueOf(player.getUniqueId());
            if (pet.getOwnerName().equals(UUID) && pet.isEquipped()) cont++;
        }

        if (cont >= getConfig().getInt("maximum equipped pets")) {
            player.sendMessage(ChatColor.RED + "Maximum pets equipped reached.");
            return;
        }

        for (Pet pet : PlayerPets) {
            if (pet.getId().equals(id)) {
                if(PlayerPets.contains(pet)) {
                    pet.spawnPet(player);
                    addNoCollision(pet.getPet());
                }
                break;
            }
        }

        player.sendMessage(ChatColor.GREEN + "You have equipped the pet!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    public void UnEquipPet(Integer id) {
        for(Pet pet: PlayerPets){
            if(Objects.equals(pet.getId(), id)) pet.deSpawnPet(false);
        }
    }

    public List<Pet> getPlayerPets(String UUID){
        List<Pet> aux = new ArrayList<>();
        for(Pet pet: PlayerPets){
            if(pet.getOwnerName().equals(UUID)) aux.add(pet);
        }
        return aux;
    }

    public void PlayerJoin(Player player) {
        if(!getConfig().getBoolean("spawn pets on join")) return;
        List<Pet> playerPets = getPlayerPets(player.getUniqueId().toString());
        if (playerPets.isEmpty()) return;

        for(Pet pet: playerPets){
            if(pet.isEquipped()){
                pet.spawnPet(player);
                addNoCollision(pet.getPet());
            }
        }
    }

    public ArrayList<Pet> getAllPlayerPets() {
        return PlayerPets;
    }

    public boolean removePet(Integer id) {
        Pet pet = getPet(id);
        if (pet == null) return false;

        pet.deSpawnPet(false);
        PlayerPets.remove(pet);

        for (int i = 0; i < PlayerPets.size(); i++) PlayerPets.get(i).setId(i);
        return true;
    }

    public void openAvailablePetsGUI(Player player, Player target) {
        int inventorySize = Math.min(((AvailablePets.size() - 1) / 9 + 1) * 9, 54);
        Inventory gui = getServer().createInventory(null, inventorySize, "Add pets to" + target.getName());

        int slot = 0;

        for (String petType : AvailablePets) {
            if (slot >= gui.getSize()) break;
            ItemStack petItem = null;
            try {
                petItem = createHead(petType);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            ItemMeta petMeta = petItem.getItemMeta();
            if (petMeta == null) continue;

            petMeta.setDisplayName(ChatColor.YELLOW + petType);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Type: " + petType);
            lore.add(ChatColor.GRAY + "Left click to give");
            petMeta.setLore(lore);
            petItem.setItemMeta(petMeta);

            gui.setItem(slot, petItem);
            slot++;
        }

        player.openInventory(gui);
    }

    public ItemStack createHead(String type) throws MalformedURLException {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        switch (type.toLowerCase()) {
            case "wolf" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/a70611e0734caaa9d6b915d515f4aa5aa88032e902dab4d6a231f05d4fceade7"));
            case "cat" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/9d08749c1b1a64660638f50c82227c2acc84995f6afe38d7d8d2969fffabd486"));
            case "horse" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/712c5ef8363fec905bcea2779bfa4cd6d99676b0cb1dd5e7d9f6f0068729842f"));
            case "rabbit" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/f85f60252fcc179f0536a3fd3b35614864c1b4cd20a2247c1b57a0af3d5e13b1"));
            case "wither" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/c743123a19e8093b465b84838e28f7d64af079c58b7cf3dac07c8c592a64fc32"));
            case "donkey" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/7201245fcdc6cc42694d8e67e566282e07bc00a0a397fc726609c2789fd9e6b1"));
            case "guardian" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/3ac17d7d5fd2ceaf993f4cdccf40b10429aa218e1dda3b724ccfbb70fdfd56af"));
            case "allay" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/df5de940bfe499c59ee8dac9f9c3919e7535eff3a9acb16f4842bf290f4c679f"));
            case "vex" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/b663134d7306bb604175d2575d686714b04412fe501143611fcf3cc19bd70abe"));
            case "parrot" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/7731b9b48f06e013044e7a2e81ea5e9b0444aebaa9835f4df9811bc357cdf02d"));
            case "enderman" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/4f24767c8138b3dfec02f77bd151994d480d4e869664ce09a26b19289212162b"));
            case "add" -> textures.setSkin(new URL("http://textures.minecraft.net/texture/f5c140e08429309dd38f1ec7ed3d1765556e5476dc6c3fbf53e2cb2d1b15"));
            default -> textures.setSkin(new URL("http://textures.minecraft.net/texture/c160b06d73293cbef849c2edf9a760c6004040e47e074da83d9414277599ba83"));
        }

        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);

        return head;
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
            gui.setItem(inventorySize / 2, noPetsItem);
        }
        else {
            int slot = 0;

            for (Pet pet : playerPets) {
                if (slot >= gui.getSize()) break;

                ItemStack petItem = null;
                try {
                    petItem = createHead(pet.getType());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                ItemMeta petMeta = petItem.getItemMeta();
                if (petMeta == null) continue;

                petMeta.setDisplayName(ChatColor.YELLOW + pet.getName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Type: " + pet.getType());
                lore.add(ChatColor.DARK_GRAY + "ID: " + pet.getId());
                if (pet.isEquipped()) lore.add(ChatColor.AQUA + "Status: Equipped");
                else lore.add(ChatColor.RED + "Status: Unequipped");
                if (admin != null) lore.add(ChatColor.RED + "Left click to remove");
                else {
                    lore.add(ChatColor.GRAY + "Left click to equip");
                    lore.add(ChatColor.GRAY + "Right click to unequip");
                    lore.add(ChatColor.GRAY + "Shift + left click to change name");
                }

                petMeta.setLore(lore);
                petItem.setItemMeta(petMeta);

                gui.setItem(slot, petItem);
                slot++;
            }
        }

        if (admin != null) {
            ItemStack addItem = null;
            try {
                addItem = createHead("Add");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            ItemMeta addMeta = addItem.getItemMeta();
            if (addMeta != null) {
                addMeta.setDisplayName("Add pet");
                addItem.setItemMeta(addMeta);
            }
            gui.setItem(gui.getSize() - 2, addItem);

            ItemStack addAllItem = null;
            try {
                addAllItem = createHead("AddAll");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            ItemMeta addAllMeta = addAllItem.getItemMeta();
            if (addAllMeta != null) {
                addAllMeta.setDisplayName("Add all pets");
                addAllItem.setItemMeta(addAllMeta);
            }
            gui.setItem(gui.getSize() - 1, addAllItem);

            admin.openInventory(gui);
        }
        else player.openInventory(gui);
    }

    public List<String> getAvailablePets() {
        return AvailablePets;
    }

    public void addNoCollision(Creature pet) {
        noCollision.addEntry(pet.getUniqueId().toString());
    }
}
