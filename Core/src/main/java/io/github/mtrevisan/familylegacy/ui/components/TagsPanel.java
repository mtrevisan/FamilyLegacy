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
package io.github.mtrevisan.familylegacy.ui.components;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordTags;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;


/**
 * The free tags of a record (see {@link RecordTags}): the ones it has, shown as small boxes ("chips") that wrap on several lines, and a
 * field to add another one, with the tags already used in the file to pick from. Tags are free text: any text can be typed, and several
 * tags can be given at once, separated by commas.
 * <p>
 * A tag is taken away with the <b>Delete</b> key (or Backspace) when its box has the focus, or by clicking its cross; the arrows move
 * the focus from one box to the next.
 * <p>
 * The panel works on a copy of the tags: nothing reaches the model until {@link #apply()}, so that an editor that is cancelled leaves
 * them as they were.
 */
public final class TagsPanel extends JPanel{

	private static final Color BORDER = (UIManager.getColor("Component.borderColor") != null
		? UIManager.getColor("Component.borderColor")
		: Color.GRAY);


	private final FLEFModel model;
	private final String recordId;

	private final List<String> tags = new ArrayList<>();
	private final ChipsPanel chips = new ChipsPanel();
	/** Where a new tag is typed: the last item of the area of the boxes, with no border of its own. */
	private final JTextField newTag = new JTextField(12);


	public TagsPanel(final FLEFModel model, final String recordId){
		this.model = model;
		this.recordId = recordId;

		initComponents();
		tags.addAll(model.tags().get(recordId));
		refresh();
	}


