/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the Unlicense for details:
 *     https://unlicense.org/
 */

import ij.ImagePlus;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImageJ;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * A very simple plugin.
 * <p>
 * The annotation {@code @Plugin} lets ImageJ know that this is a plugin. There
 * are a vast number of possible plugins; {@code Command} plugins are the most
 * common one: they take inputs and produce outputs.
 * </p>
 * <p>
 * A {@link Command} is most useful when it is bound to a menu item; that is
 * what the {@code menuPath} parameter of the {@code @Plugin} annotation does.
 * </p>
 * <p>
 * Each input to the command is specified as a field with the {@code @Parameter}
 * annotation. Each output is specified the same way, but with a
 * {@code @Parameter(type = ItemIO.OUTPUT)} annotation.
 * </p>
 * 
 * @author Johannes Schindelin
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, headless = true, menuPath = "Analyze >Identify Neurons")
public class IdentifyService implements Command {

	@Parameter
	private LogService logService;

	@Parameter
	private DatasetIOService datasetIOService;

	@Parameter
	private ImagePlus imagePlus;

	@Parameter(type = ItemIO.OUTPUT)
	private Dataset image;

	/**
	 * Produces an output with the well-known "Hello, World!" message. The
	 * {@code run()} method of every {@link Command} is the entry point for
	 * ImageJ: this is what will be called when the user clicks the menu entry,
	 * after the inputs are populated.
	 */
	@Override
	public void run() {

		System.out.println(Arrays.toString(imagePlus.getDimensions(false)));

	}

	/**
	 * A {@code main()} method for testing.
	 * <p>
	 * When developing a plugin in an Integrated Development Environment (such as
	 * Eclipse or NetBeans), it is most convenient to provide a simple
	 * {@code main()} method that creates an ImageJ context and calls the plugin.
	 * </p>
	 * <p>
	 * In particular, this comes in handy when one needs to debug the plugin:
	 * after setting one or more breakpoints and populating the inputs (e.g. by
	 * calling something like
	 * {@code ij.command().run(MyPlugin.class, "inputImage", myImage)} where
	 * {@code inputImage} is the name of the field specifying the input) debugging
	 * becomes a breeze.
	 * </p>
	 * 
	 * @param args unused
	 */
	public static void main(final String... args) {
		// Launch ImageJ as usual.
		final ImageJ ij = new ImageJ();
		ij.launch(args);

		// Launch our "Hello World" command right away.
		//ij.command().run(IdentifyService.class, true);
	}

}
