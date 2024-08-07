/*
 * ConstraintsTreeLikelihoodTest.java
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

package test.dr.evomodel.bigfasttree.thorney;

import dr.evolution.io.Importer;
import dr.evolution.io.NewickImporter;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.tree.TreeUtils;
import dr.evolution.util.Taxon;
import dr.evomodel.bigfasttree.BigFastTreeModel;
import dr.evomodel.bigfasttree.thorney.ConstraintsTreeLikelihood;
import dr.evomodel.tree.TreeModel;
import junit.framework.TestCase;

import java.io.IOException;


public class ConstraintsTreeLikelihoodTest extends TestCase {
    public void setUp() throws Exception {
        super.setUp();

        NewickImporter importer = new NewickImporter("(((1:1.0,(3:1.0,5:1.0)):1.0,2:2.0):1.0,(4:3.0,6:1.0):0.1);");
        NewickImporter constraintsImporter = new NewickImporter("((1:1.0,3:1.0,2:1.0):1.0,4:1.0);");

        Tree tree = importer.importTree(null);
        Tree constraintsTree = constraintsImporter.importTree(null);

        targetTree = new BigFastTreeModel(tree);

        constraintsTreeLikelihood = new ConstraintsTreeLikelihood("constrainedTreeLikelihood", targetTree, constraintsTree);
    }

    public void testConstraintsMet() {
        assertEquals(0.0, constraintsTreeLikelihood.getLogLikelihood());
    }

    public void testConstaintsRepected() {


        Taxon selectedTaxon1 = targetTree.getTaxon(targetTree.getTaxonIndex("3"));
        Taxon selectedTaxon2 = targetTree.getTaxon(targetTree.getTaxonIndex("2"));
        NodeRef selectedNode1 = null;
        NodeRef selectedNode2 = null;
        for (int j = 0; j < targetTree.getExternalNodeCount(); j++) {
            NodeRef tip = targetTree.getExternalNode(j);
            if (targetTree.getNodeTaxon(tip).equals(selectedTaxon1)) {
                selectedNode1 = tip;
            } else if (targetTree.getNodeTaxon(tip).equals(selectedTaxon2)) {
                selectedNode2 = tip;
            }
        }

        NodeRef parent1 = targetTree.getParent(selectedNode1);
        NodeRef parent2 = targetTree.getParent(selectedNode2);

        targetTree.beginTreeEdit();
        targetTree.removeChild(parent1, selectedNode1);
        targetTree.removeChild(parent2, selectedNode2);

        targetTree.addChild(parent1, selectedNode2);
        targetTree.addChild(parent2, selectedNode1);
        targetTree.endTreeEdit();


        assertEquals(0.0, constraintsTreeLikelihood.getLogLikelihood());
        constraintsTreeLikelihood.makeDirty();
        assertEquals(0.0, constraintsTreeLikelihood.getLogLikelihood());
    }





    public void testShouldThrowError() throws IOException, Importer.ImportException {
        NewickImporter importer = new NewickImporter("((1,2),3);");
        NewickImporter constraintsImporter = new NewickImporter("((1,2),4)");

        Tree tree = importer.importTree(null);
        Tree constraintsTree = constraintsImporter.importTree(null);
        targetTree = new BigFastTreeModel(tree);

        try {
            new ConstraintsTreeLikelihood("MYSTAT", targetTree, constraintsTree);
            fail("Missing exception");
        } catch (TreeUtils.MissingTaxonException e) {
            assertEquals("4", e.getMessage()); // Optionally make sure you get the correct message, too
        }

    }

    public void testConstaintsViolated() {

        Taxon selectedTaxon1 = targetTree.getTaxon(targetTree.getTaxonIndex("4"));
        Taxon selectedTaxon2 = targetTree.getTaxon(targetTree.getTaxonIndex("1"));
        NodeRef selectedNode1 = null;
        NodeRef selectedNode2 = null;
        for (int j = 0; j < targetTree.getExternalNodeCount(); j++) {
            NodeRef tip = targetTree.getExternalNode(j);
            if (targetTree.getNodeTaxon(tip).equals(selectedTaxon1)) {
                selectedNode1 = tip;
            } else if (targetTree.getNodeTaxon(tip).equals(selectedTaxon2)) {
                selectedNode2 = tip;
            }
        }
        constraintsTreeLikelihood.getLogLikelihood();
        NodeRef parent1 = targetTree.getParent(selectedNode1);
        NodeRef parent2 = targetTree.getParent(selectedNode2);

        targetTree.beginTreeEdit();
        targetTree.removeChild(parent1, selectedNode1);
        targetTree.removeChild(parent2, selectedNode2);

        targetTree.addChild(parent1, selectedNode2);
        targetTree.addChild(parent2, selectedNode1);
        targetTree.endTreeEdit();


        assertEquals(Double.NEGATIVE_INFINITY, constraintsTreeLikelihood.getLogLikelihood());

        constraintsTreeLikelihood.makeDirty();
        assertEquals(Double.NEGATIVE_INFINITY, constraintsTreeLikelihood.getLogLikelihood());

    }

    //TODO add tests for all nodes tracked - the logic hasn't changed it's just not covered by the tests anymore


    public void testConstaintsViolatedWithUnTractedtip() {

        Taxon selectedTaxon1 = targetTree.getTaxon(targetTree.getTaxonIndex("5"));
        Taxon selectedTaxon2 = targetTree.getTaxon(targetTree.getTaxonIndex("4"));
        NodeRef selectedNode1 = null;
        NodeRef selectedNode2 = null;

        for (int j = 0; j < targetTree.getExternalNodeCount(); j++) {
            NodeRef tip = targetTree.getExternalNode(j);
            if (targetTree.getNodeTaxon(tip).equals(selectedTaxon1)) {
                selectedNode1 = tip;
            } else if (targetTree.getNodeTaxon(tip).equals(selectedTaxon2)) {
                selectedNode2 = tip;
            }
        }

        NodeRef parent1 = targetTree.getParent(selectedNode1);
        NodeRef parent2 = targetTree.getParent(selectedNode2);

        targetTree.beginTreeEdit();
        targetTree.removeChild(parent1, selectedNode1);
        targetTree.removeChild(parent2, selectedNode2);

        targetTree.addChild(parent1, selectedNode2);
        targetTree.addChild(parent2, selectedNode1);
        targetTree.endTreeEdit();

        assertEquals(Double.NEGATIVE_INFINITY, constraintsTreeLikelihood.getLogLikelihood());
        constraintsTreeLikelihood.makeDirty();
        assertEquals(Double.NEGATIVE_INFINITY, constraintsTreeLikelihood.getLogLikelihood());
    }


    private ConstraintsTreeLikelihood constraintsTreeLikelihood;
    private TreeModel targetTree;
}

