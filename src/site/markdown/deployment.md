# Deployment

**This is a work in progress document.**

The EHRI REST backend is deployed as a Neo4j unmanaged extension. In practice, this means:

 - it compiles to an "uberjar" (all dependencies in one jar) which live in the `plugins` directory of a Neo4j
 installation
 - the following setting in `neo4j.conf` maps our Jersey web URIs to a path (/ehri):
   * `dbms.unmanaged_extension_classes=eu.ehri.extension=/ehri`

In practice, a set of symlinks are used to allow easier versioning of releases:

The symlink `$NEO4J_HOME/plugins/ehri-rest.jar` points to `/opt/webapps/ehri-rest/current`,
which is itself a symlink to `/opt/webapps/ehri-rest/deploys/ehri-data-[TIMESTAMP]_[GIT-REV].jar`.

When new versions of the EHRI code is released the uberjar is uploaded to the `/opt/webapps/ehri-rest/deploys`
directory, named with the timestamp and the code's git revision. Then the `current` symlink is updated to point to
the new deployment and the Neo4j service restarted.

## Using the Fabfile for automated deployment tasks

The `fabfile.py` script provides a set of tasks that can be used to release new versions of the code. Once Fabric
(version 1.8) has been installed (typically via `sudo pip install fabric`) you can view the available tasks like so:

```bash
$> fab --list

Fabric deployment script for EHRI rest backend.

Available commands:

    clean_deploy         Build a clean version and deploy.
    copy_db              Copy a (not running) DB from the remote server.
    current_version      Show the current date/revision
    current_version_log  Output git log between HEAD and the current deployed version.
    deploy               Deploy the latest version of the site to the servers, install any
    latest               Point symlink at latest version.
    online_backup        Do an online backup to a particular directory on the server.
    online_clone_db      Copy a Neo4j DB from a server using the backup tool.
    prod                 Use the remote virtual server.
    reindex_all          Run a full reindex of Neo4j -> Solr data
    reindex_repository   Reindex items held by a repository.
    restart              Restart neo4j-service.
    rollback             Rollback to the last versioned dir and restart.
    stage                Use the remote staging server.
    start                Start neo4j-service.
    stop                 Stop neo4j-service.
    test                 Use the remote testing server.
    update_db            Update a Neo4j DB on a server.
```

More detailed info for tasks are available with the `-d` switch:

```bash
$> fab -d online_clone_db

Displaying detailed information for task 'online_clone_db':

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

