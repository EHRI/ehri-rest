# Deployment

**This is a work in progress document.**

The EHRI REST backend is deployed as a Neo4j unmanaged extension. In practice, this means:

 - it compiles to a bunch of Jars which live in the `plugins/ehri` directory of a Neo4j installation
 - the following setting in `neo4j-server.conf` maps our Jersey web URIs to a path (/ehri):
   * `org.neo4j.server.thirdparty_jaxrs_classes=eu.ehri.extension=/ehri`

In practice, a set of symlinks are used to allow easier versioning of releases:

The symlink `$NEO4J_HOME/plugins/ehri` points to `/opt/webapps/ehri-rest/current`,
which is itself a symlink to `/opt/webapps/ehri-rest/deploys/[TIMESTAMP]_[GIT-REV]`.

When new versions of the EHRI code are released the Jars are uploaded to a directory in
`/opt/webapps/ehri-rest/deploys` that is named with the timestamp and the code's git revision. Then the `current`
symlink is updated to point to the new deployment and the Neo4j service restarted.

## Using the Fabfile for automated deployment tasks

The `fabfile.py` script provides a set of tasks that can be used to release new versions of the code. Once Fabric
(version 1.8) has been installed (typically via `sudo pip install fabric`) you can view the available tasks like so:

```bash
$> fab --list

Fabric deployment script for EHRI rest backend.

Available commands:

    clean_deploy   Build a clean version and deploy.
    clone_db       Copy a Neo4j DB from a server using the backup tool.
    deploy         Deploy the latest version of the site to the servers, install any
    latest         Point symlink at latest version.
    online_backup  Do an online backup to a particular directory on the server.
    restart        Restart neo4j-service.
    rollback       Rollback to the last versioned dir and restart.
    start          Start neo4j-service.
    stop           Stop neo4j-service.
    update_db      Update a Neo4j DB on a server.
```

More detailed info for tasks are available with the `-d` switch:

```bash
$> fab -d clone_db

Displaying detailed information for task 'clone_db':

    Copy a Neo4j DB from a server using the backup tool.
        This creates a copy of the running DB in /tmp, zips it,
        downloads the zip, extracts it to the specified DB, and
        cleans up.

        clone_db:/local/path/to/graph.db

    Arguments: local_dir
```

Three environments are available (which assume you have the hostnames defined in your .ssh/config file):

- test  (sets hostname to `ehritest`)
- stage (sets hostname to `ehristage`)
- prod  (sets hostname to `ehriprod`)

You can activate these environments by running them as commands prior to another task, i.e.

```bash
$> fab stage deploy # runs the deploy task with the 'ehristage' server
```

