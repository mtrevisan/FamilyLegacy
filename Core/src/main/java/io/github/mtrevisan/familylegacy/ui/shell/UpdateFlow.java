/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.UpdateDownloadDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.ProjectInfo;
import io.github.mtrevisan.updater.GitHubReleases;
import io.github.mtrevisan.updater.InstallerDownloader;
import io.github.mtrevisan.updater.InstallerLauncher;
import io.github.mtrevisan.updater.ReleaseInfo;
import io.github.mtrevisan.updater.Transport;
import io.github.mtrevisan.updater.UpdateCheck;
import io.github.mtrevisan.updater.UpdateChecker;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Help &gt; Check for Updates: looks for a newer release on GitHub, shows what it brings, downloads its installer, closes the application
 * and starts the installer.
 * <p>
 * The network work is done off the event thread; every dialog is shown on it.
 * </p>
 */
final class UpdateFlow{

	private static final int NOTES_COLUMNS = 60;
	private static final int NOTES_ROWS = 12;

	private final FamilyLegacyFrame frame;
	private final GitHubReleases releases;
	private final Transport transport;
	/** Only one check at a time: the menu item stays enabled while the answer is awaited. */
	private boolean busy;


	UpdateFlow(final FamilyLegacyFrame frame){
		this(frame, GitHubReleases.application(), Transport.http());
	}

	UpdateFlow(final FamilyLegacyFrame frame, final GitHubReleases releases, final Transport transport){
		this.frame = frame;
		this.releases = releases;
		this.transport = transport;
	}

	/** Starts the check; call it on the event thread. */
	void start(){
		if(busy)
			return;

		busy = true;
		frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		final String current = ProjectInfo.getAppVersion();
		new SwingWorker<UpdateCheck, Void>(){
			@Override
			protected UpdateCheck doInBackground() throws IOException{
				return new UpdateChecker(releases).check(current);
			}

			@Override
			protected void done(){
				busy = false;
				frame.setCursor(Cursor.getDefaultCursor());
				try{
					show(get(), current);
				}
				catch(final InterruptedException e){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException e){
					final Throwable cause = (e.getCause() != null? e.getCause(): e);
					error("menu.help.check.updates.failed", cause.getMessage());
				}
			}
		}.execute();
	}


	private void show(final UpdateCheck check, final String current){
		switch(check.status()){
			case NO_RELEASE -> info(I18N.t("menu.help.check.updates.none"));
			case UP_TO_DATE -> info(I18N.tf("menu.help.check.updates.current", current));
			case UNKNOWN_CURRENT_VERSION -> offerPage(I18N.tf("menu.help.check.updates.unknown", check.release().version().toString()),
				check.release());
			case UPDATE_AVAILABLE -> offerUpdate(check.release(), current);
		}
	}

