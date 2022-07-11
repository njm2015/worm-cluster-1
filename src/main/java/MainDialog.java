/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the Unlicense for details:
 *     https://unlicense.org/
 */

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.StackMaker;
import ij.plugin.ZProjector;
import ij.plugin.Zoom;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackProcessor;
import io.scif.formats.ScancoISQFormat;
import loci.formats.FormatException;
import loci.plugins.BF;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;

import ij.IJ;
import ij.ImagePlus;

public class MainDialog extends JDialog {

    @Parameter
    private OpService ops;

    @Parameter
    private LogService log;

    @Parameter
    private StatusService status;

    @Parameter
    private CommandService cmd;

    @Parameter
    private ThreadService thread;

    @Parameter
    private UIService ui;

    @Parameter
    private ImageJ ij;

    private static Integer curIndex;

    private static List<ImagePlus> imp;

    private static MetadataList mdl;

    private File metadataFile;

    private OvalRoi roi;

    private final JPanel contentPanel = new JPanel();

    private final JTextField fileField = new JTextField(30);

    private final static JTextField slicesInfo = new JTextField(5);
    private final JLabel fileInfo = new JLabel();
    private final Checkbox cb = new Checkbox();
    private static Rectangle origRectangle;
    private final static JButton zoom = new JButton();

    DefaultTableModel model = new DefaultTableModel() {

        String[] columnNames = {"Name", "Area", "Mean", "Min", "Max"};

        @Override
        public String getColumnName(int index) { return columnNames[index]; }

        @Override
        public int getColumnCount() { return columnNames.length; }

    };

    private final JTable results = new JTable(model);

    /**
     * Create the dialog.
     */
    public MainDialog(final Context ctx, final BlockingQueue<Runnable> mainThread) {

        ctx.inject(this);
        setBounds(100, 100, 550, 700);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);;

        JPanel filePanel = new JPanel();
        filePanel.setPreferredSize(new Dimension(500, 50));
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.add(fileField);

        JButton chooseFileButton = new JButton("Choose File");
        filePanel.add(chooseFileButton);
        fileField.setMaximumSize(new Dimension(400, 26));
        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser jfc = new JFileChooser();
                int success = jfc.showOpenDialog(contentPanel);

                if (success == JFileChooser.APPROVE_OPTION) {
                    metadataFile = jfc.getSelectedFile();
                    fileField.setText(metadataFile.getAbsolutePath());

                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        mdl = mapper.readValue(new File(metadataFile.getAbsolutePath()), MetadataList.class);

                        if (mdl.data.length > 0) {
                            curIndex = 0;
                            openImagePlus(mdl.data[curIndex]);
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (FormatException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        });

        JPanel controlPanel = new JPanel();
//        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
        controlPanel.setLayout(new FlowLayout());
        controlPanel.setMaximumSize(new Dimension(450, 50));

        JButton playAll = new JButton("Play All");
        playAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                PlayAllWorker worker = new PlayAllWorker();
                worker.execute();
            }
        });

        JButton playSlices = new JButton("Play Slices");
        playSlices.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                PlaySlicesWorker worker = new PlaySlicesWorker();
                worker.execute();
            }
        });

        JButton prevImage = new JButton("Prev");
        prevImage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                curIndex = curIndex + mdl.data.length - 1;
                curIndex %= mdl.data.length;

                try {
                    openImagePlus(mdl.data[curIndex]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (FormatException e) {
                    throw new RuntimeException(e);
                }

            }
        });

        JButton nextImage = new JButton("Next");
        nextImage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ++curIndex;
                curIndex %= mdl.data.length;

                try {
                    openImagePlus(mdl.data[curIndex]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (FormatException e) {
                    throw new RuntimeException(e);
                }

            }
        });

        JButton copy = new JButton("Copy");
        copy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                copy();
            }
        });

        controlPanel.add(playAll);
        controlPanel.add(playSlices);
        controlPanel.add(prevImage);
        controlPanel.add(nextImage);
        controlPanel.add(copy);

        JPanel roiPanel = new JPanel();
