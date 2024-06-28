/*
 * TreeStatistic.java
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

package dr.evomodel.tree;

import dr.evolution.tree.Tree;
import dr.inference.model.Statistic;

/**
 * An interface for statistics on trees
 *
 * @version $Id: TreeStatistic.java,v 1.14 2005/05/24 20:25:58 rambaut Exp $
 *
 * @author Andrew Rambaut
 *
 */
public abstract class TreeStatistic extends Statistic.Abstract {
       
	public static final String TREE = "tree";

	public TreeStatistic(String name) {
		super(name);
	}

	public abstract void setTree(Tree tree);
	public abstract Tree getTree();
}
