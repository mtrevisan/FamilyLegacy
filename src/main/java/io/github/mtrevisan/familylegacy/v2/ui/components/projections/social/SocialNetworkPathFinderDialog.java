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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.social;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Window;


/**
 * Dialog that asks the user for two entities and returns the chosen ids.
 * <p>
 * The dialog does not perform the search itself: it only collects the two
 * endpoints, which the caller then passes to {@link SocialPathFinder}
 * against the current graph. This keeps the dialog decoupled from the
 * graph instance, so it can be reused across different network
 * reconstructions.
 * <p>
 * Both endpoints are chosen through a {@link RecordSelectionDialog} that
 * allows inline creation, and default to the individual handler. The
 * dialog accepts groups as well: if the user creates a group instead of an
 * individual, the id is still valid because the social network treats both
 * as row entities.
 */
public final class SocialNetworkPathFinderDialog extends JDialog{

	private final FLEFModel model;

	private FLEFRecord fromEntity;
	private FLEFRecord toEntity;
	private boolean accepted;

	private final JLabel fromLabel = new JLabel("(none)");
	private final JLabel toLabel = new JLabel("(none)");
	private final JButton fromBtn = new JButton("Pick…");
	private final JButton toBtn = new JButton("Pick…");
	private final JButton okBtn = new JButton("Find path");
	private final JButton cancelBtn = new JButton("Cancel");


	public SocialNetworkPathFinderDialog(final Window owner, final FLEFModel model, final FLEFRecord initialFromEntity,
			final FLEFRecord initialToEntity){
		super(owner, "Find social path", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.fromEntity = initialFromEntity;
		this.toEntity = initialToEntity;


		initComponents();

		updateLabels();

		pack();
		setLocationRelativeTo(owner);
	}


	/**
	 * Returns the chosen source entity, or {@code null}.
	 */
	public String getFromEntityId(){
		return fromEntity.getId();
	}

	/**
	 * Returns the chosen target entity, or {@code null}.
	 */
	public String getToEntityId(){
		return toEntity.getId();
	}

	/**
	 * Returns whether the user confirmed the search.
	 */
	public boolean isAccepted(){
		return accepted;
	}


	private void initComponents(){
		final JPanel content = new JPanel(new MigLayout("ins 12,wrap 2", "[right][grow,fill]", "[]8[]16[]"));

		content.add(new JLabel("From:"));
		final JPanel fromPanel = new JPanel(new MigLayout("ins 0", "[grow,fill][]", "[]"));
		fromPanel.add(fromLabel, "growx");
		fromPanel.add(fromBtn);
		content.add(fromPanel, "growx");

		content.add(new JLabel("To:"));
		final JPanel toPanel = new JPanel(new MigLayout("ins 0", "[grow,fill][]", "[]"));
		toPanel.add(toLabel, "growx");
		toPanel.add(toBtn);
		content.add(toPanel, "growx");

		final JPanel buttons = new JPanel(new MigLayout("ins 0", "[][]", "[]"));
		buttons.add(okBtn);
		buttons.add(cancelBtn);
		content.add(buttons, "span 2, align right");

		setLayout(new BorderLayout());
		add(content, BorderLayout.CENTER);

		fromBtn.addActionListener(e -> pickFrom());
		toBtn.addActionListener(e -> pickTo());
		okBtn.addActionListener(e -> {
			if(fromEntity == null || toEntity == null || fromEntity.equals(toEntity)){
				JOptionPane.showMessageDialog(this,
					"Please choose two distinct entities.",
					"Invalid selection", JOptionPane.WARNING_MESSAGE);

				return;
			}
			accepted = true;

			dispose();
		});
		cancelBtn.addActionListener(e -> dispose());

		getRootPane()
			.setDefaultButton(okBtn);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private void pickFrom(){
		final FLEFRecord pickedEntity = pickEntity();
		if(pickedEntity != null){
			fromEntity = pickedEntity;

			updateLabels();
		}
	}

	private void pickTo(){
		final FLEFRecord pickedEntity = pickEntity();
		if(pickedEntity != null){
			toEntity = pickedEntity;

			updateLabels();
		}
	}

	private FLEFRecord pickEntity(){
		final FLEFRecord[] result = {null};
		final Window owner = (getOwner() instanceof Window w? w: null);
		@SuppressWarnings("unchecked") final RecordSelectionDialog dialog = RecordSelectionDialog.createWithAllowRecordCreation(
			owner, model,
			(record, handler) -> result[0] = record,
			IndividualHandler.class);
		dialog.setVisible(true);

		final FLEFRecord record = result[0];
		if(record == null)
			return null;

		return record;
	}

	private void updateLabels(){
		fromLabel.setText(fromEntity != null? extractLabel(fromEntity): "(none)");
		toLabel.setText(toEntity != null? extractLabel(toEntity): "(none)");
	}

	private String extractLabel(final FLEFRecord record){
		final String tag = record.getTag();
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(GroupHandler.TYPE.equalsIgnoreCase(tag)
			? GroupHandler.TYPE
			: IndividualHandler.TYPE);
		return (record.getId() != null? handler.getDisplayText(record, model): "?");
	}

}
