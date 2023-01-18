package cc.jambox.buttscape;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@PluginDescriptor(
	name = "Buttscape"
)
public class ButtscapePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ButtscapeConfig config;

	private HttpClient http;

	final int INTENSITY_CAP = 100;

	Thread cth;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Buttscape started!");
		http = HttpClient.newHttpClient();
		GetDevices();
		DeviceCapability(config.deviceName());
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Buttscape stopped!");
	}

	@Subscribe
	public void onGameTick(GameTick tick) {
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied hitsplatApplied) {
		if (!config.onReceiveHit()) {
			return;
		}
		if (hitsplatApplied.getActor() == client.getLocalPlayer()) {
			double hitpoints = client.getRealSkillLevel(Skill.HITPOINTS);
			double vibrationAmount = (double)hitsplatApplied.getHitsplat().getAmount() / hitpoints;
			VibrateAll(config.deviceName(), scale(vibrationAmount), config.onHitMilliseconds());
		}
	}

	// Expects a value from 0 to 1.
	private int scale(double in) {
		double scaled = in * config.intensityScalar() * 100;
		if (scaled > INTENSITY_CAP) {
			scaled = INTENSITY_CAP;
		} else if (scaled < 0) {
			scaled = 0;
		}
		return (int)scaled;
	}

	public void GetDevices() {
		HttpRequest request = HttpRequest.newBuilder(URI.create(config.apiurl() + "api/Device/List"))
				.header("SecretKey", config.secretKey())
				.GET()
				.build();
		http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept((x) -> {
			ChatDebug(this.getName() + " devices", x.body());
			CheckStatus(x.statusCode(), "list devices", "ToyWebBridge");
		});
	}

	public void DeviceCapability(String deviceName) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(config.apiurl() + "api/Device/Info/" + URLEncoder.encode(deviceName, StandardCharsets.UTF_8)))
				.header("SecretKey", config.secretKey())
				.GET()
				.build();
		http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept((x) -> {
			ChatDebug(deviceName + " capability", x.body());
			CheckStatus(x.statusCode(), deviceName+" capability", "ToyWebBridge");
		});
	}

	public void VibrateAll(String device, int intensity, int timeMilliseconds) {
		SendRequest("VibrateCmd", device, String.valueOf(intensity), null, RequestType.GET);
		cth = new Thread(() -> {
			// todo; think of a better way to do this
			try {
				Thread.sleep(config.onHitMilliseconds());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			if (cth == Thread.currentThread()) {
				VibrateStop(device);
				cth = null;
			}
		});
		cth.start();
	}
	public void VibrateStop(String device) {
		SendRequest("VibrateCmd", device, "0", null, RequestType.GET);
	}

//	public void VibrateSeq(String device, int intensity, int timeMilliseconds) {
//		SendRequest("SequenceVibrateCmd", device, null, "{\n" +
//				"   \"Loop\":false,\n" +
//				"   \"Time\":["+String.valueOf(timeMilliseconds)+",1],\n" +
//				"   \"Speeds\":[\n" +
//				"      ["+String.valueOf(intensity)+",0]\n" +
//				"   ]\n" +
//				"}", RequestType.POST);
//	}

	public void ChatDebug(String name, String text) {
		log.info(name + ": " + text);
		clientThread.invokeLater(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, name, text, name));
	}

	public void SendRequest(String action, String device, String arg, String body, RequestType rq) {
		String argument = arg == null ? "" : "/" + URLEncoder.encode(arg, StandardCharsets.UTF_8);
		URI uri = URI.create(config.apiurl() + "api/Device/" +
				URLEncoder.encode(action, StandardCharsets.UTF_8) + "/" + URLEncoder.encode(device, StandardCharsets.UTF_8) + argument);
		log.info(uri.toString());
		HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
				.header("SecretKey", config.secretKey());
		if (rq == RequestType.POST) {
			builder.POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
		} else if (rq == RequestType.GET) {
			builder.GET();
		}
		http.sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding()).thenAccept((x) -> {
			CheckStatus(x.statusCode(), action, device);
		});
	}

	public void CheckStatus(int statusCode, String action, String device) {
		if (statusCode != 200) {
			String errorName = statusCode == 400 ? "Bad Request" :
				statusCode == 401 ? "Unauthorized" :
				statusCode == 404 ? "Not found"
						: "Unknown error";
			ChatDebug("Buttscape", errorName + ": '" + action + "' for '" + device + "'");
		}

	}


	@Provides
	ButtscapeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ButtscapeConfig.class);
	}
}
