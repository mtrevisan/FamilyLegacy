/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.panels;

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CertaintyComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCredibility;


public class SupportedByCulturalNormPanel extends JPanel implements RelationshipDataPanelInterface{

	@Serial
	private static final long serialVersionUID = -2145083268284871901L;


	private final JLabel certaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> certaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

	private final String tableName;
	private Integer recordID;
	private final int groupID;

	private Map<String, Object> relationshipData = new HashMap<>(0);

	protected volatile boolean ignoreEvents;


	public static SupportedByCulturalNormPanel create(final String tableName, final int groupID){
		return new SupportedByCulturalNormPanel(tableName, groupID);
	}


	private SupportedByCulturalNormPanel(final String tableName, final int groupID){
		this.tableName = tableName;
		this.groupID = groupID;

		initialize();
	}


	private void initialize(){
		initComponents();

		initLayout();
	}

	protected void initComponents(){
		GUIHelper.bindLabelUndoAutoComplete(certaintyLabel, certaintyComboBox);
		GUIHelper.bindOnSelectionChange(certaintyComboBox, this::saveData);

		GUIHelper.bindLabelUndoAutoComplete(credibilityLabel, credibilityComboBox);
		GUIHelper.bindOnSelectionChange(credibilityComboBox, this::saveData);
	}

	private void initLayout(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(certaintyLabel, "align label,sizegroup lbl,split 2");
		add(certaintyComboBox, "wrap related");
		add(credibilityLabel, "align label,sizegroup lbl,split 2");
		add(credibilityComboBox);

		setOpaque(false);
	}

	public final Map<String, Object> getRelationshipData(){
		return relationshipData;
	}


	@Override
	public void loadData(final int recordID){
		this.recordID = recordID;


		final List<Map<String, Object>> relationships = Repository.findRelationships(tableName, recordID,
			EntityManager.NODE_GROUP, groupID,
			EntityManager.RELATIONSHIP_BELONGS_TO
		);
		relationshipData = (!relationships.isEmpty()? relationships.getFirst(): new HashMap<>(0));

		final String certainty = extractRecordCertainty(relationshipData);
		final String credibility = extractRecordCredibility(relationshipData);

		callWithoutEvents(() -> {
			certaintyComboBox.setSelectedItem(certainty);
			credibilityComboBox.setSelectedItem(credibility);
		});
	}

	@Override
	public void clearData(){
		callWithoutEvents(() ->{
			certaintyComboBox.setSelectedItem(null);
			credibilityComboBox.setSelectedItem(null);
		});
	}

	protected void callWithoutEvents(final Runnable run){
		ignoreEvents = true;

		run.run();

		ignoreEvents = false;
	}

	private boolean saveData(){
		if(ignoreEvents)
			return false;

		final String certainty = GUIHelper.getTextTrimmed(certaintyComboBox);
		final String credibility = GUIHelper.getTextTrimmed(credibilityComboBox);

		insertRecordCertainty(relationshipData, certainty);
		insertRecordCredibility(relationshipData, credibility);
		Repository.upsertRelationship(tableName, recordID,
			EntityManager.NODE_GROUP, groupID,
			EntityManager.RELATIONSHIP_BELONGS_TO, relationshipData,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		return true;
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		final int person1ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		final Map<String, Object> group1 = new HashMap<>();
		group1.put("type", "family");
		int group1ID = Repository.upsert(group1, EntityManager.NODE_GROUP);
		final Map<String, Object> relationshipData = new HashMap<>(3);
		insertRecordCertainty(relationshipData, "certain");
		insertRecordCredibility(relationshipData, "direct and primary evidence used, or by dominance of the evidence");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person1ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, relationshipData,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);


		EventQueue.invokeLater(() -> {
			final SupportedByCulturalNormPanel panel = create(EntityManager.NODE_PERSON, group1ID);
			panel.loadData(person1ID);

			EventBusService.subscribe(panel);

			final JFrame frame = new JFrame();
			final Container contentPane = frame.getContentPane();
			contentPane.setLayout(new BorderLayout());
			contentPane.add(panel, BorderLayout.NORTH);
			frame.pack();
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

}
