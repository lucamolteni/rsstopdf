package io.triode.rsstopdf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.tinylog.Logger;

public class Latex {

	public void executePdflatex(String texFilePath, File currentDirectory) {
		// Command to execute pdflatex with the file as an argument
		List<String> command = List.of( "pdflatex", texFilePath );

		ProcessBuilder processBuilder = new ProcessBuilder( command );

		// Redirect the process's input/output to the console
		processBuilder.inheritIO();
		processBuilder.directory( currentDirectory );

		try {
			// Start the process
			Process process = processBuilder.start();

			// Wait for the process to complete
			int exitCode = process.waitFor();

			if ( exitCode == 0 ) {
				Logger.info( "pdflatex executed successfully." );
			}
			else {
				Logger.error( "pdflatex execution failed with exit code: " + exitCode );
			}
		}
		catch (IOException | InterruptedException e) {
			Logger.error( e );
		}
	}

	public static void main(String[] args) {
		// Example usage
		String texFilePath = "example.tex";
		new Latex().executePdflatex( texFilePath, new File( "." ) );
	}
}
