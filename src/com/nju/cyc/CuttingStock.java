package com.nju.cyc;

/**
 * @author GrahamSa (https://github.com/chenyucheng0503)
 * @ClassName CuttingStock.java
 * @Description
 * @References
 * @createTime 2022-08-12 08:29:00
 */

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * instance:
 * 17       -- stock length
 * 3        -- need type number
 * 3,4,5    -- cutting type
 * 20,25,30 -- demand
 */

public class CuttingStock {
    public double stock_length = 0;
    public int item_types = 0;
    public double[] item;
    public double[] item_demand;

    void loadInstance() throws IOException {
        Scanner sc = new Scanner(new File("Data/cutting_stock.txt"));
        stock_length = Double.parseDouble(sc.nextLine());
        item_types = Integer.parseInt(sc.nextLine());

        String[] tmp = sc.nextLine().trim().split(",");
        item = new double[item_types];
        for (int i=0; i<item_types; i++) {
            item[i] = Double.parseDouble(tmp[i]);
        }

        tmp = sc.nextLine().trim().split(",");
        item_demand = new double[item_types];
        for (int i=0; i<item_types; i++) {
            item_demand[i] = Double.parseDouble(tmp[i]);
        }
    }
}
