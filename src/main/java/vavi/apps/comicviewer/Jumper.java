/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import vavi.util.Debug;

import static javax.swing.SwingConstants.CENTER;


/**
 * Jumper.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-10-13 nsano initial version <br>
 */
class Jumper extends JPanel {

    JLabel label = new JLabel();
    JSlider slider = new JSlider();

    Runnable on;
    Runnable off;

    Jumper(Runnable on, Runnable off) {
        this.on = on;
        this.off = off;

        label.setHorizontalAlignment(CENTER);
        label.setVisible(false);

        slider.setOpaque(false);
        slider.setVisible(false);
        slider.setInverted(true);

        SliderKnobTooltip tooltip = new SliderKnobTooltip("%03d");
        slider.addMouseListener(tooltip.getMouseAdapter());
        slider.addMouseMotionListener(tooltip.getMouseAdapter());

        setLayout(new GridLayout(2, 1));
        Color c = new Color(Color.pink.getRed(), Color.pink.getGreen(), Color.pink.getBlue(), 0x40);
        setBackground(c);
        setOpaque(false); // need to keep visible for enter/exit, so make this transparent by opaque
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!slider.isVisible() && contains(e.getPoint())) {
Debug.println("mouseEntered: " + e.getPoint() + ", " + getBounds());
                    on.run();

                    slider.setVisible(true);
                    label.setVisible(true);
                    setOpaque(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
Debug.println("mouseExited: " + e.getPoint() + ", " + getBounds() + ", " + !contains(e.getPoint()));
                if (slider.isVisible() && !isInside(e)) { // contains(e.getPoint())
                    slider.setVisible(false);
                    label.setVisible(false);
                    setOpaque(false);

                    off.run();
                }
            }

            static boolean isInside(MouseEvent e) {
                Point p = new Point(e.getLocationOnScreen());
                SwingUtilities.convertPointFromScreen(p, e.getComponent());
Debug.println("isInside: " + p);
                return e.getComponent().contains(p);
            }
        });

        add(slider);
        add(label);
    }

    /** add a pager adapter */
    void addChangeListener(ChangeListener l) {
        slider.addChangeListener(l);
    }

    void setText(String text) {
        label.setText(text);
    }

    void setMinimum(int minimum) {
        slider.setMinimum(minimum);
    }

    void setMaximum(int maximum) {
        slider.setMaximum(maximum);
    }

    void setValue(int value) {
        slider.setValue(value);
    }
}
