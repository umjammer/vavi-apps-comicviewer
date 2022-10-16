/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer.filter;

import java.awt.image.BufferedImage;

import com.github.araxeus.dnnsuperres.DNNSuperResolutionOp;
import vavi.apps.comicviewer.Filter;
import vavi.util.Debug;


/**
 * DnnSuperResFilter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-10-16 nsano initial version <br>
 */
public class DnnSuperResFilter implements Filter {

    DNNSuperResolutionOp.Mode mode = DNNSuperResolutionOp.MODES[6];
    DNNSuperResolutionOp dnnsrFilter = new DNNSuperResolutionOp(mode);

    @Override
    public String getName() {
        return "OpenCV:DnnSuperRes";
    }

    @Override
    public BufferedImage filter(BufferedImage image) {
Debug.println("DNNSuperResolution: " + mode);
        return dnnsrFilter.filter(image, null);
    }
}
