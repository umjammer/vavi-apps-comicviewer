package vavi.apps.comicviewer;/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;


class Test1 {

    @Test
    void test1() throws Exception {
        Main.AspectRatio as = new Main.AspectRatio(16, 9);
        int h = as.getHeight(300);
        assertEquals(147, h);
//        as = new Main.AspectRatio(1920, 1080);
//        h = as.getHeight(1920);
//        assertEquals(1080, h);
    }
}

/* */
