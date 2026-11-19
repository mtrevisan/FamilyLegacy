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
package io.github.mtrevisan.familylegacy.ui.components.searches;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.search.SearchMatcher;
import io.github.mtrevisan.familylegacy.search.SearchMode;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


/**
 * The search field of the lists and the boards, with the same powers of the record selection dialog: the text is matched word by word
 * ({@link SearchMatcher}), and the options narrow it to whole words, widen it to typos (fuzzy) or, where the notes and the citations of a
 * record can be searched too, include them.
 * <p>
 * The box is compact: a text field, with a hint that tells what it is for and a tooltip that tells how it works, and a small
 * button that opens a popup menu with one checkable item per {@link SearchOption}. The button shows a dot while an option is on, so a
 * narrowed search is not forgotten. The popup stays open while the items are toggled, and is also opened by {@code Alt+Down} in the field.
 * <p>
 * The options are not fixed: {@link #addOption(SearchMode, String)} registers one more at any time, and the popup grows by itself (the
 * button is hidden while there is none). The enabled modes are read with {@link #modes()}.
 * <p>
 * The listeners are told once the user has stopped typing, so a long list is not filtered at every key; a change of an option is told at
 * once.
 */
public final class SearchBox extends JPanel{

	private static final int DELAY_MS = 250;
	private static final String ACTION_OPEN_OPTIONS = "searchBox.openOptions";


	private final HintTextField field;
	private final JButton optionsButton = new JButton();
	private final FunnelIcon funnelIcon = new FunnelIcon();
	private final JPopupMenu popup = new JPopupMenu();
	/** The registered options, in the order they were added (that is the order of the popup). */
	private final Map<SearchMode, SearchOption> options = new LinkedHashMap<>();
	private final Map<SearchMode, JCheckBoxMenuItem> items = new LinkedHashMap<>();
	private final Set<SearchMode> enabled = EnumSet.noneOf(SearchMode.class);
	private final List<Runnable> listeners = new ArrayList<>();
	private final List<Runnable> optionListeners = new ArrayList<>();
	private final Timer timer = new Timer(DELAY_MS, e -> fire());


	/**
	 * Creates a box with the options <em>Whole word</em> and <em>Fuzzy</em>, and <em>Search in notes and citations</em> if asked.
	 *
	 * @param columns	The width of the text field.
	 * @param withNotes	Whether the search can include the notes and the citations (the text of the record is not all there is to find).
	 */
	public SearchBox(final int columns, final boolean withNotes){
		this(columns);

		addOption(SearchMode.WHOLE_WORD, I18N.t("search.mode.whole_word"), I18N.t("search.mode.whole_word.tooltip"));
		addOption(SearchMode.FUZZY, I18N.t("search.mode.fuzzy"), I18N.t("search.mode.fuzzy.tooltip"));
		if(withNotes)
			addOption(SearchMode.NOTES_AND_CITATIONS, I18N.t("search.mode.notes_and_citations"),
				I18N.t("search.mode.notes_and_citations.tooltip"));
	}

