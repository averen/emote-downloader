/*
 *    Copyright (C) Avery Clifton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package codes.meister.emotedownloader;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Emote;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class EmoteDownloader {

	private static final String version = "Emote Downloader 3.4.0";
	private static final String github = "github.com/averen";
	private static final OkHttpClient ok = new OkHttpClient();
	private static final Logger logger = LoggerFactory.getLogger(EmoteDownloader.class);
	private static boolean finished = false;
	private static int counter = 0;

	public static void main(String[] args) {

		// Setup our progress window
		JFrame window = new JFrame();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		window.setTitle(String.format("%s - %s", version, github));
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setResizable(false);
		window.setBounds(0, 0, 550, 110);
		window.setLocation(dim.width/2- window.getSize().width/2, dim.height/2- window.getSize().height/2);
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				try {
					if (finished || JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?", version, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
						System.exit(0);
					}
				} catch (Exception e) {
					exception(e);
				}
			}
		});
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		window.setContentPane(panel);
		JLabel label = new JLabel("Initializing...");
		JProgressBar progressBar = new JProgressBar();
		GroupLayout layout = new GroupLayout(panel);
		layout.setHorizontalGroup(
				layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
								.addContainerGap()
								.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
										.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
										.addComponent(label, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE))
								.addContainerGap())
		);
		layout.setVerticalGroup(
				layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
								.addContainerGap()
								.addComponent(label)
								.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE)
								.addContainerGap())
		);
		panel.setLayout(layout);
		window.setVisible(true);
		progressBar.setIndeterminate(true);
		// I know, this try catch is atrocious. Don't try this at home
		try {
			// Check to see if a configuration exists
			File source = new File("config.json");
			if (!source.exists()) {
				// Prompt for the required information and create the file
				String token = JOptionPane.showInputDialog(null, "To begin, please enter your CLIENT token.\nYou can find more information about this in the repository's README.", "Configuration", JOptionPane.QUESTION_MESSAGE);
				if (token == null || token.isEmpty()) {
					// User selected cancel or left the field empty
					System.exit(0);
				}
				FileUtils.writeStringToFile(source, new JSONObject()
						.put("token", token)
						.toString(2), "utf-8");
			}
			// Load our file from disk
			JSONObject config = new JSONObject(FileUtils.readFileToString(source, "utf-8"));
			if (config.has("mode")) {
				// Update an old configuration file
				config.remove("mode");
				FileUtils.writeStringToFile(source, config.toString(2), "utf-8");
			}
			label.setText("Logging in... (This may take a minute)");
			// Sign into Discord
			JDA jda = new JDABuilder(AccountType.CLIENT)
					.setToken(config.getString("token"))
					.buildBlocking();
			// Create a list of the emotes & close our session
			List<Emote> emotes = jda.getEmotes();
			jda.shutdown();
			// Configure the progress bar further
			progressBar.setIndeterminate(false);
			progressBar.setStringPainted(true);
			progressBar.setString("0/0");
			progressBar.setMaximum(emotes.size());
			// Create the directory and start the download
			File temp = new File("temp/");
			if (!temp.exists() && !temp.mkdir())
				showError("The download directory couldn't be created! Is the disk full?");
			File i = new File("emotes/index.json");
			JSONObject index = new JSONObject();
			if (i.exists())
				index = new JSONObject(FileUtils.readFileToString(i, "utf-8"));
			List<Emote> downloaded = new ArrayList<>();
			for (Emote emote : emotes) {
				label.setText(String.format("%s from %s", emote.getName(), emote.getGuild().getName()));
				if (!index.has(emote.getId())) {
					File o = new File(String.format("temp/%s.%s", emote.getId(), emote.isAnimated() ? "gif" : "png"));
					if (!o.exists()) {
						try {
							logger.trace(String.format("Downloading %s (%s) from %s (%s)", emote.getName(), emote.getId(), emote.getGuild().getName(), emote.getGuild().getId()));
							Response response = ok.newCall(new Request.Builder().url(emote.getImageUrl()).build()).execute();
							FileOutputStream fos = new FileOutputStream(o);
							fos.write(response.body().bytes());
							fos.close();
							downloaded.add(emote);
						} catch (Exception e) {
							logger.error("An error occurred while downloading "+ emote.getImageUrl(), e);
						}
					}
				}
				progressBar.setValue(++counter);
				progressBar.setString(String.format("%d/%d", counter, emotes.size()));
			}
			// Process the files to name them correctly
			progressBar.setIndeterminate(true);
			progressBar.setString("Ignore this text");
			label.setText("Processing files...");
			if (downloaded.size() == 0)
				showMessage("Nothing new was downloaded, nothing was modified.", JOptionPane.INFORMATION_MESSAGE);
			File e = new File("emotes/");
			if (!e.exists() && !e.mkdir())
				showError("The emotes directory couldn't be created! Is the disk full?");
			// Update guild directories
			List<String> p = new ArrayList<>();
			for (Emote emote : downloaded) {
				String id = emote.getGuild().getId();
				if (p.contains(id)) continue;
				String name = stripSpecialCharacters(emote.getGuild().getName());
				if (name.isEmpty()) name = id;
				File n = new File("emotes/"+ name);
				if (index.has(id) && !n.exists()) {
					// A folder for this guild already exists, but the guild was renamed
					File o = new File("emotes/"+ index.getString(id));
					if (o.exists() && !o.renameTo(n))
						showError(String.format("The directory %s could not be renamed. Are the permissions correct?", o.getPath()));
				} else {
					if (!n.exists() && !n.mkdir())
						showError(String.format("The directory %s could not be created. Is the disk full?", n.getPath()));
				}
				index.put(id, name);
				p.add(id);
			}
			// Copy the new files with friendly names
			for (Emote emote : downloaded) {
				String name = stripSpecialCharacters(emote.getName());
				if (name.isEmpty()) name = emote.getId();
				File n = new File(String.format("emotes/%s/%s.%s", index.getString(emote.getGuild().getId()), name, emote.isAnimated() ? "gif" : "png"));
				int counter = 1;
				// Prevent emotes with the same name from overwriting each other
				while (n.exists())
					n = new File(String.format("emotes/%s/%s (%d).%s", index.getString(emote.getGuild().getId()), name, counter++, emote.isAnimated() ? "gif" : "png"));
				File o = new File(String.format("temp/%s.%s", emote.getId(), emote.isAnimated() ? "gif" : "png"));
				if (o.exists() && !o.renameTo(n))
					showError(String.format("The file %s could not be renamed. Are the permissions correct?", o.getPath()));
				index.put(emote.getId(), name);
			}
			// Clean up after ourselves. The return value is ignored, as the user doesn't need to be notified.
			temp.delete();
			FileUtils.writeStringToFile(i, index.toString(2), "utf-8");
			progressBar.setIndeterminate(false);
			label.setText("Finished!");
			finished = true;
		} catch (Exception e) {
			exception(e);
		}

	}

	private static void exception(Exception e) {
		logger.error("An exception occurred", e);
		showError(e.toString());
	}

	private static void showError(String message) {
		showMessage(message, JOptionPane.ERROR_MESSAGE);
	}

	private static void showMessage(String message, int type) {
		JOptionPane.showMessageDialog(null, message, version, type);
		System.exit(1);
	}

	private static String stripSpecialCharacters(String input) {
		return input.replaceAll("[^a-zA-Z0-9\\-\\s]", "");
	}

}
