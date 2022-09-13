/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.prefs.Preferences;
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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
Debug.println("shutdownHook");
            app.prefs.putInt("lastIndex", app.index);
            app.prefs.putInt("lastX", app.frame.getX());
            app.prefs.putInt("lastY", app.frame.getY());
            app.prefs.putInt("lastWidth", Math.max(app.panel.getWidth(), 160));
            app.prefs.putInt("lastHeight", Math.max(app.panel.getHeight(), 120));
            app.prefs.put("lastPath", String.valueOf(app.path));
        }));
        // create and display a simple jframe
        app.gui();
        if (args.length > 0) {
            Path p = Paths.get(args[0]);
            if (Files.exists(p)) {
                app.init(p, 0);
            }
            } else {
                String p = app.prefs.get("lastPath", null);
                if (p != null) {
                    Path path = Paths.get(p);
                    if (Files.exists(path)){
                        int index = app.prefs.getInt("lastIndex", 0);
                        app.init(path, index);
                    }
                }
            }
        }

    static final String[] archiveExts = {"zip", "cbz", "rar", "lha", "cab", "7z", "arj"};

    static String getExt(Path path) {
        String filename = path.getFileName().toString();
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    static boolean isArchive(Path path) {
        return !Files.isDirectory(path) && Arrays.asList(archiveExts).contains(getExt(path));
    }

    static final String[] imageExts = {"avif", "jpg", "jpeg", "png", ""};

    static boolean isImage(Path path) {
Debug.println(Level.FINE, "path: " + path.getFileName() + (Files.isDirectory(path) ? "" : ", " + getExt(path)));
        return !Files.isDirectory(path) && Arrays.asList(imageExts).contains(getExt(path));
    }

    void init(Path path, int index) throws IOException {
        this.path = path;
        this.index = index;

        images.clear();
        cache.clear();
        if (es != null && !es.isTerminated()) {
            es.shutdownNow();
            while (!es.isTerminated()) Thread.yield();
        }

        Path virtualRoot;
        if (!isArchive(path)) {
            virtualRoot = path;
        } else {
            URI uri = URI.create("archive:" + path.toUri());
Debug.println("open fs: " + uri);
            if (fs != null) {
                fs.close();
            }
            fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
            virtualRoot = fs.getRootDirectories().iterator().next();
        }
Debug.println(virtualRoot);
        Files.walk(virtualRoot)
                .filter(Main::isImage)
                .sorted()
                .forEach(images::add);
Debug.println("images: " + images.size());
        frame.setTitle("zzzViewer - " + path.getFileName());
        panel.repaint();

        es = Executors.newSingleThreadExecutor();
        es.submit(() -> {
            // don't disturb before first 2 pages are shown
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            for (int i = index + 2; i < images.size(); i++) {
                getImage(i);
                // create gap for paging
                Thread.yield();
            }
            for (int i = 0; i < index; i++) {
                getImage(i);
                // create gap for paging
                Thread.yield();
            }
Debug.println("CACHE: done");
            es.shutdown();
            if (fs != null) {
               try {
                   fs.close();
Debug.println("close fs");
               } catch (IOException e) {
Debug.println(e);
               }
               fs = null;
           }
        });
    }

    BufferedImage getImage(int i) {
        synchronized (cache) {
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
            return image;
        }
    }

    // app
    Preferences prefs = Preferences.userNodeForPackage(Main.class);
    ExecutorService es;
    FileSystem fs;

    // view-model
    List<Path> images = new ArrayList<>();
    final Map<Integer, BufferedImage> cache = new HashMap<>();
    BufferedImage imageR;
    BufferedImage imageL;
    Rectangle rectR = new Rectangle();
    Rectangle rectL = new Rectangle();
    float scaleL, scaleR;

    // view
    JFrame frame;
    JPanel panel;

    // model
    Path path;
    int index = 0;

    void nextPage(int d) {
        if (index + d < images.size() - 1) {
            index += d;
        }
    }

    void prevPage(int d) {
        if (index >= d) {
            index -= d;
        }
    }

    void gui() {
        int x = prefs.getInt("lastX", 0);
        int y = prefs.getInt("lastY", 0);
        int w = prefs.getInt("lastWidth", 1600);
        int h = prefs.getInt("lastHeight", 1200);

        frame = new JFrame();
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int d = e.isShiftDown() ? 1 : 2;
Debug.println("move: " + d);
                switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    nextPage(d);
                    break;
                case KeyEvent.VK_RIGHT:
                    prevPage(d);
                    break;
                case KeyEvent.VK_N:
                    if (e.isControlDown()) {
                        nextPage(d);
                    }
                    break;
                case KeyEvent.VK_P:
                    if (e.isControlDown()) {
                        prevPage(d);
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

        float magnitude = 10;
        JPanel magnify = new JPanel() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);

                int x = getX() + getWidth() / 2;
                int y = getY() + getHeight() / 2;
                int w = (int) (getWidth() / magnitude);
                int h = (int) (getHeight() / magnitude);
//Debug.printf("magnify: %d, %d %d, %d", x, y, w, h);
                BufferedImage image = null;
                Rectangle r = null;
                float s = 0;
                if (rectL.contains(x, y)) {
                    image = imageL;
                    r = rectL;
                    s = scaleL;
                } else if (rectR.contains(x, y)) {
                    image = imageR;
                    r = rectR;
                    s = scaleR;
                };
                if (image != null) {
                    int iw = (int) (w / s);
                    int ih = (int) (h / s);
                    int ix = (int) ((x - r.x - iw / 2) / s);
                    int iy = (int) ((y - r.y - ih / 2) / s);
//Debug.printf("sub: %d, %d %d, %d", ix, iy, iw, ih);
                    // TODO cropping out of bounds
                    BufferedImage sub = image.getSubimage(ix, iy, iw, ih);
                    g.setClip(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                    g.drawImage(sub, 0, 0, getWidth(), getHeight(), 0, 0, iw, ih, null);
                }
            }
        };
        magnify.setSize(new Dimension(450, 450));
        magnify.setOpaque(false);
        magnify.setVisible(false);

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
                                    System.setProperty("vavi.util.archive.zip.encoding", "utf-8");
                                    init(Paths.get(((List<File>) data).get(0).getPath()), 0);
                                    return true;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return false;
                                } catch (IllegalArgumentException e) {
                                    if (e.getMessage().equals("MALFORMED")) {
Debug.println("zip reading failure by utf-8, retry using ms932");
                                        try {
                                            System.setProperty("vavi.util.archive.zip.encoding", "ms932");
                                            init(Paths.get(((List<File>) data).get(0).getPath()), 0);
                                            return true;
                                        } catch (IOException f) {
                                            f.printStackTrace();
                                        }
                                    } else {
                                        e.printStackTrace();
                                    }
                                    return false;
                                }
                            }
                        },
                        true);
            }
            void drawText(Graphics g, String text, String fontName, int point, int ratio) {
                Font font = new Font(fontName, Font.PLAIN, point);

                float stroke = point / (float) ratio;

                Graphics2D graphics = (Graphics2D) g;
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                FontRenderContext frc = graphics.getFontRenderContext();

                AttributedString as = new AttributedString(text);
                as.addAttribute(TextAttribute.FONT, font, 0, text.length());
                AttributedCharacterIterator aci = as.getIterator();

                TextLayout tl = new TextLayout(aci, frc);
                float sw = (float) tl.getBounds().getWidth();
                float sh = (float) tl.getBounds().getHeight();
                Shape shape = tl.getOutline(AffineTransform.getTranslateInstance((getWidth() - sw) / 2, (getHeight() - sh) / 2));
                graphics.setColor(Color.black);
                graphics.setStroke(new BasicStroke(stroke));
                graphics.draw(shape);
                graphics.setColor(Color.white);
                graphics.fill(shape);
            }
            // https://stackoverflow.com/a/10245583
            void drawPage(Graphics g, BufferedImage image, boolean right) {
                int w = getWidth() / 2;
                int h = getHeight();
                int iw = image.getWidth();
                int ih = image.getHeight();
                float sw = 1;
                float sh = 1;
                float s;
                if (iw > w || ih > h) {
                    if (iw > w) {
                        sw = w / (float) iw;
                    }
                    if (ih * sw > h) {
                        sh = h / (float) ih;
                    }
                    s = Math.min(sw, sh);
                } else {
                    if (w > iw) {
                        sw = (float) iw / w;
                    }
                    if (ih * sw < h) {
                        sh = (float) ih / h;
                    }
                    s = 1 / Math.max(sw, sh);
                }
                int nw = (int) (iw * s);
                int nh = (int) (ih * s);
                g.drawImage(image, right ? w : w - nw, (h - nh) / 2, nw, nh, null);
                Rectangle r = right ? rectR : rectL;
                r.setBounds(right ? w : w - nw, (h - nh) / 2, nw, nh);
                if (right) scaleR = s; else scaleL = s;
            }
            public void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (images.size() == 0) {
                    String text = "Drop an archive file or a folder here";
                    drawText(g, text, "San Serif", 20, 12);
                    return;
                }

                frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                imageR = getImage(index);
                imageL = null;
                if (index + 1 < images.size() - 1) {
                    imageL = getImage(index + 1);
                }

                if (imageR != null) {
                    drawPage(g, imageR, true);
                }
                if (imageL != null) {
                    drawPage(g, imageL, false);
                }

                frame.setCursor(Cursor.getDefaultCursor());
            }
        };
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isMetaDown()) {
                    // start magnify
//Debug.printf("mousePressed: %d, %d", e.getX(), e.getY());
                    magnify.setLocation(e.getX() - magnify.getWidth() / 2, e.getY() - magnify.getHeight() / 2);
                    magnify.setVisible(true);
                    magnify.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // end magnify
//Debug.printf("mouseReleased: %d, %d", e.getX(), e.getY());
                magnify.setVisible(false);
                magnify.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int d = e.isShiftDown() ? 1 : 2;
                if (rectL.contains(e.getX(), e.getY())) {
                    nextPage(d);
                } else if (rectR.contains(e.getX(), e.getY())) {
                    prevPage(d);
                };
                panel.repaint();
            }
        });
        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
//Debug.printf("mouseDragged: %d, %d", e.getX(), e.getY());
                magnify.setLocation(e.getX() - magnify.getWidth() / 2, e.getY() - magnify.getHeight() / 2);
                magnify.repaint();
            }
        });
        panel.setBackground(Color.black);
        panel.setPreferredSize(new Dimension(w, h));
        panel.setLayout(null);
        panel.add(magnify);

        frame.setLocation(x, y);
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
