/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.utilities;

import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.function.BiConsumer;


public class TagPanel extends JPanel{

	private static final long serialVersionUID = 665517573169978352L;

	public enum TagChangeType{SET, ADD, REMOVE, CLEAR}


	private BiConsumer<TagChangeType, Iterable<String>> tagsChanged;


	public TagPanel(){
		setLayout(new HorizontalFlowLayout(FlowLayout.LEFT, 2, 0));
	}

	public TagPanel(final BiConsumer<TagChangeType, Iterable<String>> tagsChanged){
		this();

		setTagsChanged(tagsChanged);
	}

	public TagPanel(final BiConsumer<TagChangeType, Iterable<String>> tagsChanged, final String... tags){
		this(tagsChanged);

		addTag(tags);
	}

	public void setTagsChanged(final BiConsumer<TagChangeType, Iterable<String>> tagsChanged){
		this.tagsChanged = tagsChanged;
	}

	@Override
	public Color getBackground(){
		return UIManager.getColor("TextField.background");
	}

	public void addTag(final String... tags){
		addTag(Arrays.asList(tags));
	}

	public void addTag(final Iterable<String> tags){
		synchronized(getTreeLock()){
			if(tags == null)
				removeAll();
			else{
				for(final String tag : tags){
					final TagComponent component = new TagComponent(tag, this::removeTag);
					add(component, BorderLayout.LINE_END);
				}

				if(tagsChanged != null)
					tagsChanged.accept(TagChangeType.ADD, tags);
			}

			forceRepaint();
		}
	}

	public java.util.List<String> getTags(){
		final Component[] components = getComponents();
		final java.util.List<String> tags = new ArrayList<>(components.length);
		for(final Component component : components)
			tags.add(((TagComponent)component).getTag());
		return tags;
	}

	private void removeTag(final TagComponent tag){
		synchronized(getTreeLock()){
			remove(tag);

			if(tagsChanged != null)
				tagsChanged.accept(TagChangeType.REMOVE, Collections.singletonList(tag.getTag()));

			forceRepaint();
		}
	}

	private void forceRepaint(){
		repaint();
		revalidate();
	}

	public void applyFilter(final String tag){
		GUIHelper.executeOnEventDispatchThread(() -> {
			if(tag == null || tag.isEmpty())
				for(final Component component : getComponents())
					component.setVisible(true);
			else{
				final String lowercaseTag = tag.toLowerCase(Locale.ROOT);
				for(final Component component : getComponents())
					component.setVisible(((TagComponent)component).getTag().toLowerCase(Locale.ROOT).contains(lowercaseTag));
			}
		});
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final JDialog dialog = new JDialog(parent, true);
			final TagPanel tagPanel = new TagPanel(null);
			tagPanel.addTag("un");
			tagPanel.addTag("do");
			tagPanel.addTag("trè");
			tagPanel.addTag("kuatro");
			tagPanel.addTag("ŧinkue");

			final JScrollPane scrollPane = new JScrollPane();
			scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setViewportView(tagPanel);

			dialog.add(scrollPane);
			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(200, 100);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
