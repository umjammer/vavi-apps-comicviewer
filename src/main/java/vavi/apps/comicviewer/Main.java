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
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.apple.eawt.Application;
import vavi.awt.dnd.Droppable;
import vavi.swing.JImageComponent;
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
            app.prefs.putInt("lastWidth", Math.max(app.base.getWidth(), 160));
            app.prefs.putInt("lastHeight", Math.max(app.base.getHeight(), 120));
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
            String osName = System.getProperty("os.name").toLowerCase();
            // existing `CFProcessPath` means this program is executed by .app
            if (osName.contains("mac") && System.getenv("CFProcessPath") != null) {
                try {
                    // https://alvinalexander.com/blog/post/jfc-swing/java-handle-drag-drop-events-mac-osx-dock-application-icon-2/
                    Application application = Application.getApplication();
                    // TODO OpenFileHandler is <= 1.8
                    application.setOpenFileHandler(openFilesEvent -> {
                        List<File> files = openFilesEvent.getFiles();
Debug.println(Level.FINE, "files: " + files.size() + ", " + (files.size() > 0 ? files.get(0) : ""));
                        app.init(Paths.get(files.get(0).getPath()), 0);
                    });
                } catch (Throwable ex) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintWriter pr = new PrintWriter(baos);
                    ex.printStackTrace(pr);
                    pr.flush();
                    pr.close();
JOptionPane.showMessageDialog(null, baos.toString(), ex.getMessage(), JOptionPane.ERROR_MESSAGE);
                }
            }

            if (app.path == null) {
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

    void clean() {
        images.clear();
        cache.clear();
        if (es != null && !es.isTerminated()) {
            es.shutdownNow();
            while (!es.isTerminated()) Thread.yield();
        }
    }

    void addMenuItemTo(JMenu menu, Path p) {
        try {
            if (!Files.isSameFile(p, path)) {
                JMenuItem mi = new JMenuItem(p.getFileName().toString());
                mi.addActionListener(e -> {
                    init(p, 0);
                });
                menu.add(mi);
Debug.println("add menuItem: " + mi.getText());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void clearMenuItems(JMenu menu) {
        List<JMenuItem> mis = new ArrayList<>();
        for (int i = 0; i < menu.getItemCount(); i++) {
            mis.add(menu.getItem(i));
        }
        for (JMenuItem mi : mis) {
Debug.println("remove menuItem: " + mi);
            menu.remove(mi);
        }
Debug.println("remove menuItem: " + menu.getItemCount());
    }

    void updateOpenRecentMenu() {
        recent.add(path);
        if (recent.size() > RECENT) {
            recent.remove(0);
        }
        clearMenuItems(openRecentMenu);
        recent.forEach(p -> addMenuItemTo(openRecentMenu, p));
    }

    void init(Path path, int index) {
        try {
            this.path = path;
            this.index = index;

            clean();

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
            updateModel();
            clearMenuItems(openSiblingMenu);
            Files.list(path.getParent())
                    .filter(Main::isArchive)
                    .sorted()
                    .forEach(p -> addMenuItemTo(openSiblingMenu, p));
            clearMenuItems(openRecentMenu);
            updateOpenRecentMenu();

            es = Executors.newSingleThreadExecutor();
            es.submit(() -> {
                // don't disturb before first 2 pages are shown
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
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
                        e.printStackTrace();
                    }
                    fs = null;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    static final int RECENT = 10;
    List<Path> recent = new ArrayList<>(RECENT);
    JImageComponent imageR;
    JImageComponent imageL;
    JMenu openRecentMenu;
    JMenu openSiblingMenu;

    // view
    JFrame frame;
    JLayeredPane base;
    JPanel panel;
    JPanel glass;
    Map<Object, Object> hints;

    // model
    Path path;
    int index = 0;

    void nextPage(int d) {
        if (index + d < images.size() - 1) {
            index += d;
            updateModel();
        }
    }

    void prevPage(int d) {
        if (index >= d) {
            index -= d;
            updateModel();
        }
    }

    void updateView() {
        frame.repaint();
//Debug.println("glass: " + glass.getBounds());
//Debug.println("base: " + base.getBounds());
    }

    void updateModel() {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

Debug.println("index: " + index);
        imageR.setImage(index < images.size() ? getImage(index) : null);
        imageL.setImage(index + 1 < images.size() ? getImage(index + 1) : null);

        frame.setCursor(Cursor.getDefaultCursor());

        updateView();
    }

    void open() {
        JFileChooser ofd = new JFileChooser();
        ofd.setDialogTitle("Open");

        Path dir = Paths.get(prefs.get("lastDir", path != null ? path.getParent().toString() : System.getProperty("user.dir")));
        if (Files.exists(dir)) {
            ofd.setCurrentDirectory(dir.toFile());
        }

        if (ofd.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            init(Paths.get(ofd.getSelectedFile().getPath()), 0);
            prefs.get("lastDir", ofd.getCurrentDirectory().getPath());
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
//Debug.println("move: " + d);
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
            }
        });
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                glass.setSize(base.getSize());
                panel.setSize(base.getSize());
                updateView();
            }
        });

        JMenuItem openMenu = new JMenuItem("Open...");
        openMenu.addActionListener(e -> { open(); });
        openRecentMenu = new JMenu("Open Recent");
        openSiblingMenu = new JMenu("Open Sibling");
        JMenuItem closeMenu = new JMenuItem("Close");
        closeMenu.addActionListener(e -> { clean(); updateModel(); });
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(openMenu);
        fileMenu.add(openRecentMenu);
        fileMenu.add(openSiblingMenu);
        fileMenu.add(closeMenu);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);

        frame.setJMenuBar(menuBar);

        float magnitude = 10;
        JPanel magnify = new JPanel() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);

                int w = Math.round(getWidth() / magnitude);
                int h = Math.round(getHeight() / magnitude);
                int x = getX() + Math.round((getWidth() - w) / 2f);
                int y = getY() + Math.round((getHeight() - h) / 2f);
Debug.printf(Level.FINE, "magnify: %d, %d %d, %d", x, y, w, h);
                int mx = getX() + Math.round(getWidth() / 2f);
                int my = getY() + Math.round(getHeight() / 2f);
                JImageComponent comp = null;
                int dx = 0;
                if (imageL.getBounds().contains(mx, my)) {
Debug.printf(Level.FINE, "mag left: " + imageL.getBounds());
                    comp = imageL;
                } else if (imageR.getBounds().contains(mx, my)) {
Debug.printf(Level.FINE, "mag right: " + imageR.getBounds());
                    comp = imageR;
                    dx = Math.round(panel.getWidth() / 2f);
                }
                if (comp != null) {
//                    ((Graphics2D) g).setRenderingHints(hints);
Debug.printf(Level.FINE, "mag area: %d, %d %d, %d", x - dx, y, w, h);
                    BufferedImage sub = comp.getSubimage(x - dx, y, w, h);
                    g.setClip(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                    g.drawImage(sub, 0, 0, getWidth(), getHeight(), null);
                }
//g.setColor(Color.green);
//Debug.printf("green: %d, %d %d, %d", x - getX(), y - getY(), w, h);
//g.drawRect(x - getX(), y - getY(), w, h);
            }
        };
        magnify.setSize(new Dimension(450, 450));
        magnify.setOpaque(false);
        magnify.setVisible(false);

        glass = new JPanel() {
            {
                Droppable.makeComponentSinglePathDroppable(this, path -> {
                    try {
System.setProperty("vavi.util.archive.zip.encoding", "utf-8");
                        init(path, 0);
                        return true;
                    } catch (IllegalArgumentException e) {
                        if (e.getMessage().equals("MALFORMED")) {
Debug.println("zip reading failure by utf-8, retry using ms932");
System.setProperty("vavi.util.archive.zip.encoding", "ms932");
                            init(path, 0);
                            return true;
                        } else {
                            e.printStackTrace();
                        }
                        return false;
                    }
                });
            }
            void drawText(Graphics g, String text, String fontName, int point, int ratio, Color strokeColor, Color fillColor) {
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
                graphics.setColor(strokeColor);
                graphics.setStroke(new BasicStroke(stroke));
                graphics.draw(shape);
                graphics.setColor(fillColor);
                graphics.fill(shape);
            }
            public void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (images.size() == 0) {
                    String text = "Drop an archive file or a folder here";
                    drawText(g, text, "San Serif", 20, 12, Color.pink, Color.white);
                }
            }
        };
        glass.addMouseListener(new MouseAdapter() {
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
                if (!e.isMetaDown()) {
                    int d = e.isShiftDown() ? 1 : 2;
                    if (imageL.getBounds().contains(e.getX(), e.getY())) {
                        nextPage(d);
                    } else if (imageR.getBounds().contains(e.getX(), e.getY())) {
                        prevPage(d);
                    }
                }
            }
        });
        glass.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
//Debug.printf("mouseDragged: %d, %d", e.getX(), e.getY());
                magnify.setLocation(e.getX() - magnify.getWidth() / 2, e.getY() - magnify.getHeight() / 2);
                magnify.repaint();
            }
        });
        glass.setOpaque(false);
        glass.setLayout(null);
        glass.add(magnify);

        panel = new JPanel();
        panel.setBackground(Color.black);
        panel.setLayout(new GridLayout(1, 2));

        hints = new HashMap<>();
        hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        imageL = new JImageComponent();
        imageL.setRenderingHints(hints);
        imageL.setImageHorizontalAlignment(SwingConstants.RIGHT);
        imageL.setImageVerticalAlignment(SwingConstants.CENTER);
        imageR = new JImageComponent();
        imageR.setRenderingHints(hints);
        imageR.setImageVerticalAlignment(SwingConstants.CENTER);

        panel.add(imageL);
        panel.add(imageR);

        base = new JLayeredPane();
        base.setPreferredSize(new Dimension(w, h));
        base.add(glass);
        base.add(panel);

        frame.setLocation(x, y);
        frame.setContentPane(base);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
