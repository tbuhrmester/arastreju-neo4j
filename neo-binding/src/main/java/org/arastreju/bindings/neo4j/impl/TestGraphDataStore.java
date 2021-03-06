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

import java.io.File;
import java.io.IOException;

import org.arastreju.sge.ArastrejuProfile;
import org.arastreju.sge.spi.ProfileCloseListener;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.test.TestGraphDatabaseFactory;

/**
 * <p>
 *  Container for a temporary test database. 
 * </p>
 *
 * <p>
 * 	Created Jun 6, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class TestGraphDataStore extends GraphDataStore {

    GraphDatabaseService gdbService;
    
    public TestGraphDataStore() throws IOException{
        super();
        try{
        gdbService = new TestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase();
        }catch(Exception any){
            throw new RuntimeException(any);
        }
    }
	
    
    public GraphDatabaseService getGdbService() {
        return gdbService;
    }
    
    /**
     * @return the indexManager
     */
    public IndexManager getIndexManager() {
        return gdbService.index();
    }
    
    // -- ProfileCloseListener ----------------------------
    
    /**
     * {@inheritDoc}
     */
    public void onClosed(final ArastrejuProfile profile) {
        close();
    }
    
    public void close() {
        gdbService.shutdown();
    }
	
	
}
