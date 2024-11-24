package mc.jabifx.pet_Manager;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Creature;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Pet implements ConfigurationSerializable {

    private Player owner;
    private Creature pet;
    private BukkitTask followTask;
    private Integer id;
    private boolean equipped;
    private String name;
    private final String type;
    private final String ownerName;

    public Pet(Player owner, String name, String type, Integer id, boolean equipped) {
        this.owner = owner;
        this.ownerName = owner.getUniqueId().toString();
        this.id = id;
        this.equipped = equipped;
        this.name = name;
        this.type = type;
        if (equipped) this.spawnPet(owner);
    }

    public Pet(Map<String, Object> map) {
        this.ownerName = (String) map.get("ownerName");
        this.owner = Bukkit.getPlayerExact(ownerName);
        this.id = (Integer) map.get("id");
        this.equipped = (Boolean) map.get("equipped");
        this.name = (String) map.get("name");
        this.type = (String) map.get("type");
    }

    // Serializar el objeto a un mapa
    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("ownerName", ownerName);
        map.put("id", id);
        map.put("equipped", equipped);
        map.put("name", name);
        map.put("type", type);
        return map;
    }

    public void spawnPet(Player player) {
        if (followTask != null && !followTask.isCancelled()) return;
        equipped = true;
        owner = player;

        EntityType entityType = EntityType.valueOf(type.toUpperCase());

        Location spawnLocation = owner.getLocation().add(2, 0, 0);
        pet = (Creature) owner.getWorld().spawnEntity(spawnLocation, entityType);
        pet.customName(Component.text(name));
        pet.setCustomNameVisible(true);
        pet.setInvulnerable(true);
        pet.setPersistent(true);
        pet.setRemoveWhenFarAway(false);

        this.setSize();
        this.setupAI();
    }

    private void setSize() {
        var currentScale = Objects.requireNonNull(pet.getAttribute(Attribute.GENERIC_SCALE)).getBaseValue();
        if(currentScale == 0.5) return;

        double size = 0.75;
        if(Objects.equals(type, "WITHER") || Objects.equals(type, "HORSE") || Objects.equals(type, "DONKEY")) size = 0.5;

        var scaleAttribute = pet.getAttribute(Attribute.GENERIC_SCALE);
        assert scaleAttribute != null;
        scaleAttribute.setBaseValue(size);
    }

    private void setupAI() {
        if (pet == null) return;

        followTask = Bukkit.getScheduler().runTaskTimer(
                Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Pet_Manager")),
                () -> {
                    if (owner.isOnline()) {
                        Location ownerLocation = owner.getLocation();
                        Location petLocation = pet.getLocation();

                        double distance = ownerLocation.distance(petLocation);

                        if (distance > 10)  pet.teleport(ownerLocation);
                        else if (distance > 3) pet.getPathfinder().moveTo(ownerLocation, 1.1);
                    }
                    else deSpawnPet(true);
                },
                0L, 20L
        );
    }

    public void deSpawnPet(boolean disconnect) {
        if (pet != null) {
            if (!disconnect) equipped = false;
            Location loc = pet.getLocation();
            loc.setY(-100);
            pet.teleport(loc);

            pet.remove();
            pet = null;
        }

        if (followTask != null && !followTask.isCancelled()) {
            followTask.cancel();
            followTask = null;
        }
    }

    public void setName(String name){
        this.name = name;
        if(pet != null) pet.customName(Component.text(name));
    }

    public Integer getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isEquipped() {
        return equipped;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Creature getPet() {
        return pet;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
