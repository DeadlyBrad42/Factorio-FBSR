package com.demod.fbsr.app;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.json.JSONObject;
import org.rapidoid.setup.App;

import com.demod.factorio.Config;
import com.demod.fbsr.Blueprint;
import com.demod.fbsr.BlueprintFinder;
import com.demod.fbsr.BlueprintStringData;
import com.demod.fbsr.FBSR;
import com.demod.fbsr.TaskReporting;
import com.google.common.util.concurrent.AbstractIdleService;

public class LocalCommandService extends AbstractIdleService {
	public static final String ConfigName = "command";

	public static String blueprintString = new String();

	private JSONObject configJson;

	public LocalCommandService() {
		System.out.println("LocalCommandService constructor called!");
		System.out.println("blueprintStr= " + blueprintString);
	}

	private String saveToLocalStorage(File folder, BufferedImage image) throws IOException {
		if (!folder.exists()) {
			folder.mkdirs();
		}

		File imageFile;
		long id = System.currentTimeMillis();
		String fileName;
		while ((imageFile = new File(folder, fileName = "Blueprint" + id + ".png")).exists()) {
			id++;
		}

		ImageIO.write(image, "PNG", imageFile);

		return fileName;
	}

	@Override
	protected void shutDown() {
		ServiceFinder.removeService(this);

		App.shutdown();
	}

	@Override
	protected void startUp() {
		ServiceFinder.addService(this);

		configJson = Config.get().getJSONObject(ConfigName);

		System.out.println("Parsing string from commandline!");
		TaskReporting reporting = new TaskReporting();

		String content = blueprintString;

		List<BlueprintStringData> blueprintStrings = BlueprintFinder.search(content, reporting);
		List<Blueprint> blueprints = blueprintStrings.stream().flatMap(s -> s.getBlueprints().stream())
				.collect(Collectors.toList());

		for (Blueprint blueprint : blueprints) {
			try {
				BufferedImage image = FBSR.renderBlueprint(blueprint, reporting);

				File localStorageFolder = new File(configJson.getString("local-storage"));
				String imageLink = saveToLocalStorage(localStorageFolder, image);
				reporting.addImage(blueprint.getLabel(), imageLink);
				reporting.addLink(imageLink);
			} catch (Exception e) {
				System.out.println("Something went terribly awry!");
			} finally {
				System.out.println("I think it worked??");
				System.out.println("created blueprint for " + blueprint.getLabel().orElse("... something new!"));
			}
		}
	}
}
