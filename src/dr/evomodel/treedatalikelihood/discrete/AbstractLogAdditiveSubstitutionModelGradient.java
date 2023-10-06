/*
 * AbstractLogAdditiveSubstitutionModelGradient.java
 *
 * Copyright (c) 2002-2023 Alexei Drummond, Andrew Rambaut and Marc Suchard
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.evomodel.treedatalikelihood.discrete;

import dr.evolution.tree.Tree;
import dr.evolution.tree.TreeTrait;
import dr.evolution.tree.TreeTraitProvider;
import dr.evomodel.branchmodel.BranchModel;
import dr.evomodel.substmodel.*;
import dr.evomodel.treedatalikelihood.BeagleDataLikelihoodDelegate;
import dr.evomodel.treedatalikelihood.ProcessSimulation;
import dr.evomodel.treedatalikelihood.TreeDataLikelihood;
import dr.evomodel.treedatalikelihood.preorder.ProcessSimulationDelegate;
import dr.inference.hmc.GradientWrtParameterProvider;
import dr.inference.loggers.Loggable;
import dr.inference.model.Likelihood;
import dr.inference.model.Model;
import dr.inference.model.ModelListener;
import dr.math.matrixAlgebra.WrappedVector;
import dr.util.Citable;
import dr.xml.Reportable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Marc A. Suchard
 */

