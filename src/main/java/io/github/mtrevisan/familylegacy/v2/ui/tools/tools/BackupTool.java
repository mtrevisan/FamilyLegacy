package io.github.mtrevisan.familylegacy.v2.ui.tools.tools;

import io.github.mtrevisan.familylegacy.v2.io.FLEFWriter;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Saves a copy of the current model to a user-chosen file. The name of
 * the file is pre-filled with a timestamp so a sequence of backups
 * never overwrites itself.
 * <p>
 * Backup is a document-level operation: it does not change the current
 * file, it does not clear the dirty flag, and it does not touch the
 * recent files list. The model is written exactly as it is, without
 * running the validator: if the model has errors, they are preserved,
 * so a backup can be used to recover from a bad edit.
 */
public final class BackupTool implements ToolOperation{

	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");


	@Override
	public String getName(){
		return "Backup…";
	}

	@Override
	public void run(final ToolContext context){
		if(context == null)
			return;

		final FLEFModel model = context.model();
		if(model == null){
			JOptionPane.showMessageDialog(context.owner(),
				"No model loaded.", "Backup", JOptionPane.WARNING_MESSAGE);

			return;
		}

		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Save backup");
		chooser.setFileFilter(new FileNameExtensionFilter("FLEF backup (*.flef)", "flef"));
		chooser.setSelectedFile(new File("backup-" + TS.format(LocalDateTime.now()) + ".flef"));
		if(chooser.showSaveDialog(context.owner()) != JFileChooser.APPROVE_OPTION)
			return;

		File target = chooser.getSelectedFile();
		if(!target.getName().toLowerCase().endsWith(".flef"))
			target = new File(target.getParentFile(), target.getName() + ".flef");

		if(target.exists()){
			final int choice = JOptionPane.showConfirmDialog(context.owner(),
				"The file already exists. Overwrite it?",
				"Backup", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(choice != JOptionPane.YES_OPTION)
				return;
		}

		try{
			final String content = FLEFWriter.createCompact()
				.writeToString(model);
			Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);

			JOptionPane.showMessageDialog(context.owner(),
				"Backup saved to:\n" + target.getAbsolutePath(),
				"Backup", JOptionPane.INFORMATION_MESSAGE);
		}
		catch(final IOException ex){
			JOptionPane.showMessageDialog(context.owner(),
				"Unable to save the backup:\n" + ex.getMessage(),
				"Backup", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel());
	}

}