	/**
	 * Creates a box without options: register them with {@link #addOption(SearchMode, String)}.
	 *
	 * @param columns	The width of the text field.
	 */
	public SearchBox(final int columns){
		super(new BorderLayout(2, 0));

		field = new HintTextField(columns, I18N.t("search.box.placeholder"));
		field.setToolTipText(I18N.t("search.box.tooltip"));
		timer.setRepeats(false);
		field.getDocument()
			.addDocumentListener(new DocumentListener(){
				@Override
				public void insertUpdate(final DocumentEvent e){
					timer.restart();
				}

				@Override
				public void removeUpdate(final DocumentEvent e){
					timer.restart();
				}

				@Override
				public void changedUpdate(final DocumentEvent e){
					timer.restart();
				}
			});

		optionsButton.setIcon(funnelIcon);
		optionsButton.setFocusable(false);
		optionsButton.setBorderPainted(false);
		optionsButton.setContentAreaFilled(false);
		optionsButton.setMargin(new Insets(0, 2, 0, 2));
		optionsButton.setRolloverEnabled(true);
		optionsButton.addActionListener(e -> showOptions());
		optionsButton.setVisible(false);
		refreshButton();

		field.getInputMap(JComponent.WHEN_FOCUSED)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), ACTION_OPEN_OPTIONS);
		field.getActionMap()
			.put(ACTION_OPEN_OPTIONS, new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent e){
					showOptions();
				}
			});

		add(field, BorderLayout.CENTER);
		add(optionsButton, BorderLayout.EAST);
		setOpaque(false);
	}


	/**
	 * Registers an option, that is shown as the last item of the popup.
	 *
	 * @param mode	The mode the option switches on (an option for each mode, at most).
	 * @param label	The text of the item.
	 * @throws IllegalArgumentException	If the mode already has an option.
	 */
	public void addOption(final SearchMode mode, final String label){
		addOption(mode, label, null);
	}

	/**
	 * Registers an option, that is shown as the last item of the popup.
	 *
	 * @param mode	The mode the option switches on (an option for each mode, at most).
	 * @param label	The text of the item.
	 * @param description	The tooltip of the item (can be {@code null}).
	 * @throws IllegalArgumentException	If the mode already has an option.
	 */
	public void addOption(final SearchMode mode, final String label, final String description){
		addOption(new SearchOption(mode, label, description));
	}

	/**
	 * Registers an option, that is shown as the last item of the popup.
	 *
	 * @param option	The option (an option for each mode, at most).
	 * @throws IllegalArgumentException	If the mode already has an option.
	 */
	public void addOption(final SearchOption option){
		if(options.containsKey(option.mode()))
			throw new IllegalArgumentException("The mode " + option.mode() + " has already an option");

		options.put(option.mode(), option);
		final JCheckBoxMenuItem item = new StickyCheckBoxMenuItem(option.label());
		item.setToolTipText(option.description());
		item.addActionListener(e -> select(option.mode(), item.isSelected()));
		items.put(option.mode(), item);
		popup.add(item);
		if(popup.isVisible())
			popup.pack();
		optionsButton.setVisible(true);
		refreshButton();
	}

	/** @return	The registered options, in the order of the popup. */
	public List<SearchOption> options(){
		return Collections.unmodifiableList(new ArrayList<>(options.values()));
	}

	/**
	 * @param mode	A mode.
	 * @return	Whether the mode has an option and the user (or the code) has switched it on.
	 */
	public boolean isSelected(final SearchMode mode){
		return enabled.contains(mode);
	}

	/**
	 * Switches an option on or off, as if the user did it (the listeners are told if it changes).
	 *
	 * @throws IllegalArgumentException	If the mode has no option.
	 */
	public void setSelected(final SearchMode mode, final boolean selected){
		final JCheckBoxMenuItem item = items.get(mode);
		if(item == null)
			throw new IllegalArgumentException("The mode " + mode + " has no option");

		item.setSelected(selected);
		select(mode, selected);
	}

	private void select(final SearchMode mode, final boolean selected){
		final boolean changed = (selected? enabled.add(mode): enabled.remove(mode));
		if(!changed)
			return;

		refreshButton();
		for(final Runnable listener : optionListeners)
			listener.run();
		fire();
	}

	private void refreshButton(){
		funnelIcon.active = !enabled.isEmpty();

		final StringBuilder tooltip = new StringBuilder(I18N.t("search.box.options"));
		boolean first = true;
		for(final SearchMode mode : enabled){
			tooltip.append(first? ": ": ", ")
				.append(options.get(mode).label());
			first = false;
		}
		optionsButton.setToolTipText(tooltip.toString());
		optionsButton.repaint();
	}

	private void showOptions(){
		if(options.isEmpty() || !optionsButton.isShowing())
			return;

		//aligned to the right edge of the button: the button is at the end of the box, a popup that starts there would go out of a narrow dialog
		final int x = optionsButton.getWidth() - popup.getPreferredSize().width;
		popup.show(optionsButton, x, optionsButton.getHeight());
	}


	/** Replaces the grey hint shown while the field is empty (the default one is generic: a box can say what it searches). */
	public void setHint(final String hint){
		field.setHint(hint);
	}

	/** Tells when the text or a mode changes (once the user has stopped typing). */
	public void onChange(final Runnable listener){
		listeners.add(listener);
	}

	/**
	 * Tells, at once, when an option is switched on or off (and not when the text changes): for who debounces the text by itself, and so
	 * listens to the document of {@link #field()}. {@link #onChange(Runnable)} listeners are told of an option too.
	 */
	public void onOptionChange(final Runnable listener){
		optionListeners.add(listener);
	}

	private void fire(){
		for(final Runnable listener : listeners)
			listener.run();
	}

	public JTextField field(){
		return field;
	}

	/** @return	What the user typed, without the spaces around it. */
	public String query(){
		return field.getText().strip();
	}

	public boolean isBlank(){
		return query().isEmpty();
	}

	/** @return	The modes that are switched on, in a new set (so it can be handed to another thread). */
	public Set<SearchMode> modes(){
		return EnumSet.copyOf(enabled);
	}

	/**
	 * @param fields	The texts of a row (the fields are matched together, so the words of the query can be in different ones).
	 * @return	Whether the row matches the query (always, if the query is blank).
	 */
	public boolean matches(final String... fields){
		return matches(query(), modes(), fields);
	}

	/** The same for a query and some modes that are already read (to match many rows off the event dispatch thread). */
	public static boolean matches(final String query, final Set<SearchMode> modes, final String... fields){
		if(query.isEmpty())
			return true;

		final StringBuilder target = new StringBuilder();
		for(final String field : fields){
			if(field == null)
				continue;

			if(!target.isEmpty())
				target.append(' ');
			target.append(field);
		}
		return SearchMatcher.score(query, target.toString(), modes).passes();
	}

	/** @return	A test for a text (for the trees that prune what does not match), reading the query and the modes now. */
	public Predicate<String> predicate(){
		final String query = query();
		final Set<SearchMode> modes = modes();
		return text -> matches(query, modes, text);
	}

	/** @return	How many items the popup has (one for each option). */
	int popupItemCount(){
		return popup.getComponentCount();
	}


	/** A text field that shows a hint, in the colour of the disabled text, while it is empty (the pattern of IntelliJ and VS Code). */
	private static final class HintTextField extends JTextField{

		private String hint;


		private HintTextField(final int columns, final String hint){
			super(columns);

			this.hint = hint;
		}


		private void setHint(final String hint){
			this.hint = hint;
			repaint();
		}

		@Override
		protected void paintComponent(final Graphics g){
			super.paintComponent(g);

			if(!getText().isEmpty())
				return;

			final Graphics2D g2 = (Graphics2D)g.create();
			try{
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setColor(hintColor());
				g2.setFont(getFont());
				final Insets insets = getInsets();
				final FontMetrics metrics = g2.getFontMetrics();
				final int y = insets.top + (getHeight() - insets.top - insets.bottom - metrics.getHeight()) / 2 + metrics.getAscent();
				g2.drawString(hint, insets.left, y);
			}
			finally{
				g2.dispose();
			}
		}

		private Color hintColor(){
			final Color color = UIManager.getColor("TextField.inactiveForeground");
			return (color != null? color: getDisabledTextColor());
		}

	}


	/**
	 * A check box menu item that does not close the popup when it is clicked, so that more options can be toggled one after the other. The
	 * keyboard still closes it, as usual.
	 */
	private static final class StickyCheckBoxMenuItem extends JCheckBoxMenuItem{

		private StickyCheckBoxMenuItem(final String text){
			super(text);
		}


		@Override
		protected void processMouseEvent(final MouseEvent e){
			if(e.getID() == MouseEvent.MOUSE_RELEASED && contains(e.getPoint())){
				//`doClick` would close the popup: press and release the model directly
				getModel().setArmed(true);
				getModel().setPressed(true);
				getModel().setPressed(false);
				e.consume();
			}
			else
				super.processMouseEvent(e);
		}

	}


	/** A small funnel (the icon of a filter), with a dot when an option is on. */
	private static final class FunnelIcon implements Icon{

		private static final int SIZE = 14;
		private static final Color BADGE = new Color(0x3592C4);

		private boolean active;


		@Override
		public void paintIcon(final Component c, final Graphics g, final int x, final int y){
			final Graphics2D g2 = (Graphics2D)g.create();
			try{
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.translate(x, y);
				g2.setColor(c.isEnabled()? c.getForeground(): UIManager.getColor("Button.disabledText"));
				g2.fillPolygon(new int[]{1, 13, 8, 8, 6, 6}, new int[]{2, 2, 7, 12, 10, 7}, 6);
				if(active){
					g2.setColor(BADGE);
					g2.fillOval(8, 7, 6, 6);
				}
			}
			finally{
				g2.dispose();
			}
		}

		@Override
		public int getIconWidth(){
			return SIZE;
		}

		@Override
		public int getIconHeight(){
			return SIZE;
		}

	}

}
