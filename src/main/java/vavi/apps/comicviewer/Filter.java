/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer;

import java.awt.image.BufferedImage;


/**
 * Filter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-10-16 nsano initial version <br>
 */
public interface Filter {

    String getName();

    BufferedImage filter(BufferedImage image);
}
