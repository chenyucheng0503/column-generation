package com.nju.cyc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import ilog.concert.*;
import ilog.cplex.*;

/**
 * @author GrahamSa (https://github.com/chenyucheng0503)
 * @ClassName ColumnGeneration.java
 * @Description
 * @References
 * @createTime 2022-08-12 08:49:00
 */

public class ColumnGeneration {
    static final double EPSILON = 1.0E-6;
    CuttingStock problem;

    // master problem model
    IloCplex masterSolver;
    IloObjective obj;
    IloRange[] demandsToFill;

    // sub problem model
    IloCplex subSolver;
    IloObjective reducedCost;

    // record cutting patterns
    ArrayList<IloNumVar> patterns;
    ArrayList<int[]> columns;
    IloNumVar[] use;


    ColumnGeneration(CuttingStock problem) {
        this.problem = problem;
    }

    void solveProblems() throws IOException, IloException {
        InitRMP();
        InitSubProblem();

        // start to column generation
        int iter = 0;
        while (true) {
            iter++;
            masterSolver.solve();

            // set the sub problem
            double[] newPattern;
            // price is the dual prices of patterns
            double[] dualPrice = masterSolver.getDuals(demandsToFill);
            reducedCost.setExpr(subSolver.scalProd(use, dualPrice));
//            subSolver.exportModel("sub_model.lp");
            subSolver.solve();

            reportPatternUsage(iter, dualPrice);

            // check the result of sub problem
            if (1 - subSolver.getObjValue() <= -EPSILON) {
                newPattern = subSolver.getValues(use);
                columns.add(Arrays.stream(newPattern).mapToInt(i -> (int) i).toArray());
                reportNewPattern();
            } else {
                break;
            }

            // add new column to master problem for the generated pattern
            IloColumn column = masterSolver.column(obj, 1.0); // delta_obj
            for (int i = 0; i < newPattern.length; i++) {
                column = column.and(masterSolver.column(demandsToFill[i], newPattern[i])); // delta_d_i
            }
            patterns.add(masterSolver.numVar(column, 0, Double.MAX_VALUE));
        }

        // sub problem cannot find reduce cost < 1

        // 强制求解int, 如果不转换的话 x_j 会是小数
        // column-generation phase terminates, type of the solution should be changed from continuous to integer,
        for (IloNumVar pattern : patterns) {
            masterSolver.add(masterSolver.conversion(pattern, IloNumVarType.Int));
        }
        masterSolver.solve();

        reportBestCuttingStrategy();

        masterSolver.end();
        subSolver.end();
    }

    /**
     * init Restricted Master Problem
     * @throws IloException
     */
    void InitRMP() throws IloException {
        masterSolver = new IloCplex();
        masterSolver.setOut(null);
        // objective
        obj = masterSolver.addMinimize();

        // add constraint: item product >= item_demand
        demandsToFill = new IloRange[problem.item_types];
        for (int i = 0; i < demandsToFill.length; i++) {
            demandsToFill[i] = masterSolver.addRange(problem.item_demand[i], Double.MAX_VALUE);     // addRange(Double:lb, Double:ub)
        }

        //
        patterns = new ArrayList<>();
        columns = new ArrayList<>();

        // 添加初始列, 使用 greedy
        for (int i = 0; i < problem.item_types; i++) {
            // 只使用 1 type item, 生成初始 columns(size is item_types) [5,0,0]
            int greedy_num = (int) (problem.stock_length / problem.item[i]);
            int[] greedy_column = new int[problem.item_types];
            greedy_column[i] = greedy_num;
            columns.add(greedy_column);

            // 一列包括了一个变量在 objective & constraints 的所有系数
            IloColumn delta_obj = masterSolver.column(obj, 1.0);        // column(Objective, int): Creates and returns a column from the specified objective and value
            IloColumn delta_d_i = masterSolver.column(demandsToFill[i], greedy_num); // column(IRange, int): Creates and returns a column from the specified range and value.
            // variable x_i: times that pattern a_i is applied
            patterns.add(masterSolver.numVar(delta_obj.and(delta_d_i), 0.0, Double.MAX_VALUE));     // numVar(Column, Double:lb, Double:up): Creates and returns a numeric variable representing a column with a specified lower and upper bound.
        }

    }

    /**
     * Init Sub Problem
     * @throws IloException
     */
    void InitSubProblem() throws IloException {
        subSolver = new IloCplex();
        subSolver.setOut(null);
        // objective of sub problem
        reducedCost = subSolver.addMaximize();
        // item_types 个 numVar
        use = subSolver.numVarArray(problem.item_types, 0, Double.MAX_VALUE, IloNumVarType.Int);
        // add a constraint lb <= expr <= ub
        subSolver.addRange(-Double.MAX_VALUE, subSolver.scalProd(problem.item, use), problem.stock_length);
    }

    /**
     * report the
     * @throws IloException
     */
    private void reportBestCuttingStrategy() throws IloException {
        System.out.println("\n------------------------------------------------------");
        System.out.println("Solution status: " + masterSolver.getStatus());
        // report total usage of boards and usage of patterns of the best cutting strategy
        System.out.println("A total of " + patterns.size() + " patterns are generated:");
        for (int p = 0; p < patterns.size(); p++) {
            System.out.print("\nPat " + p + ":" + "\t");
            for (int cut: columns.get(p)) {
                System.out.print(cut + "\t");
            }
        }
        // print round-up solution and corresponding patterns
        System.out.print("\nBest integer solution uses " + masterSolver.getObjValue() + " rolls");
        System.out.println();
        for (int j = 0; j < patterns.size(); j++) {
            System.out.print("\nPattern " + j + " = " + masterSolver.getValue(patterns.get(j)) + " ");
            if (masterSolver.getValue(patterns.get(j)) > 0.0) {
                System.out.print(Arrays.toString(columns.get(j)));
            }
        }
    }

    /**
     * report reduced cost and new pattern generated
     */
    private void reportNewPattern() throws IloException {
        System.out.println("Reduced cost is " + subSolver.getObjValue());
        for (int i = 0; i < use.length; i++) {
            System.out.printf("Type %d cut = %s%n", i, (int) subSolver.getValue(use[i]));
        }
    }

    /**
     * report usage of patterns and dual price
     */
    private void reportPatternUsage(int iter, double[] price) throws IloException {
        System.out.println("\n>> Iteration " + iter);
        System.out.println("Using " + masterSolver.getObjValue() + " rolls.");
        for (int i = 0; i < patterns.size(); i++) {
            System.out.printf("Pattern %d n_cut = %s%n", i, masterSolver.getValue(patterns.get(i)));
        }
        for (int i = 0; i < price.length; i++) {
            System.out.printf("Type %d price = %s%n", i, price[i]);
        }
    }
}
