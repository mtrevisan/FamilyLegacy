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
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quality.RecordReferences;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;


/**
 * Where a record is used: every record that points to it, and by which field (e.g. the media of an individual, the media part of an extract,
 * the preferred image of a group). A line is opened with a double-click, <b>Enter</b> or the <b>Open</b> button, in the usual editor of
 * the record; the list stays on the screen. ESC closes it. Nothing changes unless a record is edited and saved in its editor.
 */
public final class RecordUsageDialog extends JDialog{

	/** A record that points to the record, by the given field. */
	record Use(FLEFRecord owner, String path){}


	private final FLEFModel model;

	private final DefaultListModel<Use> usesModel = new DefaultListModel<>();
	private final JList<Use> usesList = new JList<>(usesModel);


	/**
	 * Shows where a record is used.
	 *
	 * @param owner  the window the list is shown over
	 * @param model  the model
	 * @param record the record
	 */
	public static void show(final Window owner, final FLEFModel model, final FLEFRecord record){
		new RecordUsageDialog(owner, model, record).setVisible(true);
	}


	RecordUsageDialog(final Window owner, final FLEFModel model, final FLEFRecord record){
		super(owner, I18N.tf("dialog.usage.title", record.getId()), ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.model = model;

		for(final RecordReferences.Reference use : RecordReferences.to(model, record.getId()))
			usesModel.addElement(new Use(use.owner(), use.path()));
		initComponents();

		pack();
		setLocationRelativeTo(owner);
	}


	private void initComponents(){
		usesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//the text is computed when painted, so that it shows what an editor has just changed
		usesList.setCellRenderer((list, value, index, selected, focus) -> {
			final JLabel label = new JLabel(I18N.tf("dialog.usage.use", describe(value.owner()), value.path()));
			label.setOpaque(true);
			label.setBackground(selected? list.getSelectionBackground(): list.getBackground());
			label.setForeground(selected? list.getSelectionForeground(): list.getForeground());
			return label;
		});
		usesList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2)
					openSelected();
			}
		});
		if(!usesModel.isEmpty())
			usesList.setSelectedIndex(0);

		usesList.registerKeyboardAction(e -> openSelected(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
		//ESC closes the list, from any component, without a button
		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout("fill,wrap 1", "[grow,fill,500::]", "[]5[grow,fill,200::]"));
		add(new JLabel(usesModel.isEmpty()? I18N.t("dialog.usage.none"): I18N.t("dialog.usage.hint")));
		add(new JScrollPane(usesList));
	}

	/** @return the name of a record in a list: its type, what it is about, and its identifier */
	private String describe(final FLEFRecord record){
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		String text = null;
		try{
			text = (handler != null? handler.getDisplayText(record, model): null);
		}
		catch(final RuntimeException ignored){
			//a record with an unexpected shape is still listed, by its identifier
		}
		final String type = (handler != null? handler.getLabel(): record.getTag());
		//the text of some records already ends with the identifier: it is not written twice
		final String tag = "[" + record.getId() + "]";
		return type + ": " + (StringUtils.isNotBlank(text)? (text.contains(tag)? text: text + StringUtils.SPACE + tag): tag);
	}

	/** Opens the editor of the selected record; the list stays. */
	void openSelected(){
		final Use use = usesList.getSelectedValue();
		if(use == null)
			return;

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(use.owner().getTag());
		//only a record that has an editor can be opened
		if(handler == null)
			return;

		handler.createEditDialog(this, model, use.owner()).setVisible(true);
		usesList.repaint();
	}

	/** @return the lines of the list */
	List<Use> uses(){
		return Collections.list(usesModel.elements());
	}

}
