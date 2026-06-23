package ch.framedev.essentialsmini.commands.playercommands;

import ch.framedev.essentialsmini.abstracts.CommandBase;
import ch.framedev.essentialsmini.main.Main;
import ch.framedev.essentialsmini.utils.ReplaceCharConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShowRecipeCMD extends CommandBase {

    private static final String USAGE = "/showrecipe [item]";

    private final Main plugin;

    public ShowRecipeCMD(Main plugin) {
        super(plugin, "showrecipe");
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + plugin.getOnlyPlayer(null));
            return true;
        }

        if (!player.hasPermission(plugin.getPermissionBase() + "showrecipe")) {
            player.sendMessage(plugin.getPrefix() + plugin.getNoPerms(player));
            return true;
        }

        if (args.length > 1) {
            player.sendMessage(plugin.getPrefix() + plugin.getWrongArgs(player, USAGE));
            return true;
        }

        ItemStack item = resolveItem(player, args);
        if (item == null) {
            return true;
        }

        Recipe recipe = findRecipe(item);
        if (recipe == null) {
            sendMessage(player, "ShowRecipe.NoRecipe", "&cNo recipe found for &6%Item%&c.", item.getType().name(), "", "");
            return true;
        }

        Map<String, Integer> ingredients = getIngredients(recipe);
        if (ingredients.isEmpty()) {
            sendMessage(player, "ShowRecipe.NoRecipe", "&cNo recipe found for &6%Item%&c.", item.getType().name(), "", "");
            return true;
        }

        sendMessage(player, "ShowRecipe.Header", "&aRecipe for &6%Item%&a:", recipe.getResult().getType().name(), "", "");
        for (Map.Entry<String, Integer> ingredient : ingredients.entrySet()) {
            sendMessage(player, "ShowRecipe.Ingredient", "&7- &6%Amount%x &f%Ingredient%",
                    recipe.getResult().getType().name(), ingredient.getKey(), String.valueOf(ingredient.getValue()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> materials = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && material.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                materials.add(material.name().toLowerCase(Locale.ROOT));
            }
        }
        Collections.sort(materials);
        return materials;
    }

    private ItemStack resolveItem(Player player, String[] args) {
        if (args.length == 0) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType().isAir()) {
                sendMessage(player, "ShowRecipe.NoItem", "&cHold an item or use &6/showrecipe <item>&c.", "", "", "");
                return null;
            }
            return hand;
        }

        Material material = Material.matchMaterial(args[0]);
        if (material == null || !material.isItem()) {
            sendMessage(player, "ShowRecipe.UnknownItem", "&cThis item does not exist: &6%Item%&c.", args[0], "", "");
            return null;
        }

        return new ItemStack(material);
    }

    private Recipe findRecipe(ItemStack item) {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe.getResult().getType() == item.getType()) {
                return recipe;
            }
        }
        return null;
    }

    private Map<String, Integer> getIngredients(Recipe recipe) {
        Map<String, Integer> ingredients = new LinkedHashMap<>();

        if (recipe instanceof ShapedRecipe shapedRecipe) {
            Map<Character, RecipeChoice> choices = shapedRecipe.getChoiceMap();
            for (String row : shapedRecipe.getShape()) {
                for (char key : row.toCharArray()) {
                    addIngredient(ingredients, choices.get(key));
                }
            }
        }

        if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            for (RecipeChoice choice : shapelessRecipe.getChoiceList()) {
                addIngredient(ingredients, choice);
            }
        }

        return ingredients;
    }

    private void addIngredient(Map<String, Integer> ingredients, RecipeChoice choice) {
        ItemStack item = toItemStack(choice);
        if (item == null || item.getType().isAir()) {
            return;
        }

        ingredients.merge(item.getType().name(), Math.max(1, item.getAmount()), Integer::sum);
    }

    private ItemStack toItemStack(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.ExactChoice exactChoice && !exactChoice.getChoices().isEmpty()) {
            return exactChoice.getChoices().get(0).clone();
        }

        if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
            for (Material material : materialChoice.getChoices()) {
                if (!material.isAir()) {
                    return new ItemStack(material);
                }
            }
        }

        return null;
    }

    private void sendMessage(Player player, String key, String fallback, String item, String ingredient, String amount) {
        String message = plugin.getLanguageConfig(player).getString(key, fallback);
        if (message == null) message = fallback;

        message = ReplaceCharConfig.replaceParagraph(message);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", item);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Ingredient%", ingredient);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", amount);
        player.sendMessage(plugin.getPrefix() + message);
    }
}
