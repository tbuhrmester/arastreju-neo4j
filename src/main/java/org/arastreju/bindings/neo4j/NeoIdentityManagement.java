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

import static org.arastreju.sge.SNOPS.associate;
import static org.arastreju.sge.SNOPS.remove;
import static org.arastreju.sge.SNOPS.singleObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.arastreju.bindings.neo4j.impl.SemanticNetworkAccess;
import org.arastreju.bindings.neo4j.index.ResourceIndex;
import org.arastreju.bindings.neo4j.query.NeoQueryBuilder;
import org.arastreju.sge.IdentityManagement;
import org.arastreju.sge.SNOPS;
import org.arastreju.sge.apriori.Aras;
import org.arastreju.sge.apriori.RDF;
import org.arastreju.sge.eh.ArastrejuException;
import org.arastreju.sge.eh.ArastrejuRuntimeException;
import org.arastreju.sge.eh.ErrorCodes;
import org.arastreju.sge.model.ResourceID;
import org.arastreju.sge.model.nodes.ResourceNode;
import org.arastreju.sge.model.nodes.SNResource;
import org.arastreju.sge.model.nodes.SemanticNode;
import org.arastreju.sge.model.nodes.views.SNEntity;
import org.arastreju.sge.model.nodes.views.SNText;
import org.arastreju.sge.query.Query;
import org.arastreju.sge.query.QueryResult;
import org.arastreju.sge.security.Credential;
import org.arastreju.sge.security.LoginException;
import org.arastreju.sge.security.Permission;
import org.arastreju.sge.security.Role;
import org.arastreju.sge.security.User;
import org.arastreju.sge.security.impl.PermissionImpl;
import org.arastreju.sge.security.impl.RoleImpl;
import org.arastreju.sge.security.impl.UserImpl;
import org.arastreju.sge.spi.GateContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *  Neo4J specific Identity Management. 
 * </p>
 *
 * <p>
 * 	Created Apr 29, 2011
 * </p>
 *
 * @author Oliver Tigges
 */
public class NeoIdentityManagement implements IdentityManagement {
	
	private final Logger logger = LoggerFactory.getLogger(NeoIdentityManagement.class);
	
	private final ResourceIndex index;
	
	private final SemanticNetworkAccess store;

	private final GateContext ctx;

	// -----------------------------------------------------
	
	/**
	 * Constructor.
	 * @param store The neo store.
	 * @param ctx The gate context.
	 */
	public NeoIdentityManagement(final SemanticNetworkAccess store, GateContext ctx) {
		this.store = store;
		this.ctx = ctx;
		this.index = store.getIndex();
	}
	
