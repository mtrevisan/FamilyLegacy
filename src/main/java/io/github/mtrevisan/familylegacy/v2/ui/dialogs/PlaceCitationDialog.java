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
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.XRefHelper;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.lists.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceCitationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Dialog for editing a {@code PLACE_CITATION} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct PlaceCitation {
 *   place: Xref&lt;PlaceRecord&gt;
 *   original_text?: Text
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 * }
 * </pre>
 */
public class PlaceCitationDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 6489523892351201199L;


	private static final String TAG_PLACE = "PLACE";
	private static final String TAG_ORIGINAL_TEXT = "ORIGINAL_TEXT";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";


	static{
		HandlerRegistry.register(new PlaceCitationHandler());
	}


	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]"));

	private final BindingManager bindingManager = new BindingManager();

	private final BoundTextField place;
	private final BoundTextField originalTextField;
	private final SourceCitationListPanel sourceCitationPanel;
	private final EvidenceQualifiersPanel qualifiers;


	public static PlaceCitationDialog createNew(final Dialog parent, final FLEFModel model){
		return createNew(parent, model, PlaceCitationDialog::new);
	}

	public static PlaceCitationDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		return createEdit(parent, model, record, PlaceCitationDialog::new);
	}


	private PlaceCitationDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(PlaceCitationHandler.TYPE));

		place = new BoundTextField(TAG_PLACE);
		originalTextField = new BoundTextField(TAG_ORIGINAL_TEXT, 30);
		sourceCitationPanel = new SourceCitationListPanel(TAG_SOURCE, this, "Sources", model);
		qualifiers = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Evidence");


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		bindingManager.bind(place);
		bindingManager.bind(originalTextField);


		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", createReferencesPanel());

		finalizeLayout(tabbedPane);
	}

	private JPanel createMainPanel(){
		// original text
		mainPanel.add(new JLabel("Original Text*:"), "align label");
		mainPanel.add(originalTextField, "growx, wrap");

		// qualifiers
		mainPanel.add(qualifiers, "span 2,growx");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]"));
		panel.add(sourceCitationPanel, "growx");
		return panel;
	}


	public void setPlace(final String placeId){
		if(StringUtils.isNotEmpty(placeId)){
			if(!confirmRecordExistsForType(placeId, PlaceHandler.TYPE))
				return;

			place.setText(placeId);

			refreshLayout();
		}
	}

	private void refreshLayout(){
		mainPanel.revalidate();
		mainPanel.repaint();

		pack();
	}


	@Override
	protected void loadData(){
		if(record == null)
			return;

		bindingManager.load(record);

		sourceCitationPanel.load(record);
		qualifiers.load(record);
	}

	@Override
	protected boolean validData(){
		if(StringUtils.isEmpty(place.getText())){
			JOptionPane.showMessageDialog(null,
				"Place is required for a citation.\n" +
					"Please select a place record.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		FLEFRecordHelper.updateChildValue(record, TAG_PLACE, XRefHelper.formatXRef(place.getText()));

		bindingManager.save(record);

		sourceCitationPanel.save(record);
		qualifiers.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final FLEFRecord place = FLEFRecord.createMainRecord("P1", TAG_PLACE);
			model.addRecord(place);

//			final FLEFRecord placeCitation = FLEFRecord.createEmpty();
//			placeCitation.addChild(FLEFRecord.createChildWithValue(TAG_PLACE, "P1"));
//			final PlaceCitationDialog dialog = PlaceCitationDialog.createEdit(null, model, placeCitation);
			final PlaceCitationDialog dialog = PlaceCitationDialog.createNew(null, model);
			dialog.setPlace("P1");
			dialog.setVisible(true);
		});
	}

}
