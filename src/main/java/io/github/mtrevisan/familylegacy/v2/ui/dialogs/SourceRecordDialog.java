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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.DocumentListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NoteListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RepositoryCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.DateField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.PlaceCitationField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.NoteHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Dialog for editing a {@code SOURCE_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record SourceRecord {
 *   id: LocalID
 *   title+: NameStructure
 *   author?: Text
 *   publisher?: Text
 *   date?: DateStructure
 *   place?: PlaceCitation
 *   media_type?: enum { audio, book, card, electronic, fiche, film, magazine, manuscript, map, newspaper, photo, tombstone, video } | Text
 *   repository*: RepositoryCitation
 *   document*: Xref&lt;DocumentRecord&gt;
 *   note*: Xref&lt;NoteRecord&gt;
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class SourceRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 8722200901398839002L;


	private static final String TAG_VALUE = "TITLE.VALUE";
	private static final String TAG_AUTHOR = "AUTHOR";
	private static final String TAG_PUBLISHER = "PUBLISHER";
	private static final String TAG_DATE = "DATE";
	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_MEDIA_TYPE = "MEDIA_TYPE";
	private static final String TAG_REPOSITORY = "REPOSITORY";
	private static final String TAG_DOCUMENT = "DOCUMENT";
	private static final String TAG_NOTE = "NOTE";
	private static final String TAG_RESTRICTION = "RESTRICTION";


	static{
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new RepositoryHandler());
		HandlerRegistry.register(new NoteHandler());
		HandlerRegistry.register(new SourceHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField titleField;
	private final BoundTextField authorField;
	private final BoundTextField publisherField;
	private final BoundComboBox<String> mediaTypeCombo;
	private final DateField dateField;
	private final PlaceCitationField placeCitationField;
	private final RepositoryCitationListPanel repositoryCitationPanel;
	private final DocumentListPanel documentPanel;
	private final NoteListPanel notePanel;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;


	/**
	 * Creates a new dialog to create a new record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @return	A new dialog instance.
	 */
	public static SourceRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new SourceRecordDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @return	A new dialog instance.
	 */
	public static SourceRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new SourceRecordDialog(parent, model, record);
	}


	private SourceRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(SourceHandler.TYPE));

		titleField = new BoundTextField(TAG_VALUE, 30);
		authorField = new BoundTextField(TAG_AUTHOR, 30);
		publisherField = new BoundTextField(TAG_PUBLISHER, 30);
		dateField = DateField.createWithWrapperTag(TAG_DATE, this, "Valid Date", model);
		placeCitationField = PlaceCitationField.create(TAG_PLACE, this, model);
		mediaTypeCombo = new BoundComboBox<>(TAG_MEDIA_TYPE,
			new String[]{StringUtils.EMPTY, "audio", "book", "card", "electronic", "fiche", "film",
				"magazine", "manuscript", "map", "newspaper", "photo",
				"tombstone", "video"});
		mediaTypeCombo.setEditable(true);
		repositoryCitationPanel = new RepositoryCitationListPanel(TAG_REPOSITORY, this, model);
		documentPanel = new DocumentListPanel(TAG_DOCUMENT, this, model);
		notePanel = new NoteListPanel(TAG_NOTE, this, model);
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(titleField);
		bindingManager.bind(authorField);
		bindingManager.bind(publisherField);
		bindingManager.bind(mediaTypeCombo);


		setLayout(new MigLayout("ins 10,fillx,top"));

		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Modification", modificationPanel);
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10", "[right]rel[grow]", "[]5[]5[]5[]5[]5[]10[]"));

		// title
		panel.add(new JLabel("Title*:"), "align label");
		panel.add(titleField, "growx,wrap");

		// author
		panel.add(new JLabel("Author:"), "align label");
		panel.add(authorField, "growx,wrap");

		// publisher
		panel.add(new JLabel("Publisher:"), "align label");
		panel.add(publisherField, "growx,wrap");

		// place
		panel.add(new JLabel("Place:"), "align label");
		panel.add(placeCitationField, "growx,wrap");

		// date
		panel.add(new JLabel("Date:"), "align label");
		panel.add(dateField, "growx,wrap");

		// media type
		panel.add(new JLabel("Media Type:"), "align label");
		panel.add(mediaTypeCombo, "growx,wrap");

		// document
		panel.add(documentPanel, "span 2,growx");

		return panel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]10[]"));
		panel.add(repositoryCitationPanel, "growx");
		panel.add(notePanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		dateField.load(record);
		placeCitationField.load(record);
		repositoryCitationPanel.load(record);
		documentPanel.load(record);
		notePanel.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		return true;
	}

	@Override
	protected void saveData(){
		bindingManager.save(record);

		dateField.save(record);
		placeCitationField.save(record);
		repositoryCitationPanel.save(record);
		documentPanel.save(record);
		notePanel.saveReferences(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final SourceRecordDialog dialog = SourceRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
