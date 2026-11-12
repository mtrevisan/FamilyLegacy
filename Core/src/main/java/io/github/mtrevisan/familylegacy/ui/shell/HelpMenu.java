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
import io.github.mtrevisan.familylegacy.preferences.AppPreferences;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.DiagnosticsDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.HelpBrowserDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.HelpTopics;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.KeyboardShortcutsDialog;
import io.github.mtrevisan.familylegacy.ui.helpers.ProjectInfo;
import io.github.mtrevisan.familylegacy.ui.helpers.ShortcutRegistry;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * The Help menu of the application, together with the actions it triggers
 * (open the online documentation, check for updates, show the About dialog).
 */
final class HelpMenu{

	private final FamilyLegacyFrame frame;

	private final UpdateFlow updateFlow;

	HelpMenu(final FamilyLegacyFrame frame){
		this.frame = frame;
		updateFlow = new UpdateFlow(frame);
	}

	JMenu create(){
		final JMenu menu = new JMenu(I18N.t("menu.help"));
		menu.setMnemonic(KeyEvent.VK_H);

		final JMenuItem helpContents = new JMenuItem(ShortcutRegistry.HELP_CONTENTS.action(), KeyEvent.VK_H);
		helpContents.setAccelerator(ShortcutRegistry.HELP_CONTENTS.keyStroke());
		helpContents.addActionListener(e -> HelpBrowserDialog.show(frame, HelpTopics.INDEX));
		menu.add(helpContents);

		final JMenuItem shortcuts = new JMenuItem(I18N.t("menu.help.keyboard.shortcuts"), KeyEvent.VK_K);
		shortcuts.addActionListener(e -> KeyboardShortcutsDialog.show(frame));
		menu.add(shortcuts);

		menu.add(new JSeparator());

		final JMenuItem onlineDocs = new JMenuItem(I18N.t("menu.help.online.documentation"), KeyEvent.VK_D);
		onlineDocs.addActionListener(e -> openUrl(HelpContent.DOCUMENTATION_URL));
		menu.add(onlineDocs);

		menu.add(new JSeparator());

		final JMenuItem checkUpdates = new JMenuItem(I18N.t("menu.help.check.updates"), KeyEvent.VK_U);
		checkUpdates.addActionListener(e -> updateFlow.start());
		menu.add(checkUpdates);

		final JCheckBoxMenuItem automaticUpdates = new JCheckBoxMenuItem(I18N.t("menu.help.check.updates.auto"),
			AppPreferences.getInstance().updateCheckEnabled());
		automaticUpdates.setToolTipText(I18N.t("menu.help.check.updates.auto.tooltip"));
		automaticUpdates.addActionListener(e -> AppPreferences.getInstance().setUpdateCheckEnabled(automaticUpdates.isSelected()));
		menu.add(automaticUpdates);

		final JMenuItem reportBug = new JMenuItem(I18N.t("menu.help.bug.report"), KeyEvent.VK_B);
		reportBug.addActionListener(e -> openUrl(HelpContent.ISSUE_TRACKER_URL));
		menu.add(reportBug);

		final JMenuItem diagnostics = new JMenuItem(I18N.t("menu.help.diagnostics"), KeyEvent.VK_I);
		diagnostics.addActionListener(e -> {
			final File file = frame.fileController()
				.currentFile();
			DiagnosticsDialog.show(frame, frame.model(), file);
		});
		menu.add(diagnostics);

		menu.add(new JSeparator());

		final JMenuItem about = new JMenuItem(I18N.t("menu.help.about"), KeyEvent.VK_A);
		about.addActionListener(e -> showAboutDialog());
		menu.add(about);

		return menu;
	}

	private void openUrl(final String url){
		if(!Desktop.isDesktopSupported()){
			showUnsupportedBrowserMessage();
			return;
		}
		final Desktop desktop = Desktop.getDesktop();
		if(!desktop.isSupported(Desktop.Action.BROWSE)){
			showUnsupportedBrowserMessage();
			return;
		}
		try{
			desktop.browse(new URI(url));
		}
		catch(final URISyntaxException e){
			JOptionPane.showMessageDialog(frame,
				I18N.tf("error.invalid_url", url),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
		}
		catch(final IOException e){
			JOptionPane.showMessageDialog(frame,
				I18N.tf("error.unable_to_open_url", url, e.getMessage()),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void showUnsupportedBrowserMessage(){
		JOptionPane.showMessageDialog(frame,
			I18N.t("error.not_supported.message"),
			I18N.t("error.not_supported.title"), JOptionPane.WARNING_MESSAGE);
	}

	private void showAboutDialog(){
		JOptionPane.showMessageDialog(frame, I18N.tf("menu.help.about.message", ProjectInfo.getAppVersion()),
			I18N.t("menu.help.about.title"), JOptionPane.INFORMATION_MESSAGE);
	}

}
