/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.comicviewer;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import vavi.util.Debug;


/**
 * Pager.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-10-13 nsano initial version <br>
 */
class Pager {

    private final Consumer<Integer> prev;
    private final Consumer<Integer> next;
    private final Consumer<Integer> jump;
    private final Function<MouseEvent, Boolean> left;
    private final Function<MouseEvent, Boolean> right;

    public Pager(Consumer<Integer> prev, Consumer<Integer> next,
                 Function<MouseEvent, Boolean> left, Function<MouseEvent, Boolean> right,
                 Consumer<Integer> jump) {
        this.prev = prev;
        this.next = next;
        this.left = left;
        this.right = right;
        this.jump = jump;
    }

    private static class PagingAdapter extends MouseAdapter implements MouseListener, KeyListener, ChangeListener {
        @Override public void keyTyped(KeyEvent e) {
        }
        @Override public void keyPressed(KeyEvent e) {
        }
        @Override public void keyReleased(KeyEvent e) {
        }
        @Override public void stateChanged(ChangeEvent e) {
        }
    }

    private final PagingAdapter pagingAdapter = new PagingAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            int d = e.isShiftDown() ? 1 : 2;
//Debug.println("move: " + d);
            switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                next.accept(d);
                break;
            case KeyEvent.VK_RIGHT:
                prev.accept(d);
                break;
            case KeyEvent.VK_N:
                if (e.isControlDown()) {
                    next.accept(d);
                }
                break;
            case KeyEvent.VK_P:
                if (e.isControlDown()) {
                    prev.accept(d);
                }
                break;
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!e.isMetaDown()) {
                int d = e.isShiftDown() ? 1 : 2;
                if (left.apply(e)) {
                    next.accept(d);
                } else if (right.apply(e)) {
                    prev.accept(d);
                }
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (e.getSource() instanceof JSlider slider) {
                if (!slider.getValueIsAdjusting() && slider.isVisible()) {
                    int value = slider.getValue();
Debug.println("slider index: " + value);
                    jump.accept(value);
                }
            }
        }
    };

    public PagingAdapter getPagingAdapter() {
        return pagingAdapter;
    }
}
