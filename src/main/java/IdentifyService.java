/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the Unlicense for details:
 *     https://unlicense.org/
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import io.scif.services.DatasetIOService;
import loci.formats.FormatException;
import net.imagej.Dataset;
import net.imagej.ImageJ;

import net.imagej.ops.Ops;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import loci.plugins.BF;

import javax.imageio.ImageIO;
import javax.swing.*;

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
@Plugin(type = Command.class, headless = true, menuPath = "Help>Hello")
public class IdentifyService implements Command {

	private ImagePlus[] ip;

//	private Image img;

	@Parameter
	private ImageJ ij;

	private JPanel panel = new JPanel();


	/**
	 * Produces an output with the well-known "Hello, World!" message. The
	 * {@code run()} method of every {@link Command} is the entry point for
	 * ImageJ: this is what will be called when the user clicks the menu entry,
	 * after the inputs are populated.
	 */
	@Override
	public void run() {

		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

		try {
			final MainDialog dialog = new MainDialog(ij.context(), queue);
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		}
		catch (final Exception e) {
			e.printStackTrace();
		}

		while(true) {
			try {
				queue.take().run();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

//		System.out.println("Reading " + metadataFilename);
//
//		MetadataList mdl;
//		ObjectMapper mapper = new ObjectMapper();
//
//		try {
//
//			mdl = mapper.readValue(new File(metadataFilename), MetadataList.class);
//
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//
//		for (int i = 0; i < mdl.data.length; ++i) {
//
//			Metadata t_data = mdl.data[i];
//
//			try {
//				ip = BF.openImagePlus(t_data.filename);
//			} catch (FormatException e) {
//				throw new RuntimeException(e);
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//
//			List<Integer> box = t_data.getBoxList();
//			Roi roi = new Roi(box.get(0), box.get(2), box.get(1) - box.get(0), box.get(3) - box.get(2));
//			roi.setStrokeColor(Color.RED);
//			roi.setStrokeWidth(2.0);
//			ip[0].setRoi(roi);
//
//			List<Integer> slices = t_data.getSliceList();
//			while(true) {
//				for (int j = 0; j < slices.size(); ++j) {
//
//					try {
//
//						ip[0].setPosition(slices.get(j) * 2 + 1);
//						ip[0].setC(1);
//						ip[0].setDisplayRange(0, 0);
//						ip[0].show();
//
//						TimeUnit.MILLISECONDS.sleep(250);
//
//					} catch (Exception e) {
//
//						break;
//
//					}
//
//				}
//			}
//
//		}

//		try {
//
//			ip = BF.openImagePlus("/home/nathaniel/workspace/imagej/worm-cluster-1/data/5.czi");
//
//			for (int i = 0; i < ip[0].getNSlices(); ++i) {
//				ip[0].setPosition(i * 2 + 1);
//				ip[0].setC(1);
//				ip[0].setDisplayRange(0, 0);
//				ip[0].show();
//
//				img = ip[0].getImage();
//				ImageIO.write((RenderedImage) img, "jpg", new File(String.format("/home/nathaniel/workspace/imagej/worm-cluster-1/data/%d.jpg", i)));
//			}
//
//		} catch (FormatException e) {
//			throw new RuntimeException(e);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

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
	public static void main(final String... args) throws IOException, FormatException {
		// Launch ImageJ as usual.
		final ImageJ ij = new ImageJ();
		ij.launch(args);



		// Launch our "Hello World" command right away.
		//ij.command().run(IdentifyService.class, true);
	}

}