	private void offerUpdate(final ReleaseInfo release, final String current){
		final String headline = I18N.tf("menu.help.check.updates.available", current, release.version().toString());
		if(!release.hasInstaller() || !InstallerLauncher.isSupportedPlatform()){
			offerPage(headline + "<br>" + I18N.t("menu.help.check.updates.manual"), release);
			return;
		}

		final Object[] options = {I18N.t("menu.help.check.updates.install"), I18N.t("menu.help.check.updates.page"),
			I18N.t("menu.help.check.updates.later")};
		final int choice = JOptionPane.showOptionDialog(frame, message(headline, release),
			I18N.t("menu.help.check.updates.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
		if(choice == 0)
			downloadAndInstall(release);
		else if(choice == 1)
			openPage(release.pageUrl());
	}

	private void offerPage(final String message, final ReleaseInfo release){
		final Object[] options = {I18N.t("menu.help.check.updates.page"), I18N.t("menu.help.check.updates.close")};
		final int choice = JOptionPane.showOptionDialog(frame, message(message, release), I18N.t("menu.help.check.updates.title"),
			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
		if(choice == 0)
			openPage(release.pageUrl());
	}

	/** @return	The headline (HTML) over the release notes, if there are any. */
	private static JPanel message(final String headline, final ReleaseInfo release){
		final JPanel panel = new JPanel(new BorderLayout(0, 8));
		panel.add(new JLabel("<html>" + headline + "</html>"), BorderLayout.NORTH);
		if(!release.notes().isBlank()){
			final JTextArea notes = new JTextArea(release.notes().strip(), NOTES_ROWS, NOTES_COLUMNS);
			notes.setEditable(false);
			notes.setLineWrap(true);
			notes.setWrapStyleWord(true);
			notes.setCaretPosition(0);
			final JScrollPane scroll = new JScrollPane(notes);
			scroll.setBorder(BorderFactory.createTitledBorder(I18N.t("menu.help.check.updates.notes")));
			panel.add(scroll, BorderLayout.CENTER);
		}
		return panel;
	}


	private void downloadAndInstall(final ReleaseInfo release){
		final Path directory;
		try{
			directory = Files.createTempDirectory("familylegacy-update");
		}
		catch(final IOException e){
			error("menu.help.check.updates.download.failed", e.getMessage());
			return;
		}

		final AtomicBoolean cancelled = new AtomicBoolean();
		final UpdateDownloadDialog dialog = new UpdateDownloadDialog(frame, I18N.t("menu.help.check.updates.download.title"),
			I18N.tf("menu.help.check.updates.download.message", release.version().toString()), () -> cancelled.set(true));

		final SwingWorker<Path, Void> worker = new SwingWorker<>(){
			@Override
			protected Path doInBackground() throws IOException{
				return new InstallerDownloader(transport).download(release, directory, dialog::progress, cancelled::get);
			}

			@Override
			protected void done(){
				dialog.close();
			}
		};
		worker.execute();
		//(modal: it returns once the worker has closed it)
		dialog.setVisible(true);

		final Path installer;
		try{
			installer = worker.get();
		}
		catch(final InterruptedException e){
			Thread.currentThread().interrupt();
			return;
		}
		catch(final ExecutionException e){
			if(!(e.getCause() instanceof InstallerDownloader.DownloadCancelledException))
				error("menu.help.check.updates.download.failed", (e.getCause() != null? e.getCause().getMessage(): e.getMessage()));
			deleteQuietly(directory);
			return;
		}

		final Object[] options = {I18N.t("menu.help.check.updates.run"), I18N.t("menu.help.check.updates.later")};
		final int choice = JOptionPane.showOptionDialog(frame,
			I18N.tf("menu.help.check.updates.downloaded", release.version().toString()), I18N.t("menu.help.check.updates.title"),
			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
		if(choice != 0)
			return;

		//the application asks about what is left to do and can be canceled by the user: the installer starts only if it really closes
		frame.exitApplicationAndRun(() -> {
			try{
				InstallerLauncher.launch(installer);
				return true;
			}
			catch(final IOException e){
				error("menu.help.check.updates.launch_failed", e.getMessage(), installer.toString());
				return false;
			}
		});
	}

	private static void deleteQuietly(final Path directory){
		try(final var files = Files.list(directory)){
			for(final Path file : files.toList())
				Files.deleteIfExists(file);
			Files.deleteIfExists(directory);
		}
		catch(final IOException ignored){
			//a leftover in the temporary folder is harmless
		}
	}


	private void openPage(final URI url){
		if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)){
			try{
				Desktop.getDesktop().browse(url);
				return;
			}
			catch(final IOException ignored){
				//falls through to the message
			}
		}
		JOptionPane.showMessageDialog(frame, I18N.tf("error.unable_to_open_url", url, ""),
			I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
	}

	private void info(final String message){
		JOptionPane.showMessageDialog(frame, message, I18N.t("menu.help.check.updates.title"), JOptionPane.INFORMATION_MESSAGE);
	}

	private void error(final String key, final Object... arguments){
		JOptionPane.showMessageDialog(frame, I18N.tf(key, arguments), I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
	}

}
