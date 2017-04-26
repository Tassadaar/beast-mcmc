package dr.inference.model;

import dr.evomodel.continuous.FullyConjugateMultivariateTraitLikelihood;

/**
 * @author Max Tolkoff
 */
public class FullyConjugateTreeTipsPotentialDerivative implements GradientProvider {

    private final FullyConjugateMultivariateTraitLikelihood treeLikelihood;
    private final Parameter traitParameter;

    public FullyConjugateTreeTipsPotentialDerivative(FullyConjugateMultivariateTraitLikelihood treeLikelihood){
        this.treeLikelihood = treeLikelihood;
        traitParameter = treeLikelihood.getTraitParameter();
    }

    @Override
    public int getDimension() {
        return treeLikelihood.getTraitParameter().getDimension();
    }

//    @Override
//    public void getGradient(double[] destination, int offset) {
//        throw new RuntimeException("Not yet implemented");
//    }

    @Override
    public double[] getGradient() {

        int dimTraits = treeLikelihood.getDimTrait() * treeLikelihood.getNumData();
        int ntaxa = traitParameter.getDimension() / dimTraits;
        double[] derivative = new double[traitParameter.getDimension()];
        double[][] mean = treeLikelihood.getConditionalMeans();
        double[] precfactor = treeLikelihood.getPrecisionFactors(); //TODO need to be flexible to handle diagonal case and dense case. Right now just diagonal.

        for (int i = 0; i < dimTraits; i++) {
            for (int j = 0; j < ntaxa; j++) {
                derivative[j * dimTraits + i] += (traitParameter.getParameterValue(j * dimTraits + i) - mean[j][i]) * precfactor[j];
            }

        }


        return derivative;
    }
}
