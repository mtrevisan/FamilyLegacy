package io.github.mtrevisan.familylegacy.v2.ui.tools.files.exportation;

import io.github.mtrevisan.familylegacy.v2.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


/**
 * Exports the current model to a FLEF file.
 * <p>
 * The export is a model-level operation: it serializes the current
 * model and writes it to the chosen file. It does not change the
 * current document, so the user can export a snapshot of their work
 * without committing it as the active file.
 */
public final class ExportFlefTool implements ToolOperation{


	@Override
	public String getName(){
		return "FLEF File…";
	}

	@Override
	public void run(final ToolContext context){
		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Export FLEF file");
		chooser.setFileFilter(new FileNameExtensionFilter("FLEF files (*.flef)", "flef"));
		if(chooser.showSaveDialog(context.owner()) != JFileChooser.APPROVE_OPTION)
			return;

		File target = chooser.getSelectedFile();
		if(!target.getName().toLowerCase().endsWith(".flef"))
			target = new File(target.getParentFile(), target.getName() + ".flef");

		if(target.exists()){
			final int confirm = JOptionPane.showConfirmDialog(context.owner(),
				"The file already exists. Overwrite it?",
				"Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(confirm != JOptionPane.YES_OPTION)
				return;
		}

		try{
			final String content = FLEFWriter.create().writeToString(context.model());
			Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
		}
		catch(final IOException ex){
			JOptionPane.showMessageDialog(context.owner(),
				"Unable to write the file:\n" + ex.getMessage(),
				"Export Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}