public abstract class AbstractLogAdditiveSubstitutionModelGradient implements
        GradientWrtParameterProvider, ModelListener, Reportable, Loggable, Citable {

    protected final TreeDataLikelihood treeDataLikelihood;
    protected final TreeTrait treeTraitProvider;
    protected final Tree tree;
    protected final BranchModel branchModel;

    protected final ComplexSubstitutionModel substitutionModel;
    protected final int stateCount;
//    protected final int whichSubstitutionModel;
//    protected final int substitutionModelCount;
    protected final List<Integer> crossProductAccumulationMap;

    private final ApproximationMode mode;

    public enum ApproximationMode {

        FIRST_ORDER("firstOrder") {
            @Override
            public String getInfo() {
                return "a first-order";
            }
        },
        AFFINE_CORRECTED("affineCorrected") {
            @Override
            public String getInfo() {
                return "an affine-corrected";
            }
        };

        ApproximationMode(String label) {
            this.label = label;
        }

        public abstract String getInfo();

        public String getLabel() {
            return label;
        }

        private String label;

        public static ApproximationMode factory(String label) {
            for (ApproximationMode mode : ApproximationMode.values()) {
                if (mode.getLabel().equalsIgnoreCase(label)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Unknown approximation mode");
        }
    }

    private static final ApproximationMode DEFAULT_MODE = ApproximationMode.FIRST_ORDER;

    public AbstractLogAdditiveSubstitutionModelGradient(String traitName,
                                                        TreeDataLikelihood treeDataLikelihood,
                                                        BeagleDataLikelihoodDelegate likelihoodDelegate,
                                                        ComplexSubstitutionModel substitutionModel) {
        this(traitName, treeDataLikelihood, likelihoodDelegate, substitutionModel, DEFAULT_MODE); // TODO Remove this constructor
    }

    public AbstractLogAdditiveSubstitutionModelGradient(String traitName,
                                                        TreeDataLikelihood treeDataLikelihood,
                                                        BeagleDataLikelihoodDelegate likelihoodDelegate,
                                                        ComplexSubstitutionModel substitutionModel,
                                                        ApproximationMode mode) {
        this.treeDataLikelihood = treeDataLikelihood;
        this.tree = treeDataLikelihood.getTree();
        this.branchModel = likelihoodDelegate.getBranchModel();
        this.substitutionModel = substitutionModel;
        this.stateCount = substitutionModel.getDataType().getStateCount();

//        this.whichSubstitutionModel = determineSubstitutionNumber(
//                likelihoodDelegate.getBranchModel(), substitutionModel);
//        this.substitutionModelCount = determineSubstitutionModelCount(likelihoodDelegate.getBranchModel());

        this.crossProductAccumulationMap = createCrossProductAccumulationMap(likelihoodDelegate.getBranchModel(),
                substitutionModel);

        this.mode = mode;

//        this.crossProductAccumulationMap = new int[0];
//        if (substitutionModelCount > 1) {
//            updateCrossProductAccumulationMap();
//        }

        String name = SubstitutionModelCrossProductDelegate.getName(traitName);

        if (treeDataLikelihood.getTreeTrait(name) == null) {
            ProcessSimulationDelegate gradientDelegate = new SubstitutionModelCrossProductDelegate(traitName,
                    treeDataLikelihood.getTree(),
                    likelihoodDelegate,
                    treeDataLikelihood.getBranchRateModel(),
                    substitutionModel.getDataType().getStateCount());
            TreeTraitProvider traitProvider = new ProcessSimulation(treeDataLikelihood, gradientDelegate);
            treeDataLikelihood.addTraits(traitProvider.getTreeTraits());
        }

        treeTraitProvider = treeDataLikelihood.getTreeTrait(name);
        assert (treeTraitProvider != null);

        Logger.getLogger("dr.evomodel.treedatalikelihood.discrete").info(
                "Gradient wrt " + traitName + " using " + mode.getInfo() + " approximation");
    }

    protected abstract double preProcessNormalization(double[] differentials, double[] generator, boolean normalize);

    abstract double processSingleGradientDimension(int dim,
                                                   double[] differentials, double[] generator, double[] pi,
                                                   boolean normalize, double normalizationConstant);

    @Override
    public double[] getGradientLogDensity() {

        long startTime;
        if (COUNT_TOTAL_OPERATIONS) {
            startTime = System.nanoTime();
        }

        double[] crossProducts = (double[]) treeTraitProvider.getTrait(tree, null);
        double[] generator = new double[crossProducts.length];

//        if (whichSubstitutionModel > 1 || substitutionModelCount > 1) {
        accumulateAcrossSubstitutionModelInstances(crossProducts);
//        }

        substitutionModel.getInfinitesimalMatrix(generator);
//        crossProducts = correctDifferentials(crossProducts);

        if (DEBUG_CROSS_PRODUCTS) {
            savedDifferentials = crossProducts.clone();
        }

        double[] pi = substitutionModel.getFrequencyModel().getFrequencies();

        double normalizationConstant = preProcessNormalization(crossProducts, generator,
                substitutionModel.getNormalization());

        final double[] gradient = new double[getParameter().getDimension()];
        for (int i = 0; i < getParameter().getDimension(); ++i) {
            gradient[i] = processSingleGradientDimension(i, crossProducts, generator, pi,
                    substitutionModel.getNormalization(),
                    normalizationConstant);
        }

        if (COUNT_TOTAL_OPERATIONS) {
            ++gradientCount;
            long endTime = System.nanoTime();
            totalGradientTime += (endTime - startTime) / 1000000;
        }

        return gradient;
    }

    double correction(int i, int j, double[] crossProducts) {

        if (mode == ApproximationMode.FIRST_ORDER) {
            return 0.0;
        }

        double[] affineMatrix = new double[stateCount * stateCount];

        if (crossProductAccumulationMap.size() > 1) {
            throw new RuntimeException("Not yet implemented");
        }

        // TODO Start cache for each (i,j) .. only depends on substitutionModel
        EigenDecomposition ed = substitutionModel.getEigenDecomposition();
        int index = findZeroEigenvalueIndex(ed.getEigenValues());

        double[] eigenVectors = ed.getEigenVectors();
        double[] inverseEigenVectors = ed.getInverseEigenVectors();

        double[] qQPlus = getQQPlus(eigenVectors, inverseEigenVectors, index);

        for (int m = 0; m < stateCount; ++m) {
            for (int n = 0; n < stateCount; n++) {
                // TODO there are only stateCount unique values
                affineMatrix[index12(m,n)] = (m == i) ?
                        (qQPlus[index12(m,i)] - 1.0) * qQPlus[index12(j,n)] :
                        qQPlus[index12(m,i)] * qQPlus[index12(j,n)];
            }
        }
        // TODO End cache

        double correction = 0.0;
        for (int m = 0; m < stateCount; ++m) {
            for (int n = 0; n < stateCount; ++n) {
                correction += crossProducts[index12(m,n)] * affineMatrix[index12(m,n)];
            }
        }

        return correction;
    }

    private int findZeroEigenvalueIndex(double[] eigenvalues) {
        for (int i = 0; i < stateCount; ++i) {
            if (eigenvalues[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private double[] getQQPlus(double[] eigenVectors, double[] inverseEigenVectors, int index) {
        return DifferentiableSubstitutionModelUtil.getQQPlus(eigenVectors, inverseEigenVectors, index, stateCount);
    }

    private double[] getQPlusQ(double[] eigenVectors, double[] inverseEigenVectors, int index) {
        return DifferentiableSubstitutionModelUtil.getQPlusQ(eigenVectors, inverseEigenVectors, index, stateCount);
    }

    private int index12(int i, int j) {
        return i * stateCount + j;
    }

    @SuppressWarnings("unused")
    private int index21(int i, int j) {
        return j * stateCount + i;
    }

    private void accumulateAcrossSubstitutionModelInstances(double[] crossProducts) {
        final int length = stateCount * stateCount;

        int firstModel = crossProductAccumulationMap.get(0);
        if (firstModel > 0) {
            // Copy first set of entries
            System.arraycopy(
                    crossProducts, firstModel * length,
                    crossProducts, 0, length);
        }

        for (int i = 1; i < crossProductAccumulationMap.size(); ++i) {
            int nextModel = crossProductAccumulationMap.get(i);
            for (int j = 0; j < length; ++j) {
                crossProducts[j] += crossProducts[nextModel * length + j];
            }
        }
    }

    private List<Integer> createCrossProductAccumulationMap(BranchModel branchModel,
                                                            ComplexSubstitutionModel substitutionModel) {

        List<SubstitutionModel> substitutionModels = branchModel.getSubstitutionModels();
        List<Integer> map = new ArrayList<>();
        
        for (int i = 0; i < substitutionModels.size(); ++i) {
            if (substitutionModel == substitutionModels.get(i)) {
                map.add(i);
            }
        }

        return map;
    }

    @Override
    public Likelihood getLikelihood() {
        return treeDataLikelihood;
    }

    @Override
    public int getDimension() {
        return getParameter().getDimension();
    }

    @Override
    public String getReport() {

        StringBuilder sb = new StringBuilder();

        String message = GradientWrtParameterProvider.getReportAndCheckForError(this, 0.0, Double.POSITIVE_INFINITY, getReportTolerance());
        sb.append(message);

        if (DEBUG_CROSS_PRODUCTS) {
            sb.append("\n\tdifferentials: ").append(new WrappedVector.Raw(savedDifferentials, 0, savedDifferentials.length));
        }

        if (COUNT_TOTAL_OPERATIONS) {
            sb.append("\n\tgetCrossProductGradientCount = ").append(gradientCount);
            sb.append("\n\taverageGradientTime = ");
            if (gradientCount > 0) {
                sb.append(totalGradientTime / gradientCount);
            } else {
                sb.append("NA");
            }
            sb.append("\n");
        }

        return  sb.toString();
    }


    Double getReportTolerance() {
        return null;
    }

    // This has not been rigorously tested for epochs that change structure
    @SuppressWarnings("unused")
    protected void handleModelChangedEvent(Model model, Object object, int index) {
//        if (model == branchModel) {
//            updateCrossProductAccumulationMap();
//        }
        if (model == branchModel) {
//            crossProductAccumulationMap = createCrossProductAccumulationMap(branchModel, substitutionModel);
            throw new RuntimeException("Not yet implemented");
        }
    }

    public void modelChangedEvent(Model model, Object object, int index) {

    }

    public void modelRestored(Model model) {

    }

    protected static final boolean COUNT_TOTAL_OPERATIONS = false;
    protected static final boolean DEBUG_CROSS_PRODUCTS = false;

    protected double[] savedDifferentials;

    protected long gradientCount = 0;
    protected long totalGradientTime = 0;
}
