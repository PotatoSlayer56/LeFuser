package potatoslayer56.lefuser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeFuser implements ModInitializer {
	public static final String MOD_ID = "lefuser";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Map<String, Shard> shardsDirectory = new HashMap<>();
	public static List<ShardRecipe> shardRecipes = List.of();

	public static JsonObject bazaarData;
	public static JsonObject bazaarProducts;

	public static JsonObject shardData;
	public static JsonObject shardRecipeData;

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
			BigDecimal price = bazaarProducts.getAsJsonObject(productID).getAsJsonObject("quick_status").get("sellPrice").getAsBigDecimal();
			return price.floatValue();
		} catch (Exception e) {
      LOGGER.error(e.toString());
			LOGGER.info("getBuyOrder");
			return 0f;
    }
	}

	public static float getSellOrder(String productID) {
		try {
			BigDecimal price = bazaarProducts.getAsJsonObject(productID).getAsJsonObject("quick_status").get("buyPrice").getAsBigDecimal();
			return price.floatValue();
		} catch (Exception e) {
      LOGGER.error(e.toString());
			LOGGER.info("getSellOrder");
			return 0f;
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