	// -----------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public User findUser(final String identity) {
		final QueryResult result = index.lookup(Aras.IDENTIFIED_BY, identity);
		if (result.size() > 1) {
			logger.error("More than on user with name '" + identity + "' found.");
			throw new IllegalStateException("More than on user with name '" + identity + "' found.");
		} else if (result.isEmpty()) {
			return null;
		} else {
			return new UserImpl(result.getSingleNode());
		}
	};

	/**
	 * {@inheritDoc}
	 */
	public User login(final String name, final Credential credential) throws LoginException {
		logger.debug("trying to login user '" + name + "'.");
		if (name == null) {
			throw new LoginException(ErrorCodes.LOGIN_INVALID_DATA, "No username given");	
		}
		final QueryResult found = index.lookup(Aras.IDENTIFIED_BY, name);
		if (found.size() > 1) {
			logger.error("More than on user with name '" + name + "' found.");
			throw new IllegalStateException("More than on user with name '" + name + "' found.");
		}
		if (found.isEmpty()){
			throw new LoginException(ErrorCodes.LOGIN_USER_NOT_FOUND, "User does not exist: " + name);	
		}
		
		final SNEntity user = found.getSingleNode().asEntity();
		if (!credential.applies(singleObject(user, Aras.HAS_CREDENTIAL))){
			throw new LoginException(ErrorCodes.LOGIN_USER_CREDENTIAL_NOT_MATCH, "Wrong credential");
		}
		
		return new UserImpl(user);
	}

	/**
	 * {@inheritDoc}
	 */
	public User register(final String uniqueName, final Credential credential) throws ArastrejuException {
		return register(uniqueName, credential, new SNEntity());
	}

	/**
	 * {@inheritDoc}
	 */
	public User register(final String name, final Credential credential, final ResourceNode corresponding) throws ArastrejuException {
		assertUniqueIdentity(name);
		associate(corresponding, Aras.HAS_UNIQUE_NAME, new SNText(name), Aras.IDENT);
		associate(corresponding, Aras.IDENTIFIED_BY, new SNText(name), Aras.IDENT);
		associate(corresponding, Aras.HAS_CREDENTIAL, new SNText(credential.stringRepesentation()), Aras.IDENT);
		associate(corresponding, RDF.TYPE, Aras.USER, Aras.IDENT);
		associate(corresponding, Aras.BELONGS_TO_DOMAIN, new SNText(ctx.getDomain()), Aras.IDENT);
		store.attach(corresponding);
		return new UserImpl(corresponding);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public User changeID(User user, String newID) throws ArastrejuException {
		assertUniqueIdentity(newID);
		final ResourceNode userNode = store.resolve(user.getAssociatedResource());
		SemanticNode uniqueNameNode = SNOPS.singleObject(userNode, Aras.HAS_UNIQUE_NAME);
		for (SemanticNode idNode : SNOPS.objects(userNode, Aras.IDENTIFIED_BY)) {
			if(idNode.toString().equals(uniqueNameNode.toString())) {
				SNOPS.assure(userNode, Aras.HAS_UNIQUE_NAME, new SNText(newID), Aras.IDENT);
				SNOPS.remove(userNode, Aras.IDENTIFIED_BY, idNode);
				associate(userNode, Aras.IDENTIFIED_BY, new SNText(newID), Aras.IDENT);
				break;
			}
		}
		return findUser(newID);
	}

	/** 
	* {@inheritDoc}
	*/
	public User registerAlternateID(User user, String uniqueName) throws ArastrejuException {
		assertUniqueIdentity(uniqueName);
		final ResourceNode node = store.resolve(user.getAssociatedResource());
		associate(node, Aras.IDENTIFIED_BY, new SNText(uniqueName), Aras.IDENT);
		return user;
	}
	
	// ----------------------------------------------------

	/**
	 * {@inheritDoc}
	 */
	public Role registerRole(final String name) {
		final ResourceNode existing = findItem(Aras.ROLE, name);
		if (existing != null) {
			return new RoleImpl(existing);
		}
		final SNResource role = new SNResource();
		associate(role, Aras.HAS_UNIQUE_NAME, new SNText(name), Aras.IDENT);
		associate(role, RDF.TYPE, Aras.ROLE, Aras.IDENT);
		store.attach(role);
		return new RoleImpl(role);
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<Role> getRoles() {
		final List<ResourceNode> nodes = index.lookup(RDF.TYPE, Aras.ROLE).toList();
		final Set<Role> roles = new HashSet<Role>(nodes.size());
		for(ResourceNode current: nodes) {
			roles.add(new RoleImpl(current));
		}
		return roles;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addUserToRoles(final User user, final Role... roles) {
		final ResourceNode userNode = user.getAssociatedResource();
		store.attach(userNode);
		for (Role role : roles) {
			associate(userNode, Aras.HAS_ROLE, role.getAssociatedResource(), Aras.IDENT);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void removeUserFromRoles(final User user, final Role... roles) {
		final ResourceNode userNode = user.getAssociatedResource();
		for (Role role : roles) {
			remove(userNode, Aras.HAS_ROLE, role.getAssociatedResource());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void addPermissionsToRole(final Role role, final Permission... permissions) {
		final ResourceNode roleNode = role.getAssociatedResource();
		store.attach(roleNode);
		for (Permission permission : permissions) {
			associate(roleNode, Aras.CONTAINS, permission.getAssociatedResource(), Aras.IDENT);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Permission registerPermission(final String name) {
		final ResourceNode existing = findItem(Aras.PERMISSION, name);
		if (existing != null) {
			return new PermissionImpl(existing);
		}
		final SNResource permission = new SNResource();
		associate(permission, Aras.HAS_UNIQUE_NAME, new SNText(name), Aras.IDENT);
		associate(permission, RDF.TYPE, Aras.PERMISSION, Aras.IDENT);
		store.attach(permission);
		return new PermissionImpl(permission);
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<Permission> getPermissions() {
		final List<ResourceNode> nodes = index.lookup(RDF.TYPE, Aras.PERMISSION).toList();
		final Set<Permission> permissions = new HashSet<Permission>(nodes.size());
		for(ResourceNode current: nodes) {
			permissions.add(new PermissionImpl(current));
		}
		return permissions;
	}
	
	// -----------------------------------------------------
	
	private ResourceNode findItem(final ResourceID type, final String name) {
		final Query query = new NeoQueryBuilder(index);
		query.addField(RDF.TYPE, type);
		query.and();
		query.addField(Aras.HAS_UNIQUE_NAME, name);
		final QueryResult result = query.getResult();
		if (result.size() > 1) {
			throw new ArastrejuRuntimeException(ErrorCodes.GENERAL_CONSISTENCY_FAILURE, 
					"Unique name is not unique for " + type + ": " + name);
		} else {
			return result.getSingleNode();
		}
	}
	
	protected void assertUniqueIdentity(final String name) throws ArastrejuException {
		final QueryResult found = index.lookup(Aras.IDENTIFIED_BY, name);
		if (found.size() > 0) {
			logger.error("More than on user with name '" + name + "' found.");
			throw new ArastrejuException(ErrorCodes.REGISTRATION_NAME_ALREADY_IN_USE, 
					"More than on user with name '" + name + "' found.");
		}
	}
	
}