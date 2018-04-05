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

package codes.meister.emotedownloader.core;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Emote;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class EmoteDownloader {

	private static final String version = "Emote Downloader 3.3.1";
	private static final String credits = "github.com/averen";
	private static final String errorMsg = "\nIf you need any help, contact me on discord @ meister#7070";
	private static final OkHttpClient ok = new OkHttpClient();
	private static boolean finished = false;
	private static int counter = 0;

	public static void main(String[] args) {

		// Setup our progress window
		JFrame window = new JFrame();
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		window.setTitle(String.format("%s - %s", version, credits));
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
		// I'm a mad lad with this exception handling
		try {
			// Load our config from disk
			JSONObject config = new JSONObject(FileUtils.readFileToString(new File("config.json"), "utf-8"));
			label.setText("Logging in... (This may take a minute)");
			// Sign into Discord
			JDA jda = new JDABuilder(AccountType.CLIENT)
					.setToken(config.getString("token"))
					.buildBlocking();
			// Copy our emotes from JDA's cache & shutdown our session
			List<Emote> emotes = jda.getEmotes();
			jda.shutdown();
			// Configure the progress bar further
			progressBar.setIndeterminate(false);
			progressBar.setStringPainted(true);
			progressBar.setString("0/0");
			progressBar.setMaximum(emotes.size());
			// Create directories and start the download
			File rootDirectory = new File("emotes/");
			if (!rootDirectory.exists()) rootDirectory.mkdirs();
			for (Emote emote : emotes) {
				label.setText(String.format("%s from %s", emote.getName(), emote.getGuild().getName()));
				File emoteFile;
				if (config.optString("mode").equals("ordered")) {
					String directory = String.format("%s/%s-%s/", rootDirectory.getPath(), stripSpecialCharacters(emote.getGuild().getName()), emote.getGuild().getId());
					if (!new File(directory).exists()) new File(directory).mkdirs();
					emoteFile = new File(String.format("%s%s-%s%s", directory, stripSpecialCharacters(emote.getName()), emote.getId(), emote.getImageUrl().substring(emote.getImageUrl().lastIndexOf("."))));
				} else {
					emoteFile = new File(String.format("%s%s-%s%s", rootDirectory, stripSpecialCharacters(emote.getName()), emote.getId(), emote.getImageUrl().substring(emote.getImageUrl().lastIndexOf("."))));
				}
				if (!emoteFile.exists()) {
					download(emote.getImageUrl(), emoteFile);
				}
				counter++;
				progressBar.setValue(counter);
				progressBar.setString(String.format("%d/%d", counter, emotes.size()));
			}
			label.setText("Finished!");
			finished = true;
		} catch (Exception e) {
			exception(e);
		}

	}

	private static void exception(Exception e) {
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, e.toString() + errorMsg, version, JOptionPane.ERROR_MESSAGE);
		System.exit(1);
	}

	private static String stripSpecialCharacters(String input) {
		return input.replaceAll("[^a-zA-Z0-9.\\-]", "");
	}

	private static void download(String url, File destination) {
		try {
			Response response = ok.newCall(new Request.Builder().url(url).build()).execute();
			FileOutputStream fos = new FileOutputStream(destination);
			fos.write(response.body().bytes());
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
