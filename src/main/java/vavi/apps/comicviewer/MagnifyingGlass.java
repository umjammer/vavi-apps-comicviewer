/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.function.BiFunction;
import java.util.logging.Level;
import javax.swing.JComponent;

import vavi.util.Debug;


/**
 * MagnifyingGlass.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-10-13 nsano initial version <br>
 */
class MagnifyingGlass extends JComponent {
    private float magnitude = 10;
    private final BiFunction<Rectangle, Point, BufferedImage> subImage;

    public MagnifyingGlass(int width, int height, BiFunction<Rectangle, Point, BufferedImage> subImage) {
        setSize(new Dimension(width, height));
        setOpaque(false);
        setVisible(false);
        this.subImage = subImage;
    }

    public void setMagnitude(float magnitude) {
        this.magnitude = magnitude;
    }

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isMetaDown()) {
                // start magnify
//Debug.printf("mousePressed: %d, %d", e.getX(), e.getY());
                setLocation(e.getX() - getWidth() / 2, e.getY() - getHeight() / 2);
                setVisible(true);
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // end magnify
//Debug.printf("mouseReleased: %d, %d", e.getX(), e.getY());
            setVisible(false);
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
//Debug.printf("mouseDragged: %d, %d", e.getX(), e.getY());
            setLocation(e.getX() - getWidth() / 2, e.getY() - getHeight() / 2);
            repaint();
        }
    };

    /** must add both addMouseListener and addMouseMotionListener! */
    public MouseAdapter getMouseAdapter() {
        return mouseAdapter;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = Math.round(getWidth() / magnitude);
        int h = Math.round(getHeight() / magnitude);
        int x = getX() + Math.round((getWidth() - w) / 2f);
        int y = getY() + Math.round((getHeight() - h) / 2f);
Debug.printf(Level.FINE, "magnify: %d, %d %d, %d", x, y, w, h);
        int mx = getX() + Math.round(getWidth() / 2f);
        int my = getY() + Math.round(getHeight() / 2f);
        BufferedImage sub = subImage.apply(new Rectangle(x, y, w, h), new Point(mx, my));
        if (sub != null) {
//            ((Graphics2D) g).setRenderingHints(hints);
            g.setClip(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
            g.drawImage(sub, 0, 0, getWidth(), getHeight(), null);
        }
//g.setColor(Color.green);
//Debug.printf("green: %d, %d %d, %d", x - getX(), y - getY(), w, h);
//g.drawRect(x - getX(), y - getY(), w, h);
    }
}
