package cc.jambox.buttscape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("buttscape")
public interface ButtscapeConfig extends Config
{
	@ConfigItem(
			keyName = "apiurl",
			name = "API url",
			description = "API url (with trailing '/')"
	)
	default String apiurl()
	{
		return "http://localhost:5000/";
	}
	@ConfigItem(
			keyName = "deviceName",
			name = "Device name",
			description = "The device names"
	)
	default String deviceName()
	{
		return "";
	}
	@ConfigItem(
			keyName = "actionType",
			name = "Action type",
			description = "Action type"
	)
	default String actionType()
	{
		return "";
	}
	@ConfigItem(
			keyName = "secretKey",
			name = "Secret key",
			description = "Secret key (blank if none)"
	)
	default String secretKey()
	{
		return "";
	}
	@ConfigItem(
			keyName = "intensityScalar",
			name = "Intensity scalar",
			description = "Intensity scalar (For example, onReceiveDmg will scale vibration intensity to your health, you can then scale it further)"
	)
	default double intensityScalar()
	{
		return 1.0;
	}


	@ConfigSection(
		name = "Activation criteria",
		description = "Activation criteria",
		position = 999
	)
	String criteria = "section";

	@ConfigItem(
		keyName = "receiveHit",
		name = "On player damaged",
		description = "Beware the rock cake",
		section = criteria
	)
	default boolean onReceiveHit() { return false;	}
	@ConfigItem(
			keyName = "onHitMilliseconds",
			name = "On player damaged effect milliseconds",
			description = "600ms = 1 tick"
	)
	default int onHitMilliseconds() {
		return 600;
	}

}
