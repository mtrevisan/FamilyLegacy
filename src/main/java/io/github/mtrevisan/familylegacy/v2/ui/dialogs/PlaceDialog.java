/*
 * Copyright (c) 2026 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
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

import io.github.mtrevisan.familylegacy.v2.io.FLEFRecordUtils;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundComboBox;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.ConclusionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.NameListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/**
 * Dialog for editing a PLACE_RECORD according to FLEF 0.1.0.
 * <p>
 * Structure:
 * <pre>
 * PLACE_RECORD :=
 *   n @<XREF:PLACE>@ PLACE    {1:1}
 *     +1 <<NAME_STRUCTURE>>    {1:M}
 *     +1 TYPE <PLACE_TYPE>    {0:1}
 *     +1 MAP    {0:1}
 *       +2 LATITUDE <PLACE_LATITUDE>    {1:1}
 *       +2 LONGITUDE <PLACE_LONGITUDE>    {1:1}
 *       +2 <<EVIDENCE_QUALIFIERS>>    {0:1}
 *     +1 <<SOURCE_CITATION>>    {0:M}
 *     +1 <<EVIDENCE_QUALIFIERS>>    {0:1}
 *     +1 <<RESTRICTION_STRUCTURE>>    {0:1}
 *     +1 <<CONCLUSION_STRUCTURE>>    {0:M}
 *     +1 <<MODIFICATION_STRUCTURE>>    {1:1}
 * </pre>
 */
