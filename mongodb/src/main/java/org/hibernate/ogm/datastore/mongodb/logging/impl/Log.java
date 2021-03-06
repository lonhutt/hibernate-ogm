/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.logging.impl;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import com.mongodb.MongoException;

/**
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@LogMessage(level = INFO)
	@Message(id = 1201, value = "Connecting to MongoDB at %1$s with a timeout set at %2$d millisecond(s)")
	void connectingToMongo(String host, int timeout);

	@LogMessage(level = INFO)
	@Message(id = 1202, value = "Closing connection to MongoDB")
	void disconnectingFromMongo();

	@Message(id = 1203, value = "Unable to find or initialize a connection to the MongoDB server")
	HibernateException unableToInitializeMongoDB(@Cause RuntimeException e);

	@LogMessage(level = INFO)
	@Message(id = 1206, value = "Mongo database named [%s] is not defined. Creating it!")
	void creatingDatabase(String dbName);

	@LogMessage(level = INFO)
	@Message(id = 1207, value = "Connecting to Mongo database named [%s].")
	void connectingToMongoDatabase(String dbName);

	@Message(id = 1209, value = "The database named [%s] cannot be dropped")
	HibernateException unableToDropDatabase(@Cause MongoException e, String databaseName);

	@LogMessage(level = TRACE)
	@Message(id = 1210, value = "Removed [%d] associations")
	void removedAssociation(int nAffected);

	@Message(id = 1214, value = "Unable to connect to MongoDB instance: %1$s" )
	HibernateException unableToConnectToDatastore(String message, @Cause Exception e);

	@Message(id = 1217, value = "The following native query does neither specify the collection name nor is its result type mapped to an entity: %s")
	HibernateException unableToDetermineCollectionName(String nativeQuery);

	@LogMessage(level = WARN)
	@Message(id = 1218, value = "Cannot use primary key column name '%s' for id generator, going to use '%s' instead")
	void cannotUseGivenPrimaryKeyColumnName(String givenKeyColumnName, String usedKeyColumnName);

	@Message(id = 1219, value = "Database %s does not exist. Either create it yourself or set property '" + OgmProperties.CREATE_DATABASE + "' to true.")
	HibernateException databaseDoesNotExistException(String databaseName);

	// The following statements have to return MappingException to make sure Hibernate ORM doesn't wrap them in a generic failure
	// but maintains the user friendly error message

	@Message(id = 1220, value = "When using MongoDB it is not valid to use a name for a table (a collection) which starts with the 'system.' prefix."
			+ " Please change name for '%s', for example by using @Table ")
	MappingException collectionNameHasInvalidSystemPrefix(String qualifiedName);

	@Message(id = 1221, value = "When using MongoDB it is not valid to use a name for a table (a collection) which contains the NUL character '\\0'."
			+ " Please change name for '%s', for example by using @Table ")
	MappingException collectionNameContainsNULCharacter(String qualifiedName);

	@Message(id = 1222, value = "When using MongoDB it is not valid to use a name for a table (a collection) which contains the dollar character '$';"
			+ " for example this is a common problem with inner classes."
			+ " Please pick a valid collection name for '%s', for example by using @Table ")
	MappingException collectionNameContainsDollarCharacter(String qualifiedName);

	@Message(id = 1223, value = "When using MongoDB it is not valid to use a field name which starts with the prefix '$'."
			+ " Please change name for '%s', for example by using @Column ")
	MappingException fieldNameHasInvalidDollarPrefix(String columnName);

	@Message(id = 1224, value = "When using MongoDB it is not valid to use a field name which contains the NUL character '\\0'."
			+ " Please change name for '%s', for example by using @Column ")
	MappingException fieldNameContainsNULCharacter(String fieldName);

	@Message(id = 1225, value = "This WriteConcern has been deprecated or removed by MongoDB: %s")
	HibernateException writeConcernDeprecated(String writeConcern);

	@Message(id = 1226, value = "Unable to use reflection on invoke method '%s#%s' via reflection.")
	HibernateException unableToInvokeMethodViaReflection(String clazz, String method);

}
