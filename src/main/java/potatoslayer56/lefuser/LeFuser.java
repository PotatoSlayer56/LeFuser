package potatoslayer56.lefuser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Time;
import java.util.*;

public class LeFuser implements ModInitializer {
	public static final String MOD_ID = "lefuser";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Map<String, Shard> shardsDirectory = new HashMap<>();
	public static List<ShardRecipe> shardRecipes = new ArrayList<>();

	public static JsonObject bazaarData;
	public static JsonObject bazaarProducts;

	public static JsonObject shardData;
	public static JsonObject shardRecipeData;

	private static final ShardProfit dummyProfit = new ShardProfit("", "", "", 0f);

	public static List<ShardProfit> shardProfits = new ArrayList<>();
	public static List<ShardProfit> topFusions = new ArrayList<>(Arrays.asList(dummyProfit, dummyProfit, dummyProfit, dummyProfit, dummyProfit));

	@Override
	public void onInitialize() {
		try {
			bazaarData = JsonParser.parseString(getBazaarData()).getAsJsonObject();
			bazaarProducts = bazaarData.get("products").getAsJsonObject();
		}
		catch (Exception e) {
			LOGGER.error(e.toString());
			LOGGER.info("onInitialize");
		}

		shardData = loadShardData();
		registerShards();

		shardRecipeData = loadShardRecipes();
		registerShardRecipes();

		calculateShardProfit();

		ClientCommandRegistrationCallback.EVENT.register(((commandDispatcher, commandBuildContext) -> {
			commandDispatcher.register(ClientCommands.literal("getbestfusions").executes(commandContext -> {
				getTop5ShardProfit();
				return 0;
			}));
		}));
		ClientCommandRegistrationCallback.EVENT.register(((commandDispatcher, commandBuildContext) -> {
			commandDispatcher.register(ClientCommands.literal("calculatebestfusions").executes(commandContext -> {
				calculateShardProfit();
				clientPrefixedMessage("Recalculated best fusions!");
				return 0;
			}));
		}));
	}

	public static void getTop5ShardProfit() {
		topFusions.forEach(shardProfit -> {
			clientPrefixedMessage("§a%s §f+ §a%s §f-> §a%s §f| Profit: §6%,.2f".formatted(shardProfit.ingredient1, shardProfit.ingredient2, shardProfit.result, shardProfit.profit));
		});
	}

	public static float reptilianBonus(Shard ing1, Shard ing2) {
		if (ing1.isReptile || ing2.isReptile) return 1.2f;
		return 1f;
	}

	public static void calculateShardProfit() {
		topFusions = new ArrayList<>(Arrays.asList(dummyProfit, dummyProfit, dummyProfit, dummyProfit, dummyProfit));
		long startTime = System.currentTimeMillis();

		shardRecipes.forEach(shardRecipe -> {
			Shard ing1 = shardsDirectory.get(shardRecipe.ingredient1);
			Shard ing2 = shardsDirectory.get(shardRecipe.ingredient2);
			Shard result = shardsDirectory.get(shardRecipe.result);
			float profit = (shardRecipe.amount * reptilianBonus(ing1, ing2) * result.sellOrderPrice) - (ing1.fusionAmount * ing1.buyOrderPrice) - (ing2.fusionAmount * ing2.buyOrderPrice);
			ShardProfit shardProfit = new ShardProfit(ing1.id, ing2.id, result.id, profit);
			shardProfits.add(shardProfit);
			for (int i = 0; i < topFusions.size(); i++){
				if (profit > topFusions.get(i).profit && (!Objects.equals(ing1.id, "L49") || !Objects.equals(ing2.id, "L49"))){
					topFusions.set(i, shardProfits.getLast());
					break;
				}
			}
		});

		long endTime = System.currentTimeMillis();
		LOGGER.info("Calculated shard recipe profits, took %,dms".formatted(endTime-startTime));
	}