public class PlaceDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2581031991500033899L;


	// Handlers
	static{
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new SourceHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final NameListPanel namePanel;
	private final BoundComboBox<String> typeCombo;
	private final SourceCitationListPanel sourcePanel;
	private final RestrictionPanel restrictionPanel;
	private final ConclusionPanel conclusionPanel;
	private final ModificationPanel modificationPanel;
	private final BoundTextField latitudeField = new BoundTextField("MAP.LATITUDE", 15);
	private final BoundTextField longitudeField = new BoundTextField("MAP.LONGITUDE", 15);
	private final EvidenceQualifiersPanel mapQualifiers = new EvidenceQualifiersPanel("MAP", "Map Evidence");
	private final EvidenceQualifiersPanel placeQualifiers = new EvidenceQualifiersPanel(null, "Place Evidence");
	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]5[]10[]5[]"));


	public static PlaceDialog createNew(final Dialog parent, final FLEFModel model){
		return new PlaceDialog(parent, model, null);
	}

	public static PlaceDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new PlaceDialog(parent, model, record);
	}

	private PlaceDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(PlaceHandler.TYPE));

		// Initialize components
		this.typeCombo = new BoundComboBox<>("TYPE", new String[]{
			StringUtils.EMPTY, "address", "building", "street", "hamlet", "village", "town",
			"municipality", "city", "metropolitan_area", "county", "province",
			"department", "district", "region", "macro_region", "country",
			"empire", "parish", "diocese", "cemetery", "archive", "unknown"
		});
		this.namePanel = new NameListPanel(this, model);
		this.sourcePanel = new SourceCitationListPanel("SOURCE", this, model);
		this.restrictionPanel = new RestrictionPanel(this);
		this.conclusionPanel = new ConclusionPanel(model, this);
		this.modificationPanel = new ModificationPanel(this);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	protected void initComponents(){
		bindingManager.bind(typeCombo);
		bindingManager.bind(latitudeField);
		bindingManager.bind(longitudeField);

		tabbedPane.addTab("Main", createMainPanel());
		tabbedPane.addTab("References", sourcePanel);
		tabbedPane.addTab("Restriction", restrictionPanel);
		tabbedPane.addTab("Conclusion", conclusionPanel);
		tabbedPane.addTab("Modification", modificationPanel);

		setLayout(new MigLayout("fillx,top"));
		add(tabbedPane, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(), this::save, this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JPanel createMainPanel(){
		mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// ----- NAME_STRUCTURE -----
		mainPanel.add(namePanel, "span 2, growx, wrap");

		// ----- TYPE -----
		mainPanel.add(new JLabel("Type:"), "align label");
		typeCombo.setEditable(true);
		mainPanel.add(typeCombo, "growx, wrap");

		// ----- MAP -----
		final JPanel mapPanel = new JPanel(new MigLayout("ins 5,fillx,top", "[right]rel[grow]", "[]5[]"));
		mapPanel.setBorder(new TitledBorder("Map"));

		mapPanel.add(new JLabel("Latitude:"), "align label");
		mapPanel.add(latitudeField, "growx, wrap");

		mapPanel.add(new JLabel("Longitude:"), "align label");
		mapPanel.add(longitudeField, "growx, wrap");

		mapPanel.add(mapQualifiers, "span 2, growx, wrap");

		mainPanel.add(mapPanel, "span 2, growx, wrap");

		// ----- Evidence for the place itself -----
		mainPanel.add(placeQualifiers, "span 2, growx, wrap");

		return mainPanel;
	}

	@Override
	protected void loadData(){
		// ---- Simple fields via BindingManager ----
		bindingManager.load(record);

		// ---- NAME_STRUCTURE ----
		namePanel.loadFromRecord(record);

		// ---- MAP qualifiers ----
		mapQualifiers.load(record);

		placeQualifiers.load(record);

		// ---- SOURCE_CITATION ----
		sourcePanel.load(record);

		// ---- RESTRICTION ----
		final FLEFRecord restrictionStruct = FLEFRecordUtils.findChild(record, "RESTRICTION");
		restrictionPanel.loadFromRecord(restrictionStruct);

		// ---- CONCLUSION ----
		final FLEFRecord conclusion = FLEFRecordUtils.findChild(record, "CONCLUSION");
		conclusionPanel.loadFromRecord(conclusion);

		// ---- MODIFICATION ----
		modificationPanel.load(record);
	}

	@Override
	protected boolean validData(){
		// At least one NAME is required
		if(!namePanel.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"At least one NAME structure is required ({1:M}).",
				tabbedPane, mainPanel, namePanel);

			return false;
		}

		// If MAP is present, both latitude and longitude must be filled
		final boolean hasLat = !latitudeField.isEmpty();
		final boolean hasLon = !longitudeField.isEmpty();
		if(hasLat ^ hasLon){
			JOptionPane.showMessageDialog(this,
				"Both LATITUDE and LONGITUDE are required when MAP is present.",
				"Validation Error", JOptionPane.ERROR_MESSAGE);

			return false;
		}

		// Validate restriction if present
		if(restrictionPanel.hasData() && !restrictionPanel.validateRequiredFields())
			return false;

		if(conclusionPanel.hasData() && !conclusionPanel.validateRequiredFields())
			return false;

		return true;
	}

	@Override
	protected void saveData(){
		// ---- NAME_STRUCTURE ----
		namePanel.saveToRecord(record);

		// ---- Simple fields via BindingManager ----
		bindingManager.save(record);

//		// ---- MAP (0:1) ----
//		// The BindingManager already wrote LATITUDE and LONGITUDE under MAP.
//		// Now we need to add the MAP node and its qualifiers.
//		// First, check if latitude or longitude were set.
//		String lat = latitudeField.getText().trim();
//		String lon = longitudeField.getText().trim();
//		if(!lat.isEmpty() && !lon.isEmpty()){
//			// Ensure MAP node exists (binding might have created it, but we'll create/overwrite)
//			FLEFRecordUtils.removeChildren(record, "MAP");
//
//			// Add latitude and longitude (already set by binding, but we'll add them again to be safe)
//			FLEFRecord map = FLEFRecord.createChild("MAP");
//			map.addChild(FLEFRecord.createChildWithValue("LATITUDE", lat));
//			map.addChild(FLEFRecord.createChildWithValue("LONGITUDE", lon));
//			record.addChild(map);
//
			// Add qualifiers
			mapQualifiers.save(record);
//		}
//		else
//			// Remove any stale MAP node if no data
//			FLEFRecordUtils.removeChildren(record, "MAP");

		// ---- EVIDENCE_QUALIFIERS for place ----
		placeQualifiers.save(record);

		// ---- SOURCE_CITATION ----
		sourcePanel.save(record);

		// ---- RESTRICTION ----
		if(restrictionPanel.hasData())
			restrictionPanel.saveToRecord(record);

		// ---- MODIFICATION ----
		modificationPanel.save(record);

		if(conclusionPanel.hasData())
			conclusionPanel.saveToRecord(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final PlaceDialog dialog = PlaceDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
