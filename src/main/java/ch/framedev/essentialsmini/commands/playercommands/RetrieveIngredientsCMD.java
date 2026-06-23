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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RetrieveIngredientsCMD extends CommandBase {

    private static final String USAGE = "/retrieve [all]";

    public RetrieveIngredientsCMD(Main plugin, String cmdName) {
        super(plugin, cmdName);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getPlugin().getPrefix() + getPlugin().getOnlyPlayer(null));
            return true;
        }

        if (!player.hasPermission(getPlugin().getPermissionBase() + "retrieve")) {
            player.sendMessage(getPlugin().getPrefix() + getPlugin().getNoPerms(player));
            return true;
        }

        if (args.length > 1 || (args.length == 1 && !args[0].equalsIgnoreCase("all"))) {
            player.sendMessage(getPlugin().getPrefix() + getPlugin().getWrongArgs(player, USAGE));
            return true;
        }

        boolean processAll = args.length == 1;

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().isAir()) {
            sendMessage(player, "RetrieveIngredients.NoItem", "&cYou must hold an item.", hand, 0, 0);
            return true;
        }

        ItemStack[] ingredients = getIngredients(hand);

        if (ingredients.length == 0) {
            sendMessage(player, "RetrieveIngredients.NoRecipe", "&cNo ingredients found for &6%Item%&c.", hand, 0, 0);
            return true;
        }

        int processedAmount = processAll ? hand.getAmount() : 1;
        ItemStack[] multipliedIngredients = multiplyIngredients(ingredients, processedAmount);

        removeItemsFromHand(player, processedAmount);

        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(multipliedIngredients);

        int droppedAmount = 0;
        for (ItemStack leftover : leftovers.values()) {
            droppedAmount += leftover.getAmount();
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }

        sendMessage(player, "RetrieveIngredients.Success",
                "&aRetrieved &6%Count% &aingredients from &6%Item%&a.",
                hand, countItems(multipliedIngredients), droppedAmount);

        if (droppedAmount > 0) {
            sendMessage(player, "RetrieveIngredients.InventoryFull",
                    "&eYour inventory was full, so &6%Dropped% &eingredients were dropped.",
                    hand, countItems(multipliedIngredients), droppedAmount);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1 && "all".startsWith(args[0].toLowerCase())) {
            return Collections.singletonList("all");
        }
        return Collections.emptyList();
    }

    private void removeItemsFromHand(Player player, int amount) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() <= amount) {
            player.getInventory().setItemInMainHand(null);
            return;
        }

        hand.setAmount(hand.getAmount() - amount);
    }

    public ItemStack[] getIngredients(ItemStack result) {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();

        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();

            if (recipe.getResult().getType() != result.getType()) {
                continue;
            }

            if (recipe instanceof ShapedRecipe shapedRecipe) {
                return getShapedIngredients(shapedRecipe);
            }

            if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                return getShapelessIngredients(shapelessRecipe);
            }
        }

        return new ItemStack[0];
    }

    private ItemStack[] getShapedIngredients(ShapedRecipe shapedRecipe) {
        List<ItemStack> ingredients = new ArrayList<>();
        Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();

        for (String row : shapedRecipe.getShape()) {
            for (char key : row.toCharArray()) {
                ItemStack ingredient = toItemStack(choiceMap.get(key));

                if (ingredient == null || ingredient.getType().isAir()) {
                    continue;
                }

                ingredients.add(ingredient.clone());
            }
        }

        return ingredients.toArray(new ItemStack[0]);
    }

    private ItemStack[] getShapelessIngredients(ShapelessRecipe shapelessRecipe) {
        List<ItemStack> ingredients = new ArrayList<>();

        for (RecipeChoice choice : shapelessRecipe.getChoiceList()) {
            ItemStack ingredient = toItemStack(choice);
            if (ingredient != null && !ingredient.getType().isAir()) {
                ingredients.add(ingredient);
            }
        }

        return ingredients.toArray(new ItemStack[0]);
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

    private int countItems(ItemStack[] items) {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private ItemStack[] multiplyIngredients(ItemStack[] ingredients, int multiplier) {
        List<ItemStack> multiplied = new ArrayList<>();

        for (int i = 0; i < multiplier; i++) {
            for (ItemStack ingredient : ingredients) {
                addIngredient(multiplied, ingredient);
            }
        }

        return multiplied.toArray(new ItemStack[0]);
    }

    private void addIngredient(List<ItemStack> ingredients, ItemStack ingredient) {
        if (ingredient == null || ingredient.getType().isAir()) {
            return;
        }

        int remaining = ingredient.getAmount();
        int maxStackSize = Math.max(1, ingredient.getMaxStackSize());

        while (remaining > 0) {
            ItemStack target = findSimilarStack(ingredients, ingredient, maxStackSize);
            int amount = Math.min(remaining, maxStackSize);

            if (target == null) {
                ItemStack copy = ingredient.clone();
                copy.setAmount(amount);
                ingredients.add(copy);
                remaining -= amount;
                continue;
            }

            int available = maxStackSize - target.getAmount();
            int moved = Math.min(remaining, available);
            target.setAmount(target.getAmount() + moved);
            remaining -= moved;
        }
    }

    private ItemStack findSimilarStack(List<ItemStack> ingredients, ItemStack ingredient, int maxStackSize) {
        for (ItemStack existing : ingredients) {
            if (existing.getAmount() < maxStackSize && existing.isSimilar(ingredient)) {
                return existing;
            }
        }
        return null;
    }

    private void sendMessage(Player player, String key, String fallback, ItemStack item, int amount, int droppedAmount) {
        String message = getPlugin().getLanguageConfig(player).getString(key, fallback);
        if (message == null) message = fallback;

        message = ReplaceCharConfig.replaceParagraph(message);
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Item%", item.getType().name());
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Amount%", String.valueOf(amount));
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Count%", String.valueOf(amount));
        message = ReplaceCharConfig.replaceObjectWithData(message, "%Dropped%", String.valueOf(droppedAmount));
        player.sendMessage(getPlugin().getPrefix() + message);
    }
}
