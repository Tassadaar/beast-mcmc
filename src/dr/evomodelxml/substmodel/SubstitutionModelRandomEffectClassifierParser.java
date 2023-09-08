/*
 * GTRParser.java
 *
 * Copyright (c) 2002-2016 Alexei Drummond, Andrew Rambaut and Marc Suchard
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

package dr.evomodelxml.substmodel;

import dr.evolution.alignment.PatternList;
import dr.evomodel.branchmodel.EpochBranchModel;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.siteratemodel.GammaSiteRateModel;
import dr.evomodel.substmodel.GlmSubstitutionModel;
import dr.evomodel.substmodel.SubstitutionModelRandomEffectClassifier;
import dr.evomodel.tree.TreeModel;
import dr.xml.*;

public class SubstitutionModelRandomEffectClassifierParser extends AbstractXMLObjectParser {
    public static final String NAME = "SubstitutionModelRandomEffectClassifier";
    public static final String THRESHOLD = "threshold";

    public String getParserName() {
        return NAME;
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

//        TreeDataLikelihood likelihood = (TreeDataLikelihood)xo.getChild(TreeDataLikelihood.class);
        TreeModel tree = (TreeModel) xo.getChild(TreeModel.class);
        GlmSubstitutionModel glm = (GlmSubstitutionModel) xo.getChild(GlmSubstitutionModel.class);

//        EpochBranchModel epochs = null;
//        GammaSiteRateModel siteModel = null;

        EpochBranchModel epochs = (EpochBranchModel) xo.getChild(EpochBranchModel.class);
        GammaSiteRateModel siteModel = (GammaSiteRateModel) xo.getChild(GammaSiteRateModel.class);
        BranchRateModel branchRates = (BranchRateModel) xo.getChild(BranchRateModel.class);
        PatternList patternList = (PatternList)xo.getChild(PatternList.class);
        int nPatterns = 0;
        for (int i = 0; i < patternList.getPatternCount(); i++) {
            nPatterns += patternList.getPatternWeight(i);
        }

        double threshold = 1.0;
        if (xo.hasAttribute(THRESHOLD)) {
            threshold = xo.getDoubleAttribute(THRESHOLD);
        }

        return new SubstitutionModelRandomEffectClassifier(xo.getId(), tree, glm, epochs, branchRates, siteModel, nPatterns, threshold);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "A statistic that tracks whether a random-effect in a GLM substitution model is \"significant\" or not using a rule of thumb." +
                "Considers the expected number of substitutions across the given tree (using the specified branch rates and among site rate variation model)." +
                "For random-effect k, which affects i->j substitutions, compares expectation with random-effect at its given value to expectation with random-effect set to 0." +
                "If this difference exceeds the specified threshold, the random-effect is taken to be significant." +
                "This statistic should not currently be used for epochs if the clock model contains any time-dependent variation (such as, but not limited to, epochs)!";
    }

    public Class getReturnType() {
        return SubstitutionModelRandomEffectClassifier.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] rules = {
            new ElementRule(TreeModel.class,false),
            new ElementRule(GammaSiteRateModel.class,true),
            new ElementRule(BranchRateModel.class,false),
            new ElementRule(PatternList.class,false),
            AttributeRule.newDoubleRule(THRESHOLD,true,"If threshold is positive, this is used as the cutoff number of substitutions and the statistic returns 0/1 for each random-effect. Otherwise, the difference in expected substitution counts is returned for each random-effect.")
    };
}
