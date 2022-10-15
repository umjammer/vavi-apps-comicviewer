/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


/**
 * SliderKnobTooltip.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022/10/13 nsano initial version <br>
 * @see "https://ateraimemo.com/Swing/SliderToolTips.html"
 */
class SliderKnobTooltip {
    private final JWindow toolTip = new JWindow();
    private final JLabel label = new JLabel("", SwingConstants.CENTER);
    private final Dimension size = new Dimension(30, 20);
    private final String format;

    public SliderKnobTooltip(String format) {
        this.format = format;
        label.setOpaque(false);
        label.setBackground(UIManager.getColor("ToolTip.background"));
        label.setBorder(UIManager.getBorder("ToolTip.border"));
        toolTip.add(label);
        toolTip.setSize(size);
    }

    /** must add both addMouseListener and addMouseMotionListener! */
    public MouseAdapter getMouseAdapter() {
        return mouseAdapter;
    }

    private final MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
            updateToolTip(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            toolTip.setVisible(true);
            updateToolTip(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            toolTip.setVisible(false);
        }

        void updateToolTip(MouseEvent e) {
            JSlider slider = (JSlider) e.getSource();
            label.setText(String.format(format, slider.getValue()));
            Point p = e.getPoint();
            p.y = -size.height;
            SwingUtilities.convertPointToScreen(p, (Component) e.getSource());
            p.translate(-size.width / 2, 0);
            toolTip.setLocation(p);
        }
    };
}
