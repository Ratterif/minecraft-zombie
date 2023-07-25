package Liss;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Zombie;
import org.bukkit.Particle;
import org.bukkit.entity.ExperienceOrb;
	
public final class Zombies extends JavaPlugin implements Listener {
	int taskId = -1;
	NamespacedKey keyBreed = new NamespacedKey(this, "breed");

	@Override
    public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);   	
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
	@EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (event.getRightClicked().getType() == EntityType.ZOMBIE) {
            Mob zombie = (Mob) event.getRightClicked();
            EquipmentSlot slot = event.getHand();
            ItemStack item = player.getInventory().getItem(slot);
            if(
            		item.getType().equals(Material.COOKIE)
            		&& zombie.getPersistentDataContainer().get(keyBreed, PersistentDataType.STRING) == null
            ) {
            	if(item.getAmount() == 1) player.getInventory().setItem(slot, new ItemStack(Material.AIR));
            	else {
                	ItemStack newItem = item.clone();
                	newItem.setAmount(item.getAmount() - 1);
            		player.getInventory().setItem(slot, newItem);
            	}
            	Mob zombie2 = findAnotherZombie(zombie);
            	if(zombie2 == null)
            		zombie.getPersistentDataContainer().set(keyBreed, PersistentDataType.STRING, "FEED");
            	else {
                	zombie.getPersistentDataContainer().set(keyBreed, PersistentDataType.STRING, "BREEDING");
                	zombie2.getPersistentDataContainer().set(keyBreed, PersistentDataType.STRING, "BREEDING");
                	int intervalTicks = 20;
					taskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
                        double thresholdDistance = 2;
                        double distance = zombie.getLocation().distance(zombie2.getLocation());
                        if(zombie.isDead() || zombie2.isDead()) {
                            zombie.setTarget(player);
                            zombie2.setTarget(player);
                            zombie.getPersistentDataContainer().set(keyBreed, PersistentDataType.STRING, "FEED");
                            zombie2.getPersistentDataContainer().set(keyBreed, PersistentDataType.STRING, "FEED");
                            getServer().getScheduler().cancelTask(taskId); // Остановка задачи
                        }

                        if (distance <= thresholdDistance) {
                            ExperienceOrb experienceOrb = zombie.getLocation().getWorld().spawn(zombie.getLocation(), ExperienceOrb.class);
                            experienceOrb.setExperience(10);
                            Zombie babyZombie = (Zombie) zombie2.getLocation().getWorld().spawnEntity(zombie2.getLocation(), EntityType.ZOMBIE);
                            babyZombie.setBaby();
                            zombie.setTarget(player);
                            zombie2.setTarget(player);
                            zombie.getPersistentDataContainer().remove(keyBreed);
                            zombie2.getPersistentDataContainer().remove(keyBreed);
                            getServer().getScheduler().cancelTask(taskId); // Остановка задачи
                        } else {
                        	zombie.getLocation().getWorld().spawnParticle(Particle.HEART, zombie.getLocation(), 5);
                        	zombie2.getLocation().getWorld().spawnParticle(Particle.HEART, zombie2.getLocation(), 5);
                            zombie.setTarget(zombie2);
                            zombie2.setTarget(zombie);
                        }
                    }, 0L, intervalTicks);
            		
            	}
            	
            	player.sendMessage("It was tasty");
            	
            }        	
        }		
	}
    private Mob findAnotherZombie(Mob zombie) {
        for (Entity entity : zombie.getNearbyEntities(10, 10, 10)) {
        	String breed = entity.getPersistentDataContainer().get(keyBreed, PersistentDataType.STRING); 
            if (entity.getType() == EntityType.ZOMBIE && entity != zombie && breed != null && breed.equals("FEED")) {
                return (Mob) entity;
            }
        }
        return null;
    }
    @EventHandler
    public void onEntityHitedByEntity(EntityDamageByEntityEvent event) {
    	Entity damager = event.getDamager();
    	if(damager.getType() == EntityType.ZOMBIE && damager.getPersistentDataContainer().get(keyBreed, PersistentDataType.STRING) != null) {
    		event.setCancelled(true);    		
    	}    	
    }
}
