/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.JCheckBoxMenuItem;
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
import com.apple.eawt.FullScreenUtilities;
import vavi.awt.dnd.Droppable;
import vavi.swing.JImageComponent;
import vavi.util.Debug;
import vavi.util.archive.Archives;
import vavi.util.archive.zip.JdkZipArchive;


/**
 * Main.
 * <p>
 * TODO
 *  - opening jumper, prev/next by key made index confuse?
 *  - after jump page, treat caching well
 *  - recent
 *  - when open by .app, filename is weired
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-09-08 nsano initial version <br>
 */
public class Main {

    void storeBounds() {
        prefs.putInt("lastX", frame.getX());
        prefs.putInt("lastY", frame.getY());
        prefs.putInt("lastWidth", Math.max(base.getWidth(), 160));
        prefs.putInt("lastHeight", Math.max(base.getHeight(), 120));
    }

    /**
     * @param args none
     */
    public static void main(String[] args) throws Exception {

        Main app = new Main();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
Debug.println("shutdownHook");
            app.prefs.putInt("lastIndex", app.index);
            app.storeBounds();
            app.prefs.put("lastPath", String.valueOf(app.path));
            if (app.fs != null) {
                try {
                    app.fs.close();
Debug.println("close fs");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                app.fs = null;
            }
        }));
        // create and display a simple jframe
        app.gui();
        if (args.length > 0) {
            Path p = Paths.get(args[0]);
            if (Files.exists(p)) {
                app.init(p, 0);
            }
        } else {
            // existing `CFProcessPath` means this program is executed by .app
            if (app.isMac() && System.getenv("CFProcessPath") != null) {
                try {
                    // https://alvinalexander.com/blog/post/jfc-swing/java-handle-drag-drop-events-mac-osx-dock-application-icon-2/
                    Application application = Application.getApplication();
                    // TODO OpenFileHandler is <= 1.8
                    application.setOpenFileHandler(openFilesEvent -> {
                        List<File> files = openFilesEvent.getFiles();
                        // TODO file name is weired
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
                    if (Files.exists(path)) {
                        int index = app.prefs.getInt("lastIndex", 0);
                        app.init(path, index);
                    }
                }
            }
        }
    }

    static final String[] imageExts;

    static final String[] archiveExts;

    static {
        imageExts = ImageIO.getReaderFileSuffixes();
Debug.println("available images: " + Arrays.toString(imageExts));
        archiveExts = Archives.getReaderFileSuffixes();
Debug.println("available archives: " + Arrays.toString(archiveExts));
    }

    static String getExt(Path path) {
        String filename = path.getFileName().toString();
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    static boolean isArchive(Path path) {
        return !Files.isDirectory(path) && Arrays.asList(archiveExts).contains(getExt(path));
    }

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

    private Thread minThreadFactory(Runnable r) {
        Thread thread = new Thread(r);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
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

            es = Executors.newSingleThreadExecutor(this::minThreadFactory);
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
    Map<Object, Object> hints;
    JImageComponent imageR;
    JImageComponent imageL;
    JMenu openRecentMenu;
    JMenu openSiblingMenu;

    // view-controller
    JFrame frame;
    JLayeredPane base;
    JPanel pages;
    JPanel glass;
    Jumper jumper;
    JCheckBoxMenuItem fullScreen;

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
        frame.validate();
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

    boolean drop(Path path) {
        try {
            System.setProperty(JdkZipArchive.ZIP_ENCODING, "utf-8");
            init(path, 0);
            return true;
        } catch (IllegalArgumentException e) {
            if (e.getMessage().equals("MALFORMED")) {
Debug.println("zip reading failure by utf-8, retry using ms932");
                System.setProperty(JdkZipArchive.ZIP_ENCODING, "ms932");
                init(path, 0);
                return true;
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    static String abbreviate(Path p, int l) {
        if (p.getFileName().toString().length() < l) {
            return p.getFileName().toString();
        }
        String m = "...";
        String ext = getExt(p);
        return p.getFileName().toString().substring(0, l - ext.length() - m.length()) + m + "." + ext;
    }

    String label() {
        return String.format("#%d-%d/%d (%s | %s)", index, index + 1, images.size(),
                index < images.size() ? abbreviate(images.get(index), 17) : "",
                index + 1 < images.size() ? abbreviate(images.get(index + 1), 17) : "");
    }

    boolean isMac() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("mac");
    }

    public boolean isFullScreen(Window window) {
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
Debug.println("isFullScreen: " + (size.width == window.getWidth() && size.height == window.getHeight()));
        return size.width == window.getWidth() && size.height == window.getHeight();
    }

    void setFullScreen(boolean enabled) {
        if (isMac()) {
            Application.getApplication().requestToggleFullScreen(frame);
        } else {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

            if (gd.isFullScreenSupported() & enabled) {
                frame.setUndecorated(true);
                gd.setFullScreenWindow(frame); // TODO how to off?
            }
        }
Debug.println(Level.FINE, "fullScreen: " + enabled);
    }

    Rectangle restoreBounds() {
        int x = prefs.getInt("lastX", 0);
        int y = prefs.getInt("lastY", 0);
        int w = prefs.getInt("lastWidth", 1600);
        int h = prefs.getInt("lastHeight", 1200);
        return new Rectangle(x, y, w, h);
    }

    void gui() {
        Rectangle fr = restoreBounds();
        int x = fr.x;
        int y = fr.y;
        int w = fr.width;
        int h = fr.height;

        Pager pager = new Pager(this::prevPage, this::nextPage,
                e -> imageL.getBounds().contains(e.getPoint()),
                e -> imageR.getBounds().contains(e.getPoint()),
                i -> {
                    index = i;
                    updateModel();

                    jumper.setText(label());
                });

        frame = new JFrame();
        frame.addKeyListener(pager.getPagingAdapter());
        frame.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F && e.isMetaDown()) {
                    setFullScreen(!fullScreen.isSelected());
                }
            }
        });
        frame.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
Debug.println("componentResized: " + e.getComponent().getBounds());
                glass.setSize(base.getSize());
                pages.setSize(base.getSize());
                fullScreen.setSelected(isFullScreen(frame));
                updateView();
            }
        });

        JMenuItem openMenu = new JMenuItem("Open...");
        openMenu.addActionListener(e -> open());
        openRecentMenu = new JMenu("Open Recent");
        openSiblingMenu = new JMenu("Open Sibling");
        JMenuItem closeMenu = new JMenuItem("Close");
        closeMenu.addActionListener(e -> { clean(); updateModel(); });
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(openMenu);
        fileMenu.add(openRecentMenu);
        fileMenu.add(openSiblingMenu);
        fileMenu.add(closeMenu);
        fullScreen = new JCheckBoxMenuItem("Full Screen");
        fullScreen.addActionListener(e -> { setFullScreen(fullScreen.isSelected()); updateView(); });
        JMenu viewMenu = new JMenu("View");
        viewMenu.add(fullScreen);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);

        frame.setJMenuBar(menuBar);

        // TODO RootPaneContainer.html#getGlassPane()
        glass = new JPanel() {
            {
                Droppable.makeComponentSinglePathDroppable(this, Main.this::drop);
            }
            public void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (images.size() == 0) {
                    TextDrawer textDrawer = new TextDrawer("San Serif", 20, 12);
                    textDrawer.setStrokeColors(Color.pink, Color.white);
                    String text = "Drop an archive file or a folder here";
                    textDrawer.draw(g, text, getWidth(), getHeight());
                }
            }
        };
        glass.addMouseListener(pager.getPagingAdapter());
        glass.setOpaque(false);
        glass.setLayout(null);

        MagnifyingGlass magnify = new MagnifyingGlass(450, 450, (r, p) -> {
            JImageComponent comp = null;
            int dx = 0;
            if (imageL.getBounds().contains(p.x, p.y)) {
Debug.printf(Level.FINE, "mag left: " + imageL.getBounds());
                comp = imageL;
            } else if (imageR.getBounds().contains(p.x, p.y)) {
Debug.printf(Level.FINE, "mag right: " + imageR.getBounds());
                comp = imageR;
                dx = Math.round(pages.getWidth() / 2f);
            }
            BufferedImage sub = null;
            if (comp != null) {
Debug.printf(Level.FINE, "mag area: %d, %d %d, %d", r.x - dx, r.y, r.width, r.height);
                sub = comp.getSubimage(r.x - dx, r.y, r.width, r.height);
            }
            return sub;
        });

        glass.addMouseListener(magnify.getMouseAdapter());
        glass.addMouseMotionListener(magnify.getMouseAdapter());
        glass.add(magnify);

        jumper = new Jumper(() -> {
            if (images.size() > 0) {
                jumper.setMinimum(0);
                jumper.setMaximum(images.size() - 1);
                jumper.setValue(index);

                jumper.setText(label());
            }
        }, frame::requestFocus);
        jumper.setSize(new Dimension(320, 80));
        jumper.setLocation(0, 0);
        jumper.addChangeListener(pager.getPagingAdapter());

        glass.add(jumper);

        pages = new JPanel();
        pages.setBackground(Color.black);
        pages.setLayout(new GridLayout(1, 2));

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

        pages.add(imageL);
        pages.add(imageR);

        base = new JLayeredPane();
        base.setPreferredSize(new Dimension(w, h));
        base.add(glass);
        base.add(pages);

        if (isMac()) {
            FullScreenUtilities.setWindowCanFullScreen(frame, true);
        }

        frame.setLocation(x, y);
        frame.setContentPane(base);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
