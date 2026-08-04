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
import io.github.mtrevisan.familylegacy.v2.ui.components.ConclusionListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.EvidenceQualifiersPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ModificationPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.ClassifiedNameListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.RestrictionPanel;
import io.github.mtrevisan.familylegacy.v2.ui.components.SourceCitationListPanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;


/* DONE */
/**
 * Dialog for editing a {@code PLACE_RECORD} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * record PlaceRecord {
 *   id: LocalID
 *   name+: ClassifiedName
 *   type?: enum {
 *     address, building, street, hamlet, village, town, municipality, city,
 *     metropolitan_area, county, province, department, district, region,
 *     macro_region, country, empire, parish, diocese, cemetery, archive, unknown
 *   } | Text
 *   map?: struct {
 *     coordinates: Coord
 *     evidence?: EvidenceQualifiers
 *   }
 *   source*: SourceCitation
 *   evidence?: EvidenceQualifiers
 *   restriction?: RestrictionStructure
 *   modification: ModificationStructure
 * }
 * </pre>
 */
public class PlaceRecordDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = -2581031991500033899L;


	private static final String DOT = ".";

	private static final String TAG_NAME = "NAME";
	private static final String TAG_TYPE = "TYPE";
	private static final String TAG_MAP = "MAP";
	private static final String TAG_COORDINATES = "COORDINATES";
	private static final String TAG_SOURCE = "SOURCE";
	private static final String TAG_EVIDENCE = "EVIDENCE";
	private static final String TAG_RESTRICTION = "RESTRICTION";
	private static final String TAG_CONCLUSION = "CONCLUSION";


	static{
		HandlerRegistry.register(new PlaceHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final JPanel mainPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]10[]5[]10[]5[]"));
	private final JTabbedPane tabbedPane = new JTabbedPane();

	private final ClassifiedNameListPanel namePanel;
	private final BoundComboBox<String> typeCombo;
	private final BoundTextField mapCoordinatesField;
	private final EvidenceQualifiersPanel mapQualifiers;
	private final SourceCitationListPanel sourcePanel;
	private final EvidenceQualifiersPanel placeQualifiers;
	private final RestrictionPanel restrictionPanel;
	private final ModificationPanel modificationPanel;

	// Other
	private final ConclusionListPanel conclusionPanel;


	public static PlaceRecordDialog createNew(final Dialog parent, final FLEFModel model){
		return new PlaceRecordDialog(parent, model, null);
	}

	public static PlaceRecordDialog createEdit(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new PlaceRecordDialog(parent, model, record);
	}


	private PlaceRecordDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(PlaceHandler.TYPE));

		namePanel = new ClassifiedNameListPanel(TAG_NAME, this, "Names*", model);
		typeCombo = new BoundComboBox<>(TAG_TYPE, new String[]{
			StringUtils.EMPTY, "address", "building", "street", "hamlet", "village", "town",
			"municipality", "city", "metropolitan_area", "county", "province",
			"department", "district", "region", "macro_region", "country",
			"empire", "parish", "diocese", "cemetery", "archive", "unknown"
		});
		mapCoordinatesField = new BoundTextField(TAG_MAP + DOT + TAG_COORDINATES, 31);
		mapQualifiers = new EvidenceQualifiersPanel(TAG_MAP + DOT + TAG_EVIDENCE, "Map Evidence");
		sourcePanel = new SourceCitationListPanel(TAG_SOURCE, this, model);
		placeQualifiers = new EvidenceQualifiersPanel(TAG_EVIDENCE, "Place Evidence");
		restrictionPanel = new RestrictionPanel(TAG_RESTRICTION, this);
		modificationPanel = new ModificationPanel(this, model);

		conclusionPanel = new ConclusionListPanel(TAG_CONCLUSION, this, model);

		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	protected void initComponents(){
		bindingManager.bind(typeCombo);
		bindingManager.bind(mapCoordinatesField);

		setLayout(new MigLayout("ins 10,fillx,top"));

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
		// name structure
		mainPanel.add(namePanel, "span 2,growx,wrap");

		// type
		mainPanel.add(new JLabel("Type:"), "align label");
		typeCombo.setEditable(true);
		mainPanel.add(typeCombo, "growx,wrap");

		// map
		final JPanel mapPanel = new JPanel(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]"));
		mapPanel.setBorder(new TitledBorder("Map"));
		mapPanel.add(new JLabel("Coordinates:"), "align label");
		mapPanel.add(mapCoordinatesField, "growx,wrap");
		mapPanel.add(mapQualifiers, "span 2,growx,wrap");
		mainPanel.add(mapPanel, "span 2,growx,wrap");

		// place evidence
		mainPanel.add(placeQualifiers, "span 2,growx,wrap");

		return mainPanel;
	}

	private JPanel createReferencesPanel(){
		final JPanel panel = new JPanel(new MigLayout("ins 10,fillx,top,wrap 1", "[grow]", "[]5[]5[]"));
		panel.add(conclusionPanel, "growx");
		panel.add(sourcePanel, "growx");
		return panel;
	}


	@Override
	protected void loadData(){
		bindingManager.load(record);

		namePanel.load(record);
		mapQualifiers.load(record);
		sourcePanel.load(record);
		placeQualifiers.load(record);
		restrictionPanel.load(record);
		modificationPanel.load(record);

		conclusionPanel.load(record);
	}

	@Override
	protected boolean validData(){
		if(!namePanel.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"At least one NAME structure is required.",
				tabbedPane, mainPanel, namePanel);

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		namePanel.save(record);

		bindingManager.save(record);

		mapQualifiers.save(record);
		sourcePanel.save(record);
		placeQualifiers.save(record);
		restrictionPanel.save(record);
		modificationPanel.save(record);

		conclusionPanel.save(record);
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final PlaceRecordDialog dialog = PlaceRecordDialog.createNew(null, model);
			dialog.setVisible(true);
		});
	}

}