//        roiPanel.setLayout(new BoxLayout(roiPanel, BoxLayout.X_AXIS));
        roiPanel.setLayout(new FlowLayout());
        roiPanel.setMaximumSize(new Dimension(450, 50));

        zoom.setText("Zoom to ROI");
        zoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (zoom.getText() == "Zoom to ROI") {
                    ZoomInWorker worker = new ZoomInWorker();
                    worker.execute();
                } else {
                    ZoomOutWorker worker = new ZoomOutWorker();
                    worker.execute();
                }
            }
        });

        JButton deleteRoi = new JButton("Delete ROI");
        deleteRoi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                imp.get(0).deleteRoi();
            }
        });

        JButton stackRoi = new JButton("Stack ROI");
        stackRoi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                stackRoi();
            }
        });

        roiPanel.add(zoom);
        roiPanel.add(deleteRoi);
        roiPanel.add(stackRoi);

        JPanel infoPanel = new JPanel();
        infoPanel.setBounds(0, 0, 100, 80);
        infoPanel.setLayout(new GridLayout(3,2,2,2));
        infoPanel.add(new JLabel("Filename", JLabel.CENTER));
        fileInfo.setText("--");
        fileInfo.setHorizontalAlignment(JLabel.CENTER);
        infoPanel.add(fileInfo);
        infoPanel.add(new JLabel("ROI Slices", JLabel.CENTER));
        slicesInfo.setHorizontalAlignment(JLabel.CENTER);
        slicesInfo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateSlices();
            }
        });
        infoPanel.add(slicesInfo);
        infoPanel.add(new JLabel("Show ROI", JLabel.CENTER));


        JPanel checkpanel = new JPanel();
        checkpanel.setMaximumSize(new Dimension(25, 25));
        cb.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED)
                    imp.get(0).setRoi(roi);
                else
                    imp.get(0).deleteRoi();
            }
        });
        checkpanel.add(cb);
        infoPanel.add(checkpanel, JPanel.CENTER_ALIGNMENT);

        infoPanel.setMaximumSize(new Dimension(400, 70));

        JPanel topSpacerPanel = new JPanel();
        topSpacerPanel.setLayout(new BoxLayout(topSpacerPanel, BoxLayout.X_AXIS));
        topSpacerPanel.setMaximumSize(new Dimension(400, 50));

        JPanel spacerPanel = new JPanel();
        spacerPanel.setLayout(new BoxLayout(spacerPanel, BoxLayout.X_AXIS));
        spacerPanel.setMaximumSize(new Dimension(400, 120));

        JPanel tableAboveSpacer = new JPanel();
        tableAboveSpacer.setMaximumSize(new Dimension(400, 100));

        JScrollPane tablePane = new JScrollPane(results);
        tablePane.setMaximumSize(new Dimension(450, 100));

        contentPanel.add(filePanel, JPanel.CENTER_ALIGNMENT);
        contentPanel.add(topSpacerPanel, JPanel.CENTER_ALIGNMENT);
        contentPanel.add(infoPanel, JPanel.CENTER_ALIGNMENT);
        contentPanel.add(roiPanel, JPanel.CENTER_ALIGNMENT);
        contentPanel.add(spacerPanel, JPanel.CENTER_ALIGNMENT);
        contentPanel.add(controlPanel, JPanel.CENTER_ALIGNMENT);
        contentPanel.add(tableAboveSpacer, JPanel.CENTER_ALIGNMENT);
        contentPanel.add(tablePane, JPanel.CENTER_ALIGNMENT);

    }

    protected void updateSlices() {

        try {
            Scanner sliceScanner = new Scanner(slicesInfo.getText()).useDelimiter("-");
            int startIdx = sliceScanner.nextInt();
            imp.get(0).setPosition(startIdx * 2 + 1);
            imp.get(0).setC(1);
            imp.get(0).setDisplayRange(0, 0);
            imp.get(0).show();
        } catch (Exception e) {

        }

    }

    protected void copy() {

        StringBuilder resStr = new StringBuilder();

        for (int i = 0; i < results.getRowCount(); ++i) {

            StringBuilder tStr = new StringBuilder();

            for (int j = 0; j < results.getColumnCount(); ++j)
                tStr.append(results.getValueAt(i, j)).append("\t");

            tStr.deleteCharAt(tStr.lastIndexOf("\t"));
            resStr.append(tStr.append("\n"));
        }
        resStr.deleteCharAt(resStr.lastIndexOf("\n"));

        StringSelection stsel = new StringSelection(resStr.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);

    }

    protected void stackRoi() {

        Scanner sliceScanner = new Scanner(slicesInfo.getText()).useDelimiter("-");
        int startIdx = sliceScanner.nextInt();
        int endIdx = sliceScanner.nextInt();

        ZProjector zp = new ZProjector(imp.get(0));
        ImagePlus t_imp = ZProjector.run(imp.get(0), "sum", startIdx, endIdx);
        t_imp.setRoi(imp.get(0).getRoi());
        ImageStatistics stats = t_imp.getAllStatistics();

        Metadata mtda = mdl.data[curIndex];
        String truncFname = mtda.filename.substring(mtda.filename.lastIndexOf('/') + 1);
        saveResults(
                truncFname,
                String.format("%.3f", stats.area),
                String.format("%.3f", stats.mean),
                stats.min,
                stats.max
        );

    }

    protected void saveResults(String filename, String area, String mean, double min, double max) {

        int i;
        for (i = 0; i < results.getRowCount(); ++i) {
            if (results.getValueAt(i, 0).equals(filename))
                break;
        }

        if (i >= results.getRowCount()) {
            ((DefaultTableModel) results.getModel()).addRow(new Object[] {filename, area, mean, min, max});
        } else {

            results.setValueAt(filename, i, 0);
            results.setValueAt(area, i, 1);
            results.setValueAt(mean, i, 2);
            results.setValueAt(min, i, 3);
            results.setValueAt(max, i, 4);

        }

    }

    protected void openImagePlus(Metadata mtda) throws IOException, FormatException {

        if (imp != null && imp.size() > 0)
            imp.forEach(ImagePlus::close);

        slicesInfo.setText(String.format("%d-%d", mtda.getSliceList().get(0) - 1, mtda.getSliceList().get(mtda.getSliceList().size() - 1) + 3));
        fileInfo.setText(mtda.filename.substring(mtda.filename.lastIndexOf('/') + 1));

        imp = Arrays.asList(BF.openImagePlus(mtda.filename));
        List<Integer> box = mtda.getBoxList();
//        roi = new Roi(box.get(0), box.get(2), box.get(1) - box.get(0), box.get(3) - box.get(2));
        roi = new OvalRoi(box.get(0), box.get(2), box.get(1) - box.get(0), box.get(3) - box.get(2));
        roi.setStrokeColor(Color.RED);
        roi.setStrokeWidth(2.0);
        imp.get(0).setRoi(roi);

        cb.setState(true);

        List<Integer> slices = mtda.getSliceList();
        imp.get(0).setPosition(slices.get(slices.size() / 2) * 2 + 1);
        imp.get(0).setC(1);
        imp.get(0).setDisplayRange(0, 0);
        imp.get(0).show();

    }

    static class PlaySlicesWorker extends SwingWorker<Integer, Integer> {

        protected Integer doInBackground() throws InterruptedException {

            Scanner sliceScanner = new Scanner(slicesInfo.getText()).useDelimiter("-");
            int startIdx = sliceScanner.nextInt();
            int endIdx = sliceScanner.nextInt();
            int middleIdx = (startIdx + endIdx) / 2;

            for (int i = 0; i < 4; ++i) {
                for (int j = startIdx; j < endIdx; ++j) {
                    imp.get(0).setPosition(j * 2 + 1);
                    imp.get(0).setC(1);
                    imp.get(0).setDisplayRange(0, 0);
//                    imp.get(0).show();
                    imp.get(0).updateImage();
                    TimeUnit.MILLISECONDS.sleep(250);

                }
            }

            imp.get(0).setPosition(middleIdx * 2 + 1);
            imp.get(0).setC(1);
            imp.get(0).setDisplayRange(0, 0);
            imp.get(0).updateImage();

            return 0;

        }
    }

    static class PlayAllWorker extends SwingWorker<Integer, Integer> {

        protected Integer doInBackground() throws InterruptedException {

            Metadata mtda = mdl.data[curIndex];
            List<Integer> slices = mtda.getSliceList();
            int middleIdx = slices.size() / 2;

            for (int j = 0; j < imp.get(0).getNSlices(); ++j) {
                imp.get(0).setPosition(j * 2 + 1);
                imp.get(0).setC(1);
                imp.get(0).setDisplayRange(0, 0);
//                    imp.get(0).show();
                imp.get(0).updateImage();
                TimeUnit.MILLISECONDS.sleep(30);

            }

            imp.get(0).setPosition(slices.get(middleIdx) * 2 + 1);
            imp.get(0).setC(1);
            imp.get(0).setDisplayRange(0, 0);
            imp.get(0).updateImage();

            return 0;

        }
    }

    static class ZoomInWorker extends SwingWorker<Integer, Integer> {

        protected Integer doInBackground() {

           Integer padding = 200;
           List<Integer> box = mdl.data[curIndex].getBoxList();
           ImageCanvas canvas = imp.get(0).getCanvas();
           origRectangle = new Rectangle(canvas.getSrcRect());
           canvas.setSourceRect(new Rectangle(box.get(0) - padding, box.get(2) - padding, box.get(1) - box.get(0) + (2 * padding), box.get(3) - box.get(2) + (2 * padding)));
           canvas.setImageUpdated();
           imp.get(0).updateAndRepaintWindow();

           zoom.setText("Zoom Out");

           return 0;

        }
    }

    static class ZoomOutWorker extends SwingWorker<Integer, Integer> {

        protected Integer doInBackground() {

            ImageCanvas canvas = imp.get(0).getCanvas();
//            canvas.setMagnification(0.0);
            canvas.setSourceRect(origRectangle);
//            canvas.setSourceRect(new Rectangle(box.get(0) - padding, box.get(2) - padding, box.get(1) - box.get(0) + (2 * padding), box.get(3) - box.get(2) + (2 * padding)));
            canvas.setImageUpdated();
            imp.get(0).updateAndRepaintWindow();

            zoom.setText("Zoom to ROI");

            return 0;

        }
    }

}
