/*
 * ProbSitesGibbsOperator.java
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

package dr.evomodel.antigenic.phyloclustering.operators;

import cern.jet.random.Beta;
import dr.evomodel.antigenic.phyloclustering.TreeClusteringVirusesPrior;
import dr.inference.model.Parameter;
import dr.inference.operators.GibbsOperator;
import dr.inference.operators.MCMCOperator;
import dr.inference.operators.SimpleMCMCOperator;
import dr.math.MathUtils;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.AttributeRule;
import dr.xml.ElementRule;
import dr.xml.XMLObject;
import dr.xml.XMLObjectParser;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;


/**
 * A Gibbs operator for allocation of items to clusters under a distance dependent Chinese restaurant process.
 *
 * @author Charles Cheung
 * @author Trevor Bedford
 */
public class ProbSitesGibbsOperator extends SimpleMCMCOperator implements GibbsOperator {

    public final static String CLASSNAME_OPERATOR = "probSitesGibbsOperator";

    private TreeClusteringVirusesPrior clusterPrior;
    private Parameter probSites;
    private int numSites;

    private double probSiteAlpha = 1;
    private double probSiteBeta = 1;

    public ProbSitesGibbsOperator(double weight, TreeClusteringVirusesPrior clusterPrior, Parameter probSites, double probSiteAlpha, double probSiteBeta) {
        this.clusterPrior = clusterPrior;
        this.probSites = probSites;
        this.probSiteAlpha = probSiteAlpha;
        this.probSiteBeta = probSiteBeta;
        this.numSites = clusterPrior.getNumSites();
        setWeight(weight);
    }

    public double doOperation() {
        int[] causalCount = clusterPrior.getCausalCount();
        int[] nonCausalCount = clusterPrior.getNonCausalCount();
        int whichSite = selectRandomSite();
        double value = calculateBetaDistributionValue(causalCount[whichSite], nonCausalCount[whichSite]);
        probSites.setParameterValue(whichSite, value);
        return 0;
    }

    private int selectRandomSite() {
        return (int) Math.floor(MathUtils.nextDouble() * numSites);
    }

    private double calculateBetaDistributionValue(int causalCount, int nonCausalCount) {
        return Beta.staticNextDouble(causalCount + probSiteAlpha, nonCausalCount + probSiteBeta);
    }

    public void accept(double deviation) {
        super.accept(deviation);
    }

    public void reject() {
        super.reject();
    }

    public final String getOperatorName() {
        return CLASSNAME_OPERATOR;
    }

    public static final XMLObjectParser PARSER = new ProbSitesGibbsOperatorParser();

    public int getStepCount() {
        return 1;
    }
}