	public static void registerShards() {
		shardData.getAsJsonObject("shards").asMap().forEach((key, data) -> {
			JsonObject jsonObject = data.getAsJsonObject();
			registerShard(
				key,
				jsonObject.get("name").getAsString(),
				jsonObject.get("internal_id").getAsString(),
				getBuyOrder(jsonObject.get("internal_id").getAsString()),
				getSellOrder(jsonObject.get("internal_id").getAsString()),
				jsonObject.get("family").getAsString().contains("Reptile"),
				jsonObject.get("fuse_amount").getAsInt()
			);
		});
	}

	public static void registerShard(String id, String name, String bazaarID, float buyOrderPrice, float sellOrderPrice, boolean isReptile, int fusionAmount) {
		shardsDirectory.put(id, new Shard(id, name, bazaarID, buyOrderPrice, sellOrderPrice, isReptile, fusionAmount));
	}

	public static void registerShardRecipes() {
		shardRecipeData.getAsJsonObject("recipes").asMap().forEach((key, recipe) -> {
			String[] ingredients = key.split("\\+");
			recipe.getAsJsonArray().forEach((data) -> {
				registerShardRecipe(ingredients[0], ingredients[1], data.getAsJsonObject().get("id").getAsString(), data.getAsJsonObject().get("count").getAsInt());
			});
		});
	}

	public static void registerShardRecipe(String ingredient1, String ingredient2, String result, int amount) {
		shardRecipes.add(new ShardRecipe(ingredient1, ingredient2, result, amount));
	}

	public static String getBazaarData(){
		LOGGER.info("Fetching bazaar prices");
		String fullJsonString = "";
		try {
			URL url = new URL("https://api.hypixel.net/v2/skyblock/bazaar");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				fullJsonString += line;
			}
			in.close();
		} catch (Exception e) {
			LOGGER.error(e.toString());
			LOGGER.info("getBazaarData");
		}
		return fullJsonString;
	}


	public static float getBuyOrder(String productID) {
		try {
      return bazaarProducts.getAsJsonObject(productID).getAsJsonObject("quick_status").get("buyPrice").getAsFloat();
		} catch (Exception e) {
      LOGGER.warn("%s cannot be bought on the bazaar!".formatted(productID));
			return 99999999999f;
    }
	}

	public static float getSellOrder(String productID) {
		try {
      return bazaarProducts.getAsJsonObject(productID).getAsJsonObject("quick_status").get("sellPrice").getAsFloat();
		} catch (Exception e) {
			LOGGER.warn("%s cannot be sold on the bazaar!".formatted(productID));
			return 99999999999f;
		}
	}

	public static JsonObject loadShardData() {
		JsonObject jsonData = new JsonObject();

		String path = "lefuser/shard-data.json";

		try {
			File dataFile = new File(path);
			BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
			StringBuilder data = new StringBuilder();
			String st;
			while ((st = bufferedReader.readLine()) != null){
				data.append(st);
			}

			jsonData = JsonParser.parseString(data.toString()).getAsJsonObject();

		} catch (Exception e) {
      LOGGER.error(e.toString());
    }

    return jsonData;
	}

	public static JsonObject loadShardRecipes() {
		JsonObject jsonData = new JsonObject();

		String path = "lefuser/fusion-recipes.json";

		try {
			File dataFile = new File(path);
			BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
			StringBuilder data = new StringBuilder();
			String st;
			while ((st = bufferedReader.readLine()) != null){
				data.append(st);
			}

			jsonData = JsonParser.parseString(data.toString()).getAsJsonObject();

		} catch (Exception e) {
			LOGGER.error(e.toString());
		}

		return jsonData;
	}

	public static void clientMessage(String message) {
		if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
	}

	public static void clientPrefixedMessage(String message) {
		if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.sendSystemMessage(Component.literal("§8[§bLe§dFuser§8]§f %s".formatted(message)));
	}

	public static void sendCommandFromPlayer(String command) {
		if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.connection.sendCommand(command);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
