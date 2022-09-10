/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import vavi.awt.dnd.BasicDTListener;
import vavi.util.Debug;


/**
 * Main.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-09-08 nsano initial version <br>
 */
public class Main {

    /**
     * @param args none
     */
    public static void main(String[] args) throws Exception {

        Main app = new Main();
        app.gui();
        if (args.length > 0) {
            Path p = Paths.get(args[0]);
            if (Files.exists(p)) {
                app.init(p);
            }
        }
    }

    void init(Path path) throws IOException {
        images.clear();
        cache.clear();
        index = 0;
        if (es != null && !es.isTerminated()) {
            es.shutdownNow();
        }

        URI uri = URI.create("archive:" + path.toUri());
Debug.println(uri);
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
        Files.list(fs.getRootDirectories().iterator().next())
                .filter(p -> !Files.isDirectory(p))
                .sorted()
                .forEach(images::add);
        panel.repaint();

        es = Executors.newSingleThreadExecutor();
        es.submit(() -> {
           for (int i = 2; i < images.size(); i++) {
               BufferedImage image = cache.get(i);
               if (image == null) {
                   try {
Debug.println("CACHE: " + i + ": " + images.get(i));
                       image = ImageIO.read(Files.newInputStream(images.get(i)));
                       cache.put(i, image);
                   } catch (IOException e) {
                       Debug.println(e.getMessage());
                   }
               }
           }
Debug.println("CACHE: done");
           es.shutdown();
        });
    }

    ExecutorService es;
    JPanel panel;
    List<Path> images = new ArrayList<>();
    int index = 0;
    final Map<Integer, BufferedImage> cache = new HashMap<>();

    void gui() {
        JFrame frame = new JFrame();

        panel = new JPanel() {
            {
                // this is the DnD target sample for a file name from external applications
                new DropTarget(
                        this,
                        DnDConstants.ACTION_COPY_OR_MOVE,
                        new BasicDTListener() {

                            @Override
                            protected boolean isDragFlavorSupported(DropTargetDragEvent ev) {
                                return ev.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
                            }

                            @Override
                            protected DataFlavor chooseDropFlavor(DropTargetDropEvent ev) {
                                if (ev.isLocalTransfer() && ev.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                    return DataFlavor.javaFileListFlavor;
                                }
                                DataFlavor chosen = null;
                                if (ev.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                    chosen = DataFlavor.javaFileListFlavor;
                                }
                                return chosen;
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            protected boolean dropImpl(DropTargetDropEvent ev, Object data) {
                                try {
                                    init(Paths.get(((List<File>) data).get(0).getPath()));
                                    return true;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return false;
                                }
                            }
                        },
                        true);
            }
            public void paint(Graphics g) {
                super.paint(g);

                if (images.size() == 0) return;

                frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                try {
                    BufferedImage i1;
                    BufferedImage i2 = null;
                    i1 = cache.get(index);
                    if (i1 == null) {
Debug.println("RIGHT: " + index + ": " + images.get(index));
                        i1 = ImageIO.read(Files.newInputStream(images.get(index)));
                        synchronized (cache) {
                            cache.put(index, i1);
                        }
                    }
                    if (index + 1 < images.size() - 1) {
                        i2 = cache.get(index + 1);
                        if (i2 == null) {
Debug.println("LEFT : " + (index + 1) + ": " + images.get(index + 1));
                            i2 = ImageIO.read(Files.newInputStream(images.get(index + 1)));
                            synchronized (cache) {
                                cache.put(index + 1, i2);
                            }
                        }
                    }

                    int w = getWidth() / 2;
                    int h = getHeight();
                    int i1w = i1.getWidth();
                    int i1h = i1.getHeight();
                    int i2w = 0;
                    int i2h = 0;
                    if (i2 != null) {
                        i2w = i2.getWidth();
                        i2h = i2.getHeight();
                    }

                    // https://stackoverflow.com/a/10245583
                    float sw = 1;
                    float sh = 1;
                    if (i1w > w) {
                        sw = w / (float) i1w;
                    }
                    if (i1h * sw > h) {
                        sh = h / (float) i1h;
                    }
                    float s = Math.min(sw, sh);
                    int nw1 = (int) (i1w * s);
                    int nh1 = (int) (i1h * s);
                    g.drawImage(i1, w, (h - nh1) / 2, nw1, nh1, null);

                    if (i2 != null) {
                        sw = 1;
                        sh = 1;
                        if (i2w > w) {
                            sw = w / (float) i2w;
                        }
                        if (i2h * sw > h) {
                            sw = h / (float) i2h;
                        }
                        s = Math.min(sw, sh);
                        int nw2 = (int) (i2w * s);
                        int nh2 = (int) (i2h * s);
                        g.drawImage(i2, w - nw2, (h - nh2) / 2, nw2, nh2, null);
                    }
                } catch (IOException e) {
                    Debug.println(e.getMessage());
                }

                frame.setCursor(Cursor.getDefaultCursor());
            }
        };
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int d = (e.getModifiers() & KeyEvent.SHIFT_MASK) != 0 ? 1 : 2;
Debug.println("move: " + d);
                switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (index + d < images.size() - 1) {
                        index += d;
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (index >= d) {
                        index -= d;
                    }
                    break;
                }
                panel.repaint();
Debug.println("index: " + index);
            }
        });
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panel.repaint();
            }
        });
        panel.setBackground(Color.black);
        panel.setPreferredSize(new Dimension(1600, 1200));
        panel.setLayout(new BorderLayout());

        frame.setContentPane(panel);
        frame.setTitle("ComicViewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
