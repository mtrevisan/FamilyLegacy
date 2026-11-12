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
package io.github.mtrevisan.familylegacy.ui.dialogs.help;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


/**
 * The help of the application: the list of the pages on the left, the selected page on the right.
 * <p>
 * The pages are Markdown files bundled with the application (see {@link HelpTopics}), so the help works without a connection. A
 * link to another page ({@code page.md} or {@code page.md#heading}) opens it here; a link to the web opens the browser. The
 * window does not block the application and there is only one: opening the help again brings it to the front and shows the
 * requested page.
 */
public final class HelpBrowserDialog extends JDialog{

	/** The window being shown, if any: it is forgotten when closed, so that it can be released. */
	private static HelpBrowserDialog current;


	private final Locale locale = Locale.getDefault();
	private final List<HelpTopics.Topic> topics = HelpTopics.topics(locale);
	private final DefaultListModel<String> topicTitles = new DefaultListModel<>();
	private final JList<String> topicList = new JList<>(topicTitles);
	private final JEditorPane editor = new JEditorPane();
	private final JButton backButton = new JButton(I18N.t("dialog.help.back"));
	private final JButton forwardButton = new JButton(I18N.t("dialog.help.forward"));

	private final Deque<String> backHistory = new ArrayDeque<>();
	private final Deque<String> forwardHistory = new ArrayDeque<>();
	private String currentLocation;
	private boolean updatingList;


	private HelpBrowserDialog(final Window owner){
		super(owner, I18N.t("dialog.help.title"), ModalityType.MODELESS);

		GUIHelper.installEscapeToClose(this);
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosed(final WindowEvent e){
				if(current == HelpBrowserDialog.this)
					current = null;
			}
		});

		for(final HelpTopics.Topic topic : topics)
			topicTitles.addElement(topic.title());
		topicList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		topicList.addListSelectionListener(e -> {
			final int index = topicList.getSelectedIndex();
			if(!e.getValueIsAdjusting() && !updatingList && index >= 0)
				navigate(topics.get(index).file(), true);
		});

		editor.setContentType("text/html");
		editor.setEditable(false);
		editor.addHyperlinkListener(e -> {
			if(e.getEventType() == HyperlinkEvent.EventType.ENTERED)
				editor.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			else if(e.getEventType() == HyperlinkEvent.EventType.EXITED)
				editor.setCursor(java.awt.Cursor.getDefaultCursor());
			if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
				follow(e.getDescription());
		});

		backButton.addActionListener(e -> move(backHistory, forwardHistory));
		forwardButton.addActionListener(e -> move(forwardHistory, backHistory));

		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
		toolbar.add(backButton);
		toolbar.add(forwardButton);

		final JScrollPane listScroll = new JScrollPane(topicList);
		listScroll.setPreferredSize(new Dimension(210, 560));
		final JScrollPane contentScroll = new JScrollPane(editor);
		contentScroll.setPreferredSize(new Dimension(700, 560));
		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, contentScroll);
		split.setContinuousLayout(true);

		setLayout(new BorderLayout());
		add(toolbar, BorderLayout.NORTH);
		add(split, BorderLayout.CENTER);

		pack();
		setLocationRelativeTo(owner);
		updateButtons();
	}


	/**
	 * Opens the help on a page.
	 *
	 * @param owner	The window the help belongs to, may be {@code null}.
	 * @param location	The page ({@code page.md}), possibly followed by the heading to scroll to ({@code page.md#heading}).
	 */
	public static void show(final Window owner, final String location){
		if(current == null)
			current = new HelpBrowserDialog(owner);
		final HelpBrowserDialog dialog = current;

		dialog.navigate((location != null? location: HelpTopics.INDEX), true);
		dialog.setVisible(true);
		dialog.toFront();
	}


	/** Follows a link of a page. */
	private void follow(final String target){
		if(StringUtils.isBlank(target))
			return;

		if(Strings.CI.startsWithAny(target, "http:", "https:", "mailto:")){
			HelpViewerDialog.openExternal(target);
			return;
		}

		//a link to a heading of the same page
		navigate((target.startsWith("#")? fileOf(currentLocation) + target: target), true);
	}

	private void navigate(final String location, final boolean remember){
		final String file = fileOf(location);
		final String fragment = (location.contains("#")? StringUtils.substringAfter(location, "#"): null);
		final Optional<String> markdown = HelpTopics.markdown(file, locale);
		if(markdown.isEmpty())
			return;

		if(remember && currentLocation != null && !currentLocation.equals(location)){
			backHistory.push(currentLocation);
			forwardHistory.clear();
		}
		currentLocation = location;

		editor.setText(MarkdownToHtml.document(markdown.get()));
		SwingUtilities.invokeLater(() -> {
			if(StringUtils.isNotEmpty(fragment))
				editor.scrollToReference(fragment);
			else
				editor.setCaretPosition(0);
		});

		updatingList = true;
		try{
			topicList.clearSelection();
			for(int i = 0; i < topics.size(); i ++)
				if(topics.get(i).file().equals(file))
					topicList.setSelectedIndex(i);
		}
		finally{
			updatingList = false;
		}
		updateButtons();
	}

	private void move(final Deque<String> from, final Deque<String> to){
		if(from.isEmpty())
			return;

		to.push(currentLocation);
		final String location = from.pop();
		navigate(location, false);
	}

	private void updateButtons(){
		backButton.setEnabled(!backHistory.isEmpty());
		forwardButton.setEnabled(!forwardHistory.isEmpty());
	}

	private static String fileOf(final String location){
		return (location.contains("#")? StringUtils.substringBefore(location, "#"): location);
	}

}
