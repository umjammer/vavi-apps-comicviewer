/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import static javax.swing.SwingConstants.CENTER;


/**
 * TextDrawer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-10-13 nsano initial version <br>
 */
class TextDrawer {
    private final Font font;
    private final float stroke;
    private Color strokeColor;
    private Color fillColor;
    private int alignmentX = CENTER; // TODO
    private int alignmentY = CENTER; // TODO

    public TextDrawer(String fontName, int point, int ratio) {
        this.font = new Font(fontName, Font.PLAIN, point);
        this.stroke = point / (float) ratio;
    }

    public void setStrokeColors(Color strokeColor, Color fillColor) {
        this.strokeColor = strokeColor;
        this.fillColor = fillColor;
    }

    public void setImageHorizontalAlignment(int alignment) {
        this.alignmentX = alignment;
    }

    public void setImageVerticalAlignment(int alignment) {
        this.alignmentY = alignment;
    }

    public void draw(Graphics g, String text, int width, int height) {

        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        FontRenderContext frc = graphics.getFontRenderContext();

        AttributedString as = new AttributedString(text);
        as.addAttribute(TextAttribute.FONT, font, 0, text.length());
        AttributedCharacterIterator aci = as.getIterator();

        TextLayout tl = new TextLayout(aci, frc);
        float sw = (float) tl.getBounds().getWidth();
        float sh = (float) tl.getBounds().getHeight();
        Shape shape = tl.getOutline(AffineTransform.getTranslateInstance((width - sw) / 2, (height - sh) / 2));
        graphics.setColor(strokeColor);
        graphics.setStroke(new BasicStroke(stroke));
        graphics.draw(shape);
        graphics.setColor(fillColor);
        graphics.fill(shape);
    }
}
