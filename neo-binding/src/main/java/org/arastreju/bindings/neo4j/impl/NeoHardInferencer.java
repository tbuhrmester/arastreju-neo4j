/*
 * Copyright (C) 2012 lichtflut Forschungs- und Entwicklungsgesellschaft mbH
 *
 * The Arastreju-Neo4j binding is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.arastreju.bindings.neo4j.impl;

import org.arastreju.sge.inferencing.CompoundInferencer;
import org.arastreju.sge.inferencing.implicit.InverseOfInferencer;
import org.arastreju.sge.persistence.ResourceResolver;

/**
 * <p>
 *  Inferencer for Neo 4j.
 * </p>
 *
 * <p>
 * 	Created Dec 1, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoHardInferencer extends CompoundInferencer {

	public NeoHardInferencer(final ResourceResolver resolver) {
		addInferencer(new InverseOfInferencer(resolver));
	}
	
}
