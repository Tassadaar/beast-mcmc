/*
 * LinearOrderTreePrecisionTraitProductProvider.java
 *
 * Copyright © 2002-2024 the BEAST Development Team
 * http://beast.community/about
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
 *
 */

package dr.evomodel.continuous.hmc;

import dr.evolution.tree.TreeTrait;
import dr.evomodel.treedatalikelihood.TreeDataLikelihood;
import dr.evomodel.treedatalikelihood.continuous.ContinuousDataLikelihoodDelegate;
import dr.evomodel.treedatalikelihood.preorder.WrappedNormalSufficientStatistics;
import dr.evomodel.treedatalikelihood.preorder.WrappedTipFullConditionalDistributionDelegate;
import dr.inference.model.Parameter;
import dr.math.MathUtils;
import dr.math.MaximumEigenvalue;
import dr.math.matrixAlgebra.*;
import dr.util.TaskPool;

import java.util.List;

/**
 * @author Marc A. Suchard
 */
public class LinearOrderTreePrecisionTraitProductProvider extends TreePrecisionTraitProductProvider {

    private final TreeTrait<List<WrappedNormalSufficientStatistics>> fullConditionalDensity;

    private static final boolean DEBUG = false;
    private static final boolean NEW_DATA = false; // Maybe not useful

    public LinearOrderTreePrecisionTraitProductProvider(TreeDataLikelihood treeDataLikelihood,
                                                        ContinuousDataLikelihoodDelegate likelihoodDelegate,
                                                        String traitName,
                                                        int threadCount,
                                                        double roughTimeGuess,
                                                        double optimalTravelTimeScalar,
                                                        int eigenvalueReplicates) {
        super(treeDataLikelihood, likelihoodDelegate);

        String fcdName = WrappedTipFullConditionalDistributionDelegate.getName(traitName);
        if (treeDataLikelihood.getTreeTrait(fcdName) == null) {
            likelihoodDelegate.addWrappedFullConditionalDensityTrait(traitName);
        }

        this.fullConditionalDensity = castTreeTrait(treeDataLikelihood.getTreeTrait(fcdName));

        this.delta = new double[tree.getExternalNodeCount()][dimTrait];
		
        this.roughTimeGuess = roughTimeGuess;
        this.optimalTravelTimeScalar = optimalTravelTimeScalar;
        this.eigenvalueReplicates = eigenvalueReplicates;

        this.taxonTaskPool = new TaskPool(tree.getExternalNodeCount(), threadCount);

        this.eigenvalue = new MaximumEigenvalue.PowerMethod(50, 0.01);
    }
    
    @Override
    public double[] getProduct(Parameter vector) {

        if (vector != dataParameter) {
            throw new IllegalArgumentException("May only compute for trait data vector");
        }

        final double[] result = new double[vector.getDimension()];

        if (taxonTaskPool.getNumThreads() == 1) { // single-threaded

            final List<WrappedNormalSufficientStatistics> allStatistics;
            if (NEW_DATA) {
                allStatistics = fullConditionalDensity.getTrait(tree, null);
                assert (allStatistics.size() == tree.getExternalNodeCount());
            }

            for (int taxon = 0; taxon < tree.getExternalNodeCount(); ++taxon) {

                final WrappedNormalSufficientStatistics statistic;
                if (NEW_DATA) {
                    statistic = allStatistics.get(taxon);
                } else {
                    List<WrappedNormalSufficientStatistics> statistics = fullConditionalDensity.getTrait(tree, tree.getExternalNode(taxon));
                    assert (statistics.size() == 1);
                    statistic = statistics.get(0);
                }

                computeProductForOneTaxon(taxon, statistic, result);
            }

        } else {

            final List<WrappedNormalSufficientStatistics> allStatistics = fullConditionalDensity.getTrait(tree, null);
            assert (allStatistics.size() == tree.getExternalNodeCount());

            taxonTaskPool.fork((taxon, thread) ->
                        computeProductForOneTaxon(taxon, allStatistics.get(taxon), result));
        }

        if (DEBUG) {
            debug(result, vector);
        }

        return result;
    }

