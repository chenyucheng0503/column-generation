package com.nju.cyc;

import java.io.IOException;

/**
 * @author GrahamSa (https://github.com/chenyucheng0503)
 * @ClassName main.java
 * @Description
 * @References
 * @createTime 2022-08-12 08:41:00
 */

public class main {
    public static void main(String[] args) throws IOException {
        CuttingStock problem = new CuttingStock();
        problem.loadInstance();
        System.out.println(problem);
    }
}
