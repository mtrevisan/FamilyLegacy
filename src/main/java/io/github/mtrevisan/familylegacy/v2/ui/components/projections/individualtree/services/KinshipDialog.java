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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.fields.EntityField;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeService;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.io.Serial;
import java.util.List;
import java.util.Locale;


/**
 * Modal dialog that computes and displays the kinship between two
 * individuals of a FLEF model.
 * <p>
 * The dialog is opened by the ancestor tree panel through the {@code Ctrl+K}
 * shortcut. The first individual defaults to the current root of the tree;
 * the second is initially empty.
 * <p>
 * Both individuals are chosen through an {@link EntityField}, which provides
 * the standard interaction used elsewhere in the application: right-click on
 * the field to open a context menu with "Set…", "Edit…" and "Clear". The
 * field also displays the current selection using the same formatting as the
 * rest of the application.
 * <p>
 * The dialog is stateless with respect to the application: it receives
 * everything it needs (model, tree service, initial records) at construction
 * time and does not modify the model.
 */
public class KinshipDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 5510483225841003492L;


	private static final String DEFAULT_FONT_FAMILY = "Monospaced";
	private static final int LABEL_COLUMN_WIDTH = 90;


	private final KinshipCalculator calculator;

	private final EntityField fieldA;
	private final EntityField fieldB;

	private final JButton swapButton = new JButton("Swap");
	private final JButton calculateButton = new JButton("Calculate");
	private final JButton closeButton = new JButton("Close");
	private final JTextArea resultArea = new JTextArea();


	/**
	 * Constructor.
	 *
	 * @param owner    the parent window; may be {@code null}
	 * @param model    the FLEF model (must not be {@code null})
	 * @param service  the tree service used to walk the ancestor graph
	 *                 (must not be {@code null})
	 * @param initialA the first individual, or {@code null}
	 * @param initialB the second individual, or {@code null}
	 */
	public KinshipDialog(final Window owner, final FLEFModel model, final TreeService service,
			final FLEFRecord initialA, final FLEFRecord initialB){
		super(owner, "Kinship between two individuals", ModalityType.APPLICATION_MODAL);

		if(model == null)
			throw new IllegalArgumentException("Model must not be null");
		if(service == null)
			throw new IllegalArgumentException("Tree service must not be null");

		this.calculator = new KinshipCalculator(model, service);

		// Create the two entity fields, restricted to Individual records.
		// The path argument is unused here because the dialog never saves
		// anything back to a record; it is part of the EntityField contract
		// and is only consumed by load/save operations that we do not call.
		this.fieldA = EntityField.createForRecordFromReference(null, this, model,
			IndividualHandler.class);
		this.fieldB = EntityField.createForRecordFromReference(null, this, model,
			IndividualHandler.class);

		fieldA.setEntity(initialA);
		fieldB.setEntity(initialB);

		initComponents();
		refreshCalculateButton();

		// React to entity changes to update the button state.
		fieldA.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED, e -> refreshCalculateButton());
		fieldB.addPropertyChangeListener(EntityField.PROPERTY_ENTITY_CHANGED, e -> refreshCalculateButton());

		pack();
		setMinimumSize(new Dimension(720, 480));
		setLocationRelativeTo(owner);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}


	/* ======================================================================
	 *                          UI construction
	 * ====================================================================== */

	private void initComponents(){
		final JPanel main = new JPanel(new MigLayout("ins 12,fill", "[grow]", "[]8[]14[]8[grow]12[]"));

		main.add(buildPickerRow("Individual A:", fieldA), "growx,wrap");
		main.add(buildPickerRow("Individual B:", fieldB), "growx,wrap");

		// Action row: swap + calculate
		final JPanel actions = new JPanel(new MigLayout("ins 0", "[grow][][]", "[]"));
		actions.add(new JLabel(StringUtils.SPACE), "growx");
		actions.add(swapButton);
		actions.add(calculateButton);
		main.add(actions, "growx,wrap");

		// Result area
		resultArea.setEditable(false);
		resultArea.setLineWrap(false);
		resultArea.setFont(new Font(DEFAULT_FONT_FAMILY, Font.PLAIN, 12));
		resultArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		final JScrollPane resultScroll = new JScrollPane(resultArea);
		resultScroll.setBorder(BorderFactory.createTitledBorder("Result"));
		main.add(resultScroll, "grow,push,wrap");

		// Footer: close
		final JPanel footer = new JPanel(new MigLayout("ins 0", "[grow][]", "[]"));
		footer.add(new JLabel(StringUtils.SPACE), "growx");
		footer.add(closeButton);
		main.add(footer, "growx");

		setLayout(new BorderLayout());
		add(main, BorderLayout.CENTER);

		swapButton.addActionListener(e -> onSwap());
		calculateButton.addActionListener(e -> onCalculate());
		closeButton.addActionListener(e -> dispose());

		getRootPane()
			.setDefaultButton(calculateButton);
	}

	private static JPanel buildPickerRow(final String title, final EntityField field){
		final JPanel row = new JPanel(new MigLayout("ins 0", "[" + LABEL_COLUMN_WIDTH + "!][grow,fill]", "[]"));
		row.add(new JLabel(title));
		row.add(field, "growx");
		return row;
	}


	/* ======================================================================
	 *                          Actions
	 * ====================================================================== */

	private void onSwap(){
		final FLEFRecord a = fieldA.getEntity();
		final FLEFRecord b = fieldB.getEntity();
		// The swap is meaningful only when both sides hold a record.
		// The button is disabled otherwise, but the guard is kept for
		// robustness against programmatic invocations.
		if(a == null || b == null)
			return;

		fieldA.setEntity(b);
		fieldB.setEntity(a);
	}

	private void onCalculate(){
		final FLEFRecord a = fieldA.getEntity();
		final FLEFRecord b = fieldB.getEntity();
		if(a == null || b == null)
			return;

		final KinshipResult result = calculator.calculate(a.getId(), b.getId());
		resultArea.setText(formatResult(result));
		resultArea.setCaretPosition(0);
	}

	private void refreshCalculateButton(){
		final boolean bothSet = (fieldA.hasData() && fieldB.hasData());
		calculateButton.setEnabled(bothSet);
		swapButton.setEnabled(bothSet);
	}


	/* ======================================================================
	 *                          Formatting
	 * ====================================================================== */

	private static String formatResult(final KinshipResult result){
		final StringBuilder sb = new StringBuilder();

		// Header
		sb.append("Individual A: ").append(result.displayA()).append('\n');
		sb.append("Individual B: ").append(result.displayB()).append('\n');
		sb.append('\n');

		// Relationship description
		sb.append("Relationship:\n  ").append(result.relationshipDescription()).append('\n');
		sb.append('\n');

		if(result.isSameIndividual())
			return sb.toString();
		if(!result.isRelated()){
			sb.append("No further statistics available.\n");
			return sb.toString();
		}

		// Chain from A to MRCA
		sb.append("Chain from A to the most recent common ancestor:\n");
		appendChain(sb, result.chainA());
		sb.append('\n');

		// Chain from B to MRCA
		sb.append("Chain from B to the most recent common ancestor:\n");
		appendChain(sb, result.chainB());
		sb.append('\n');

		// MRCA and furthest common ancestor
		final KinshipResult.CommonAncestorInfo mrca = result.mrca();
		final KinshipResult.CommonAncestorInfo furthest = result.furthestCommonAncestor();
		sb.append("Most recent common ancestor: ")
			.append(mrca.display())
			.append(" (")
			.append(mrca.distanceFromA()).append(" steps from A, ")
			.append(mrca.distanceFromB()).append(" steps from B)")
			.append('\n');
		sb.append("Furthest common ancestor: ")
			.append(furthest.display())
			.append(" (")
			.append(furthest.distanceFromA()).append(" steps from A, ")
			.append(furthest.distanceFromB()).append(" steps from B)")
			.append('\n');
		sb.append('\n');

		// Statistics
		sb.append("Statistics:\n");
		sb.append("  Total path length (A to B through the MRCA): ")
			.append(result.totalSteps()).append(" steps\n");
		sb.append("  Common ancestors found: ")
			.append(result.allCommonAncestors().size()).append('\n');
		sb.append("  Relationship coefficient (Wright's R): ")
			.append(String.format(Locale.ROOT, "%.5f", result.relationshipCoefficient()))
			.append('\n');
		sb.append('\n');

		// Kinship degrees under the different systems
		sb.append("Kinship degrees:\n");
		appendCivilDegree(sb, result);
		appendCanonicalDegree(sb, result);
		appendGermanicKnee(sb, result);
		appendKoreanChon(sb, result);
		appendChineseGeneration(sb, result);
		sb.append('\n');

		// Marriage prohibitions
		sb.append("Marriage prohibitions:\n");
		appendCivilProhibition(sb, result);
		appendCanonicalProhibition(sb, result);
		appendChineseProhibition(sb, result);
		sb.append('\n');

		// Full list of common ancestors
		sb.append("All common ancestors:\n");
		for(final KinshipResult.CommonAncestorInfo c : result.allCommonAncestors()){
			sb.append("  - ")
				.append(c.display())
				.append(" (dA=")
				.append(c.distanceFromA())
				.append(", dB=")
				.append(c.distanceFromB())
				.append(", contribution=")
				.append(String.format(Locale.ROOT, "%.5f", c.contribution()))
				.append(")\n");
		}

		return sb.toString();
	}

	private static void appendChain(final StringBuilder sb, final List<KinshipResult.ChainEntry> chain){
		for(int i = 0; i < chain.size(); i ++){
			if(i > 0)
				sb.append(" -> ");
			sb.append(chain.get(i).display());
		}
		sb.append('\n');
	}

	private static void appendCivilDegree(final StringBuilder sb, final KinshipResult result){
		sb.append("  Civil / Roman degree (Italy, Germany, Japan): ")
			.append(result.civilDegree())
			.append("°");
		if(result.isCivillyRelevant())
			sb.append(" [within the 6th-degree limit]");
		else if(result.civilDegree() > 6)
			sb.append(" [beyond the 6th-degree limit]");
		sb.append('\n');
	}

	private static void appendCanonicalDegree(final StringBuilder sb, final KinshipResult result){
		sb.append("  Canonical degree (Catholic Church): ")
			.append(result.canonicalDegree())
			.append("°");
		if(result.isCanonicallyRelevant())
			sb.append(" [within the 4th-degree limit]");
		else if(result.canonicalDegree() > 4)
			sb.append(" [beyond the 4th-degree limit]");
		sb.append('\n');
	}

	private static void appendGermanicKnee(final StringBuilder sb, final KinshipResult result){
		sb.append("  Germanic knee number (historical English/Nordic): ")
			.append(result.germanicKnee())
			.append(result.germanicKnee() == 1? " knee": " knees")
			.append(" [same numerical value as the canonical degree]")
			.append('\n');
	}

	private static void appendKoreanChon(final StringBuilder sb, final KinshipResult result){
		sb.append("  Korean chon (촌): ")
			.append(result.koreanChon())
			.append(" chon")
			.append(" [same numerical value as the civil degree]")
			.append('\n');
	}

	private static void appendChineseGeneration(final StringBuilder sb, final KinshipResult result){
		sb.append("  Chinese generation (PRC Marriage Law): ")
			.append(result.chineseGeneration());
		sb.append(result.chineseGeneration() == 1? " generation": " generations");
		if(result.isChineseRelevant())
			sb.append(" [within the 3rd-generation limit for collateral relatives]");
		else if(!result.directLine() && result.chineseGeneration() > 3)
			sb.append(" [beyond the 3rd-generation limit]");
		sb.append('\n');
	}

	private static void appendCivilProhibition(final StringBuilder sb, final KinshipResult result){
		sb.append("  Italian civil law: ");
		if(result.isMarriageProhibitedCivilly()){
			if(result.directLine())
				sb.append("prohibited (direct line)");
			else
				sb.append("prohibited (collateral line, within the 4th degree)");
		}
		else
			sb.append("allowed");
		sb.append('\n');
	}

	private static void appendCanonicalProhibition(final StringBuilder sb, final KinshipResult result){
		sb.append("  Catholic canon law: ");
		if(!result.areOppositeSex())
			sb.append("not applicable (same sex)");
		else if(result.isMarriageProhibitedCanonically()){
			if(result.directLine())
				sb.append("prohibited (direct line, no dispensation possible)");
			else if(result.requiresCanonicalDispensation())
				sb.append("dispensation required (within the 4th canonical degree)");
			else
				sb.append("prohibited");
		}
		else
			sb.append("allowed");
		sb.append('\n');
	}

	private static void appendChineseProhibition(final StringBuilder sb, final KinshipResult result){
		sb.append("  PRC Marriage Law: ");
		if(result.isMarriageProhibitedChinese()){
			if(result.directLine())
				sb.append("prohibited (direct line)");
			else
				sb.append("prohibited (collateral line, within three generations)");
		}
		else
			sb.append("allowed");
		sb.append('\n');
	}

}
