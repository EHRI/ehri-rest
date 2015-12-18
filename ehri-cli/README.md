ehri-cli
============
As the name suggests, this module provides a limited (but expanding) command-line interface to the EHRI Neo4j database tools. There is currently no "installer", so it can only be run from the ehri-data project root directory, using the scripts/cmd shell wrapper.

While command-line access through the REST interface is planned, currently you need to specify the Neo4j database directory, which the commands access in an embedded manner.

Usage
-----
Commands take the form:

	cmd <database-dir> <command-name> [OPTIONS] <args1> .. <argN>

The few commands that are currently available are:

	load-fixtures

Load the fixtures into the specified DB. The fixtures are currently hard-coded as those used in the ehri-core tests, but this will become more flexible.

	import-ead -user <user-id> -repo <repository-id> [-createuser] [-createrepo] <ead-file.xml>

Import an EAD file into the specified database. The `-repo <repo>` and `-user <user>` flags are mandatory, specifying which user performed the action, and which repository the created docs belong to. If `-createuser` and `-createrepo` flags are given, these will be created. The `-tolerant` flag tells the importer not to error if an individual document fails to import.

	list <entityType>

List the IDs for a given entity type.

	get <entityType> <entity-id>

Get the JSON representation of a particular entity type, specified by its ID.

	user-list -user <user-id> <entityTypes>

List the IDs for a particular entity type that a given user can read.
	
