/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer.filter;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.rococoa.cocoa.coreimage.CIFilterOp;
import org.rococoa.cocoa.foundation.NSNumber;
import org.rococoa.cocoa.foundation.NSObject;
import vavi.apps.comicviewer.Filter;


/**
 * SharpCIFilter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-10-16 nsano initial version <br>
 */
public class SharpCIFilter implements Filter {

    @Override
    public String getName() {
        return "CI:UnsharpMask";
    }

    @Override
    public BufferedImage filter(BufferedImage image) {
        Map<String, NSObject> options = new HashMap<>();
        options.put("inputIntensity", NSNumber.of(2.0));
        options.put("inputRadius", NSNumber.of(1.0));
        return new CIFilterOp("CIUnsharpMask", options).filter(image, null);
    }
}
