/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class Test1 {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "test1.dir")
    String dir;

    @Property(name = "test1.file")
    String file;

    @BeforeEach
    void setup() throws IOException {
        Test1 app = new Test1();
        PropsEntity.Util.bind(app);
    }

    @Test
    void test1() throws Exception {
        Path path = Paths.get(dir, file);
        URI uri = URI.create("archive:" + path.toUri());
        Debug.println("open fs: " + uri);
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
        Path virtualRoot = fs.getRootDirectories().iterator().next();
Debug.println(virtualRoot);
        Files.walk(virtualRoot)
                .filter(Main::isImage)
                .sorted()
                .forEach(System.err::println);
    }

    void test2() throws Exception {
        Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(e -> {
            Clipboard clipboard = (Clipboard) e.getSource();
            if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                try {
                    StringBuilder sb = new StringBuilder();

                    Path path = Paths.get(((List<File>) clipboard.getData(DataFlavor.javaFileListFlavor)).get(0).getPath());

                    sb.append(path);

                    sb.append("\n");
                    sb.append("\n");

                    Map<String, String> envs = System.getenv();
                    for (String name : envs.keySet()) {
                        sb.append(String.format("E: %s = %s%n", name, envs.get(name)));
                    }
Debug.println("@@@@@@@@@@@@@@@@@@@@@@@: " + sb);
JOptionPane.showMessageDialog(null, sb.toString());
                } catch (UnsupportedFlavorException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}

/* */
