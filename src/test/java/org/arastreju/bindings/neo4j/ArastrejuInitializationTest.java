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
package org.arastreju.bindings.neo4j;

import java.io.File;

import junit.framework.Assert;

import org.arastreju.sge.Arastreju;
import org.arastreju.sge.ArastrejuGate;
import org.arastreju.sge.ArastrejuProfile;
import org.junit.Test;


/**
 * <p>
 *  Test cases for initialization of Arastreju.
 * </p>
 *
 * <p>
 * 	Created Jan 4, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class ArastrejuInitializationTest {
	
	@Test
	public void testRootGate() {
		final File tempDir = new File(System.getProperty("java.io.tmpdir"), Long.toString(System.currentTimeMillis()));
		
		final ArastrejuProfile profile = new ArastrejuProfile();
		profile.setProperty(ArastrejuProfile.GATE_FACTORY, Neo4jGateFactory.class.getCanonicalName());
		profile.setProperty(ArastrejuProfile.ARAS_STORE_DIRECTORY, tempDir.getAbsolutePath());
		
		final Arastreju aras = Arastreju.getInstance(profile);
		Assert.assertNotNull(aras);
		
		final ArastrejuGate root = aras.rootContext();
		Assert.assertNotNull(root);
		
		final ArastrejuGate myDomain = aras.rootContext("mydomain");
		Assert.assertNotNull(myDomain);
	}
	
	@Test
	public void testTempRootGate(){
		final ArastrejuGate rootGate = Arastreju.getInstance().rootContext();
		
		Assert.assertNotNull(rootGate);
		
		Assert.assertNotNull(rootGate.startConversation());
		
		rootGate.close();
		
	}
	
	@Test
	public void testTempRootGateForDomain(){
		final ArastrejuGate rootGate = Arastreju.getInstance().rootContext("mydomain");
		
		Assert.assertNotNull(rootGate);
		
		Assert.assertNotNull(rootGate.startConversation());
		
		rootGate.close();
	}
	
}