	private void initComponents(){
		newTag.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
		newTag.setOpaque(false);
		newTag.setToolTipText(I18N.t("dialog.tags.new.tooltip"));
		bindField(KeyEvent.VK_ENTER, this::addTyped);
		bindField(KeyEvent.VK_DOWN, this::showSuggestions);
		bindField(KeyEvent.VK_BACK_SPACE, () -> {
			//on an empty field, Backspace goes back to the last box, as in a field of tags
			if(newTag.getText().isEmpty() && chips.getComponentCount() > 1)
				chips.getComponent(chips.getComponentCount() - 2)
					.requestFocusInWindow();
		});
		bindField(KeyEvent.VK_LEFT, () -> {
			if(newTag.getCaretPosition() == 0 && chips.getComponentCount() > 1)
				chips.getComponent(chips.getComponentCount() - 2)
					.requestFocusInWindow();
		});
		newTag.addFocusListener(new FocusAdapter(){
			@Override
			public void focusLost(final FocusEvent e){
				//what was typed is a tag, even if Enter was not pressed
				if(!newTag.getText().isBlank())
					addTags(newTag.getText());
			}
		});

		final JScrollPane scroll = new JScrollPane(chips);
		scroll.setBorder(BorderFactory.createLineBorder(BORDER));
		scroll.getViewport()
			.setBackground(UIManager.getColor("TextField.background"));

		//a click on the free space of the area is a click in the field
		chips.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				newTag.requestFocusInWindow();
			}
		});

		setLayout(new MigLayout("ins 10,fill,wrap 1", "[grow,fill]", "[][grow,fill]"));
		add(new JLabel("<html><div style='width:380px'>" + I18N.t("dialog.tags.hint") + "</div></html>"), "growx");
		add(scroll, "grow,h 120:120:");
	}

	private void bindField(final int key, final Runnable action){
		final String name = "tagfield." + key;
		newTag.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, 0), name);
		newTag.getActionMap().put(name, new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent e){
				action.run();
			}
		});
	}

	/** The tags used in the file and not yet by this record, to pick from (Down in the field). */
	private void showSuggestions(){
		final JPopupMenu menu = new JPopupMenu();
		for(final String tag : model.tags().counts(model::hasRecord).keySet())
			if(!contains(tag)){
				final JMenuItem item = new JMenuItem(tag);
				item.addActionListener(e -> addTags(tag));
				menu.add(item);
			}
		if(menu.getComponentCount() > 0)
			menu.show(newTag, 0, newTag.getHeight());
	}

	/** Shows the tags as boxes, followed by the field to type another one. */
	private void refresh(){
		chips.removeAll();
		for(final String tag : tags)
			chips.add(new Chip(tag));
		chips.add(newTag);
		chips.revalidate();
		chips.repaint();
	}

	private boolean contains(final String tag){
		return tags.stream()
			.anyMatch(tag::equalsIgnoreCase);
	}

	private void addTyped(){
		addTags(newTag.getText());
		newTag.requestFocusInWindow();
	}

	/** Adds the tags in the text (several can be given, separated by commas), unless the record has them already. */
	public void addTags(final String text){
		for(final String tag : RecordTags.parse(text))
			if(!contains(tag))
				tags.add(tag);
		tags.sort(String.CASE_INSENSITIVE_ORDER);
		newTag.setText(StringUtils.EMPTY);
		refresh();
	}

	/** Takes the tag away, and gives the focus to the box that takes its place (or to the field, if there is none). */
	private void removeTag(final String tag){
		final int index = tags.indexOf(tag);
		tags.remove(tag);
		refresh();

		//the box that takes its place, or the field if it was the last one
		chips.getComponent(Math.min(index, chips.getComponentCount() - 1))
			.requestFocusInWindow();
	}

	/** @return	The tags as they are now in the panel (what was typed in the field and not added yet counts too). */
	public List<String> tags(){
		final List<String> result = new ArrayList<>(tags);
		for(final String tag : RecordTags.parse(newTag.getText()))
			if(result.stream().noneMatch(tag::equalsIgnoreCase))
				result.add(tag);
		return result;
	}

	/** Gives the tags to the model: the record has the ones of the panel. */
	public void apply(){
		model.tags()
			.set(recordId, tags());
	}


	/** A tag: its text and a cross; it takes the focus, and Delete (or Backspace, or a click on the cross) takes the tag away. */
	private final class Chip extends JPanel{

		private Chip(final String tag){
			super(new MigLayout("ins 4 10 4 6,gapx 6"));

			//the rounded box is painted by hand
			setOpaque(false);
			setFocusable(true);

			final JLabel cross = new JLabel("×");
			cross.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			cross.setToolTipText(I18N.t("dialog.tags.remove.tooltip"));
			cross.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					removeTag(tag);
				}
			});
			add(new JLabel(tag));
			add(cross);

			addMouseListener(new MouseAdapter(){
				@Override
				public void mousePressed(final MouseEvent e){
					requestFocusInWindow();
				}
			});
			addFocusListener(new FocusAdapter(){
				@Override
				public void focusGained(final FocusEvent e){
					repaint();
				}

				@Override
				public void focusLost(final FocusEvent e){
					repaint();
				}
			});

			bind(KeyEvent.VK_DELETE, () -> removeTag(tag));
			bind(KeyEvent.VK_BACK_SPACE, () -> removeTag(tag));
			bind(KeyEvent.VK_LEFT, () -> moveFocus(-1));
			bind(KeyEvent.VK_RIGHT, () -> moveFocus(1));
		}

		private void bind(final int key, final Runnable action){
			final String name = "tag." + key;
			getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key, 0), name);
			getActionMap().put(name, new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent e){
					action.run();
				}
			});
		}

		private void moveFocus(final int step){
			final int target = chips.getComponentZOrder(this) + step;
			if(target >= 0 && target < chips.getComponentCount())
				chips.getComponent(target)
					.requestFocusInWindow();
		}

		@Override
		protected void paintComponent(final Graphics g){
			final Graphics2D g2 = (Graphics2D)g.create();
			try{
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				final RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
				final Color fill = UIManager.getColor("Panel.background");
				g2.setColor(fill != null? fill: Color.LIGHT_GRAY);
				g2.fill(shape);
				Color outline = BORDER;
				if(isFocusOwner()){
					final Color focus = UIManager.getColor("Component.focusColor");
					outline = (focus != null? focus: UIManager.getColor("List.selectionBackground"));
				}
				g2.setColor(outline != null? outline: BORDER);
				g2.setStroke(new BasicStroke(isFocusOwner()? 2f: 1f));
				g2.draw(shape);
			}
			finally{
				g2.dispose();
			}
		}

	}


	/** The boxes, wrapping on as many lines as the width needs. */
	private static final class ChipsPanel extends JPanel implements Scrollable{

		private ChipsPanel(){
			super(new WrapLayout());

			setBackground(UIManager.getColor("TextField.background"));
		}

		@Override
		public Dimension getPreferredScrollableViewportSize(){
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction){
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction){
			return visibleRect.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth(){
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight(){
			return false;
		}

	}


	/** A flow layout whose preferred height counts the lines that the width obliges to. */
	private static final class WrapLayout extends FlowLayout{

		private WrapLayout(){
			super(FlowLayout.LEFT, 6, 6);
		}

		@Override
		public Dimension preferredLayoutSize(final Container target){
			return layoutSize(target);
		}

		@Override
		public Dimension minimumLayoutSize(final Container target){
			final Dimension minimum = layoutSize(target);
			minimum.width = 0;
			return minimum;
		}

		private Dimension layoutSize(final Container target){
			synchronized(target.getTreeLock()){
				int width = target.getWidth();
				if(width == 0 && target.getParent() != null)
					width = target.getParent()
						.getWidth();
				if(width == 0)
					width = Integer.MAX_VALUE;

				final Insets insets = target.getInsets();
				final int maxWidth = width - insets.left - insets.right - 2 * getHgap();
				int x = 0;
				int rowHeight = 0;
				int totalHeight = getVgap();
				int usedWidth = 0;
				for(final Component component : target.getComponents())
					if(component.isVisible()){
						final Dimension size = component.getPreferredSize();
						if(x > 0 && x + size.width > maxWidth){
							totalHeight += rowHeight + getVgap();
							x = 0;
							rowHeight = 0;
						}
						x += size.width + getHgap();
						usedWidth = Math.max(usedWidth, x);
						rowHeight = Math.max(rowHeight, size.height);
					}
				totalHeight += rowHeight + getVgap();
				return new Dimension(usedWidth + insets.left + insets.right + getHgap(), totalHeight + insets.top + insets.bottom);
			}
		}

	}

}
