package dr.evomodel.antigenic.phyloclustering.operators;


import dr.evomodel.antigenic.phyloclustering.TreeClusteringVirusesPrior;
import dr.inference.model.Parameter;
import dr.inference.operators.MCMCOperator;
import dr.xml.*;

public class ProbSitesGibbsOperatorParser extends AbstractXMLObjectParser {

    public final static String PROBSITES = "probSites";
    public final static String PROBSITE_ALPHA = "shape";
    public final static String PROBSITE_BETA = "shapeB";

    @Override
    public String getParserName() {
        return ProbSitesGibbsOperator.CLASSNAME_OPERATOR;
    }

    @Override
    public Object parseXMLObject(XMLObject xo) throws XMLParseException {
        double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);
        Parameter probSites = (Parameter) xo.getChild(PROBSITES).getChild(Parameter.class);
        TreeClusteringVirusesPrior clusterPrior = (TreeClusteringVirusesPrior) xo.getChild(TreeClusteringVirusesPrior.class);

        double probSiteAlpha = xo.hasAttribute(PROBSITE_ALPHA) ? xo.getDoubleAttribute(PROBSITE_ALPHA) : 1;
        double probSiteBeta = xo.hasAttribute(PROBSITE_BETA) ? xo.getDoubleAttribute(PROBSITE_BETA) : 1;

        return new ProbSitesGibbsOperator(weight, clusterPrior, probSites, probSiteAlpha, probSiteBeta);
    }

    @Override
    public String getParserDescription() {
        return "An operator that updates the probability of sites given a beta distribution.";
    }

    @Override
    public Class getReturnType() {
        return ProbSitesGibbsOperator.class;
    }

    @Override
    public XMLSyntaxRule[] getSyntaxRules() {
        return new XMLSyntaxRule[]{
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                AttributeRule.newDoubleRule(PROBSITE_ALPHA, true, "the alpha parameter in the Beta prior"),
                AttributeRule.newDoubleRule(PROBSITE_BETA, true, "the beta parameter in the Beta prior"),
                new ElementRule(TreeClusteringVirusesPrior.class),
                new ElementRule(PROBSITES, Parameter.class),
        };
    }
}
