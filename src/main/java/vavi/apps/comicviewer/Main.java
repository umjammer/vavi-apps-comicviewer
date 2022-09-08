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
import java.awt.image.BufferedImageOp;
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
import vavi.awt.image.resample.FfmpegResampleOp;
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

                    final int m = 2;
                    switch (m) {
                    case 1: {
                        // TODO when window is larger than image
                        float scale;

                        float s1;
                        float sx1 = (float) i1w / w;
                        sx1 = sx1 > 1 ? 1 / sx1 : sx1;
                        float sy1 = (float) i1h / h;
                        sy1 = sy1 > 1 ? 1 / sy1 : sy1;
                        s1 = Math.min(sx1, sy1);
                        scale = s1;
//Debug.println("s1: " + s1);
                        float s2;
                        if (i2 != null) {
                            float sx2 = (float) i2w / w;
                            sx2 = sx2 > 1 ? 1 / sx2 : sx2;
                            float sy2 = (float) i2h / h;
                            sy2 = sy2 > 1 ? 1 / sy2 : sy2;
                            s2 = Math.min(sx2, sy2);
//Debug.println("s2: " + s2);
                            scale = Math.min(s1, s2);
                        }
                        Debug.println("scale: " + scale);
                        int w1 = (int) (i1w * scale);
                        int h1 = (int) (i1h * scale);
                        g.drawImage(i1, w, (h - h1) / 2, w1, h1, null);
                        if (i2 != null) {
                            int w2 = (int) (i2w * scale);
                            int h2 = (int) (i2h * scale);
                            g.drawImage(i2, w - w2, (h - h2) / 2, w2, h2, null);
                        }
                        break;
                    }
                    case 2: {
                        // https://stackoverflow.com/a/10245583
                        int nw1 = i1w;
                        int nh1 = i1h;
                        if (i1w > w) {
                            nw1 = w;
                            nh1 = (int) (i1h * nw1 / (float) i1w);
                        }
                        if (nh1 > h) {
                            nh1 = h;
                            nw1 = (int) (nh1 * i1w / (float) i1h);
                        }
//                        if (nh1 < h) {
//                            nh1 = h;
//                            nw1 = (int) (i1w * nh1 / (float) i1h);
//                        }
//                        if (i1w < w) {
//                            nw1 = w;
//                            nh1 = (int) (i1h * nw1 / (float) i1w);
//                        }
                        g.drawImage(i1, w, (h - nh1) / 2, nw1, nh1, null);
                        if (i2 != null) {
                            int nw2 = i2w;
                            int nh2 = i2h;
                            if (i2w > w) {
                                nw2 = w;
                                nh2 = (int) (nw2 * i2h / (float) i2w);
                            }
                            if (nh2 > h) {
                                nh2 = h;
                                nw2 = (int) (nh2 * i2w / (float) i2h);
                            }
                            g.drawImage(i2, w - nw2, (h - nh2) / 2, nw2, nh2, null);
                        }
                        break;
                    }
                    case 3: {
                        // aspect ratio
                        // https://stackoverflow.com/a/30494623
                        // TODO w:h is not correct, getWidth/Height are wired
                        AspectRatio ar1 = new AspectRatio(i1w, i1h);
                        int nh1 = ar1.getWidth(w);
                        int nw1 = ar1.getHeight(nh1);
                        if (nh1 > h) {
                            nw1 = ar1.getHeight(h);
                            nh1 = ar1.getWidth(nw1);
                        }
                        g.drawImage(i1, w, (h - nh1) / 2, nw1, nh1, null);
                        if (i2 != null) {
                            AspectRatio ar2 = new AspectRatio(i2w, i2h);
                            int nh2 = ar2.getWidth(w);
                            int nw2 = ar2.getHeight(nh2);
                            if (nh2 > h) {
                                nw2 = ar2.getHeight(h);
                                nh2 = ar2.getWidth(nw2);
                            }
                            g.drawImage(i2, w - nw2, (h - nh2) / 2, nw2, nh2, null);
                        }
                        break;
                    }
                    case 4: {
                        // aspect ratio, ffmpeg
                        // https://stackoverflow.com/a/30494623
                        // TODO w:h is not correct, getWidth/Height are wired
                        AspectRatio ar1 = new AspectRatio(i1w, i1h);
                        float scale;
                        int nh1 = ar1.getWidth(w);
                        int nw1 = ar1.getHeight(nh1);
                        scale = (float) nw1 / i1w;
                        if (nh1 > h) {
                            nw1 = ar1.getHeight(h);
                            nh1 = ar1.getWidth(nw1);
                            scale = (float) nh1 / i1h;
                        }
                        BufferedImageOp filter = new FfmpegResampleOp(scale, scale);
                        g.drawImage(filter.filter(i1, null), w, (h - nh1) / 2, null);
                        if (i2 != null) {
                            AspectRatio ar2 = new AspectRatio(i2w, i2h);
                            int nh2 = ar2.getWidth(w);
                            int nw2 = ar2.getHeight(nh2);
                            scale = (float) nw2 / i2w;
                            if (nh2 > h) {
                                nw2 = ar2.getHeight(h);
                                nh2 = ar2.getWidth(nw2);
                                scale = (float) nh2 / i2h;
                            }
                            filter = new FfmpegResampleOp(scale, scale);
                            g.drawImage(filter.filter(i2, null), w - nw2, (h - nh2) / 2, null);
                        }
                        break;
                    }
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

    static class AspectRatio {
        private final double ratio;

        AspectRatio(int x, int y) {
            this.ratio = (double) x / y;
        }

        int getHeight(int length) {
            double height = length / Math.sqrt((Math.pow(ratio, 2d) + 1));
            return Math.round((float) height);
        }

        int getWidth(int length) {
            double width = length / Math.sqrt(1d / (Math.pow(ratio, 2d) + 1));
            return Math.round((float) width);
        }
    }
}