    private void computeProductForOneTaxon(final int taxon,
                                           final WrappedNormalSufficientStatistics statistic,
                                           final double[] result) {

        final ReadableVector mean = statistic.getMean();
        final ReadableMatrix precision = statistic.getPrecision();
        final double scalar = statistic.getPrecisionScalar();
        final int resultOffset = taxon * dimTrait;

        computeDelta(taxon, delta[taxon], dataParameter, mean);
        computePrecisionDeltaProduct(result, resultOffset, precision, delta[taxon], scalar);
    }

    private static void computeDelta(final int taxon,
                                     final double[] delta,
                                     final Parameter data,
                                     final ReadableVector mean) {
        for (int i = 0, dim = delta.length; i < dim; ++i) {
            delta[i] = data.getParameterValue(taxon * dim + i) - mean.get(i);
        }
    }

    private static void computePrecisionDeltaProduct(final double[] result,
                                                     int offset,
                                                     final ReadableMatrix precision,
                                                     final double[] delta,
                                                     final double scalar) {
        final int dim = delta.length;

        for (int i = 0; i < dim; ++i) {
            double sum = 0.0;
            for (int j = 0; j < dim; ++j) {
                sum += precision.get(i, j) * delta[j];
            }
            result[offset] = sum * scalar;
            ++offset;
        }
    }

    @Override
    public double[] getMassVector() {
        return null; // TODO
    }

    @Override
    public double getTimeScale() {

        if (roughTimeGuess > 0.0) {
            return roughTimeGuess;
        } else {
            return getMaxEigenvalueAsTravelTime();
        }
    }

    @Override
    public double getTimeScaleEigen() {
        return eigenvalue.find(likelihoodDelegate.getTraitVariance());
    }

    private double getMaxEigenvalueAsTravelTime() {

        double treeCovEigenvalue = eigenvalue.find(likelihoodDelegate.getTreeVariance());
        double traitCovEigenvalue = eigenvalue.find(likelihoodDelegate.getTraitVariance());

        return optimalTravelTimeScalar * Math.sqrt(treeCovEigenvalue * traitCovEigenvalue);
    }

    @SuppressWarnings("unused")
    private double getRoughLowerBoundForTravelTime() {

        ReadableVector savedDataParameter = new WrappedVector.Raw(dataParameter.getParameterValues());

        double precisionMinEigenvalueLowerBound = 0.0;
        for (int i = 0; i < eigenvalueReplicates; ++i) {

            ReadableVector x = drawUniformSphere(dataParameter.getDimension());
            ReadableVector.Utils.setParameter(x, dataParameter);

            ReadableVector Phi_x = new WrappedVector.Raw(getProduct(dataParameter));

            precisionMinEigenvalueLowerBound += ReadableVector.Utils.innerProduct(x, Phi_x);

        }
        precisionMinEigenvalueLowerBound /= eigenvalueReplicates; // TODO Could compute average on sqrt(1/bound) scale

        ReadableVector.Utils.setParameter(savedDataParameter, dataParameter);

        return Math.sqrt(1 / precisionMinEigenvalueLowerBound);
    }

    private static WrappedVector drawUniformSphere(final int len) {

        double[] x = new double[len];
        double normSquare = 0.0;

        for (int i = 0; i < len; i++) {
            x[i] = MathUtils.nextGaussian();
            normSquare += x[i] * x[i];
        }

        double norm = Math.sqrt(normSquare);

        for (int i = 0; i < len; i++) {
            x[i] = x[i] / norm;
        }

        return new WrappedVector.Raw(x);
    }

    @SuppressWarnings("unchecked")
    static TreeTrait<List<WrappedNormalSufficientStatistics>> castTreeTrait(TreeTrait trait) {
        return trait;
    }

    private final TaskPool taxonTaskPool;

    private final double[][] delta;
    private final double roughTimeGuess;
    private final int eigenvalueReplicates;
    private final double optimalTravelTimeScalar;

    private final MaximumEigenvalue eigenvalue;
}
