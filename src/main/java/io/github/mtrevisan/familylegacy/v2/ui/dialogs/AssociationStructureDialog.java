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
import io.github.mtrevisan.familylegacy.v2.ui.binding.BindingManager;
import io.github.mtrevisan.familylegacy.v2.ui.binding.BoundTextField;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.ParticipantField;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.AssociationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Serial;
import java.util.List;


/* DONE */
/**
 * Dialog for editing an {@code ASSOCIATION_STRUCTURE} according to FLEF 0.1.1.
 * <p>
 * Structure:
 * <pre>
 * struct {
 *   subject: ResearchTarget
 *   name?: Text
 * }
 *
 * ResearchTarget = oneof {
 *   individual: Xref&lt;IndividualRecord&gt;
 *   group: Xref&lt;GroupRecord&gt;
 *   event: Xref&lt;EventRecord&gt;
 *   event_participation: Xref&lt;EventParticipationRecord&gt;
 *   relationship: Xref&lt;RelationshipRecord&gt;
 *   individual_attribute: Xref&lt;IndividualAttributeRecord&gt;
 *   group_attribute: Xref&lt;GroupAttributeRecord&gt;
 *   place: Xref&lt;PlaceRecord&gt;
 *   place_relationship: Xref&lt;PlaceRelationshipRecord&gt;
 *   source: Xref&lt;SourceRecord&gt;
 *   document: Xref&lt;DocumentRecord&gt;
 *   identity_hypothesis: Xref&lt;IdentityHypothesisRecord&gt;
 *   cultural_norm: Xref&lt;CulturalNormRecord&gt;
 *   historic_event: Xref&lt;HistoricEventRecord&gt;
 *   void: struct { }
 * }
 * </pre>
 */
public class AssociationStructureDialog extends BaseRecordDialog{

	@Serial
	private static final long serialVersionUID = 4646884878766419980L;


	private static final String TAG_TARGET = "TARGET";
	private static final String TAG_NAME = "NAME";


	static{
		HandlerRegistry.register(new AssociationHandler());
		HandlerRegistry.register(new IndividualHandler());
		HandlerRegistry.register(new GroupHandler());
		HandlerRegistry.register(new EventHandler());
		HandlerRegistry.register(new EventParticipationHandler());
		HandlerRegistry.register(new RelationshipHandler());
		HandlerRegistry.register(new IndividualAttributeHandler());
		HandlerRegistry.register(new GroupAttributeHandler());
		HandlerRegistry.register(new PlaceHandler());
		HandlerRegistry.register(new PlaceRelationshipHandler());
		HandlerRegistry.register(new SourceHandler());
		HandlerRegistry.register(new DocumentHandler());
		HandlerRegistry.register(new IdentityHypothesisHandler());
		HandlerRegistry.register(new CulturalNormHandler());
		HandlerRegistry.register(new HistoricEventHandler());
	}


	private final BindingManager bindingManager = new BindingManager();

	private final ParticipantField targetField;
	private final BoundTextField nameField;


	/**
	 * Creates a new dialog to create a new record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @return	A new dialog instance.
	 */
	public static AssociationStructureDialog createNew(final Dialog parent, final FLEFModel model){
		return new AssociationStructureDialog(parent, model, null);
	}

	/**
	 * Creates a new dialog to edit an existing record.
	 *
	 * @param parent	The parent window.
	 * @param model	The FLEF model.
	 * @param record	The record to edit (must not be {@code null}).
	 * @return	A new dialog instance.
	 */
	public static AssociationStructureDialog createEdit(final Dialog parent, final FLEFModel model,
			final FLEFRecord record){
		if(record == null)
			throw new IllegalArgumentException("Record cannot be null");

		return new AssociationStructureDialog(parent, model, record);
	}


	private AssociationStructureDialog(final Dialog parent, final FLEFModel model, final FLEFRecord record){
		super(parent, model, record, HandlerRegistry.getHandler(AssociationHandler.TYPE));

		targetField = ParticipantField.create(TAG_TARGET, this, model);
		targetField.setHandlerTypes(List.of(IndividualHandler.TYPE, GroupHandler.TYPE, EventHandler.TYPE,
			EventParticipationHandler.TYPE, RelationshipHandler.TYPE, IndividualAttributeHandler.TYPE,
			GroupAttributeHandler.TYPE, PlaceHandler.TYPE, PlaceRelationshipHandler.TYPE, SourceHandler.TYPE,
			DocumentHandler.TYPE, IdentityHypothesisHandler.TYPE, CulturalNormHandler.TYPE, HistoricEventHandler.TYPE));
		targetField.addChangeListener(e -> updateNameFieldState());
		nameField = new BoundTextField(TAG_NAME, 30);


		initComponents();

		loadData();

		pack();

		setLocationRelativeTo(parent);
	}

	private void updateNameFieldState(){
		final boolean isVoid = isTargetVoid();
		nameField.setEnabled(isVoid);
		if(!isVoid)
			nameField.setText(StringUtils.EMPTY);
	}

	private boolean isTargetVoid(){
		return !targetField.hasData();
	}


	private void initComponents(){
		bindingManager.bind(nameField);

		setLayout(new MigLayout("ins 10,fillx,top", "[right]rel[grow]", "[]5[]10[]"));

		// target
		add(new JLabel("Target*:"), "align label");
		add(targetField, "growx, wrap");

		// name
		add(new JLabel("Name:"), "align label");
		add(nameField, "growx");

		final JPanel buttonPanel = GUIHelper.createButtonPanel(getRootPane(),
			this::save,
			this::dispose);
		add(buttonPanel, BorderLayout.SOUTH);
	}


	@Override
	protected void loadData(){
		targetField.load(record);

		bindingManager.load(record);

		updateNameFieldState();
	}

	@Override
	protected boolean validData(){
		final boolean isVoid = isTargetVoid();

		if(!isVoid && !targetField.hasData()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Target cannot be empty.",
				null, null, targetField);
			targetField.requestFocus();

			return false;
		}

		if(isVoid && nameField.isEmpty()){
			GUIHelper.showValidationErrorAndFocus(this,
				"Name cannot be empty.",
				null, null, nameField);
			targetField.requestFocus();

			return false;
		}

		return true;
	}

	@Override
	protected void saveData(){
		if(isTargetVoid()){
			FLEFRecordHelper.removeChildren(record, TAG_TARGET);

			bindingManager.save(record);
		}
		else{
			FLEFRecordHelper.removeChildren(record, TAG_NAME);

			targetField.save(record);
		}
	}


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();

		SwingUtilities.invokeLater(() -> {
			final AssociationStructureDialog dialog = new AssociationStructureDialog(null, model, null);
			dialog.setVisible(true);
		});
	}

}
