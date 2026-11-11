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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;
import io.github.mtrevisan.familylegacy.ui.features.Suggestion;

import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.Objects;


/**
 * A quiet strip at the bottom of an editor that says that an advanced concept could help with the record (a conclusion for competing
 * assertions, an identity hypothesis for a possible duplicate...), in the words of the level of the user. It is hidden until there is
 * something to say, it never takes the focus away from the editor, it opens no dialog by itself, and it can be dismissed; the user is
 * free to ignore it. Accepting a suggestion opens the usual editor on a draft.
 */
final class SuggestionStrip extends JPanel{

	private final JPanel rows = new JPanel();
	private List<Suggestion> shown = List.of();


	SuggestionStrip(){
		super(new BorderLayout(8, 0));
		setVisible(false);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor()),
			BorderFactory.createEmptyBorder(4, 4, 4, 4)));

		rows.setOpaque(false);
		rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
		add(rows, BorderLayout.CENTER);

		final JButton dismiss = new JButton("×");
		dismiss.setToolTipText(I18N.t("suggestion.dismiss"));
		dismiss.setBorderPainted(false);
		dismiss.setContentAreaFilled(false);
		dismiss.setFocusable(false);
		dismiss.addActionListener(e -> {
			//what the user hid is not suggested again: it is in the ignored findings of the assistant, where it can be shown again
			shown.stream()
				.map(Suggestion::dismiss)
				.filter(Objects::nonNull)
				.forEach(Runnable::run);
			setVisible(false);
		});
		add(dismiss, BorderLayout.EAST);
	}


	/** Shows the suggestions; the ones of a record that has none leave the strip hidden. */
	void show(final List<Suggestion> suggestions){
		rows.removeAll();
		shown = List.copyOf(suggestions);
		final UxLevel level = FeatureVisibilityService.shared().level();
		for(final Suggestion suggestion : suggestions)
			rows.add(row(suggestion, level));
		setVisible(!suggestions.isEmpty());
		revalidate();
	}

	private JComponent row(final Suggestion suggestion, final UxLevel level){
		//the message above the link: it fits whatever the width of the editor
		final JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setBorder(BorderFactory.createEmptyBorder(rows.getComponentCount() == 0? 0: 4, 0, 0, 0));

		final JLabel message = new JLabel(suggestion.message());
		message.setAlignmentX(LEFT_ALIGNMENT);
		if(suggestion.explanation() != null)
			message.setToolTipText("<html><body style='width:360px'>" + escape(suggestion.explanation()) + "</body></html>");
		row.add(message);

		final JButton action = new JButton("<html><u>" + escape(suggestion.kind().actionLabel(level)) + "</u></html>");
		action.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		action.setFocusPainted(false);
		action.setContentAreaFilled(false);
		action.setForeground(linkColor());
		action.setAlignmentX(LEFT_ALIGNMENT);
		action.setHorizontalAlignment(SwingConstants.LEFT);
		action.addActionListener(e -> {
			//accepted: the row goes away, the editor that opens is the usual one
			rows.remove(row);
			setVisible(rows.getComponentCount() > 0);
			revalidate();
			suggestion.action().run();
		});
		row.add(action);
		return row;
	}

	private static String escape(final String text){
		return StringUtils.replaceEach(text, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"});
	}

	private static Color borderColor(){
		final Color color = UIManager.getColor("Separator.foreground");
		return (color != null? color: Color.LIGHT_GRAY);
	}

	private static Color linkColor(){
		final Color color = UIManager.getColor("Component.linkColor");
		return (color != null? color: new Color(0x2A6FDB));
	}

}
