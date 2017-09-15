"""
Fabric deployment script for EHRI rest backend.
"""

from __future__ import with_statement

import os
import subprocess
from datetime import datetime

from fabric.api import *
from fabric.utils import abort, error, warn
from fabric.contrib import files
from fabric.contrib.console import confirm
from contextlib import contextmanager as _contextmanager

# globals
env.project_name = 'ehri-rest'
env.service_name = 'neo4j-service'
env.prod = False
env.path = '/opt/webapps/' + env.project_name
env.neo4j_install = '/opt/webapps/neo4j-version'
env.data_dir = '/opt/webapps/data/neo4j/databases'
env.index_helper = "/opt/webapps/docview/bin/indexer.jar"
env.properties_location = '/opt/webapps/data/import-data/properties/'
env.solr_url = 'http://localhost:8983/solr/portal'
env.service_url = 'http://localhost:7474/ehri'
env.user = os.getenv("USER")
env.use_ssh_config = True

TIMESTAMP_FORMAT = "%Y%m%d%H%M%S"

# environments
@task
def test():
    "Use the remote testing server."
    env.hosts = ['ehri-test-01']

@task
def stage():
    "Use the remote staging server."
    env.hosts = ['ehri-stage-01']

@task
def prod():
    "Use the remote virtual server."
    env.hosts = ['ehri-portal-01']
    env.prod = True

@task
def deploy():
    """
    Deploy the latest version of the site to the servers, install any
    required third party modules, and then restart the webserver
    """
    with settings(version = get_version_stamp(), artifact = get_artifact_version()):
        copy_to_server()
        symlink_current()
        restart()

@task
def clean_deploy():
    """Build a clean version and deploy."""
    local('mvn clean package -DskipTests')
    deploy()

@task
def start():
    "Start neo4j-service."
    # NB: This doesn't use sudo() directly because it insists on asking
    # for a password, even though we should have NOPASSWD in visudo.
    run('sudo service %(service_name)s start' % env, pty=False, shell=False)

@task
def stop():
    "Stop neo4j-service."
    # NB: This doesn't use sudo() directly because it insists on asking
    # for a password, even though we should have NOPASSWD in visudo.
    run('sudo service %(service_name)s stop' % env, pty=False, shell=False)

@task
def restart():
    "Restart neo4j-service."
    # NB: This doesn't use sudo() directly because it insists on asking
    # for a password, even though we should have NOPASSWD in visudo.
    if confirm("Restart Neo4j server?"):
        run('sudo service %(service_name)s restart' % env, pty=False, shell=False)

@task
def rollback():
    "Rollback to the last versioned dir and restart."
    with cd(env.path):
        # get the version number from ehri-data-YYYYMMddHHMMSS_123456.jar
        output = run("ls -1rt deploys | tail -n 2 | head -n 1 | cut -c 11-32").strip()
        if output == "":
            raise Exception("Unable to get previous version for rollback!")
        with settings(version=output):
            symlink_current()
            print("Current version is now: %s" % output)
            restart()

@task
def latest():
    "Point symlink at latest version."
    with cd(env.path):
        output = run("ls -1rt deploys | tail -n 1 | cut -c 11-32").strip()
        if output == "":
            raise Exception("Unable to get latest version for rollback!")
        with settings(version=output):
            symlink_current()
            print("Current version is now: %s" % output)
            restart()

@task
def online_backup(remote_dir, tar = True):
    """
    Do an online backup to a particular directory on the server. The
    directory will be tar/gzipped but the supplied path should not
    include the gzip extension.

    online_backup:/path/on/server/backup.graph.db
    """
    if files.exists(remote_dir):
        abort("Remote directory '%s' already exists!" % remote_dir)
    tar_name = remote_dir + ".tgz"
    dirname = os.path.dirname(remote_dir)
    basename = os.path.basename(remote_dir)
    with settings(dirname=dirname, basename=basename):
        run("%(neo4j_install)s/bin/neo4j-admin backup --from localhost:6362 --name=%(basename)s --backup-dir %(dirname)s" % env)
        if tar:
            run("tar --create --gzip --file %s -C %s ." % (tar_name, remote_dir))
            run("rm -rf " + remote_dir)

@task
def update_properties():
    """
    Put a fresh copy of the working copy .properties files on the server.
    """
    srcname = "props.tgz"
    dstpath = env.properties_location
    dstfile = os.path.join(dstpath, srcname)

    with lcd("ehri-io/src/main/resources/"):
        local("tar -czf %s *.properties" % srcname)

        # upload the gzip
        print("Running put")
        put(srcname, dstfile)
        # extract it
        with cd(dstpath):
            run("tar --extract --gzip --no-overwrite-dir --touch --overwrite --file %s" % srcname)
        # delete the zip
        run("rm %s" % dstfile)
        local("rm %s" % srcname)

@task
def load_queue():
    """Run the queue.sh script to run imports from the queue.
    
    """
    with cd(env.path):
        stop()
        run("./scripts/queue.sh")
        start()

@task
def import_ead(scope, log, properties, file_dir):
    """Import EAD files remotely via REST

    Supply the scope, log message as a file name, the path to a
    properties file and path to a directory relative to /opt/webapps/data/import-data containing
    the files to import. File paths are local on the remote machine.
    The $USER is used to run the import, and tolerant is always true when using this import.

    For example, to import all files from the Wiener Library, use:

    fab stage import_ead:scope=gb-003348,log=wiener-log.txt,properties=wienerlib.properties,file_dir=gb/wiener-library"""

    run("ls /opt/webapps/data/import-data/%s/*.xml > /opt/webapps/data/import-metadata/%s.txt" % (file_dir, scope))
    log_file = "/opt/webapps/data/import-data/logs/%s" % log
    properties_file = "/opt/webapps/data/import-data/properties/%s" % properties
    file_list = "/opt/webapps/data/import-metadata/%s.txt" % scope
    run("curl -m 7200 -X POST -H X-User:$USER --data-binary @%s -H \"Content-Type: text/plain\" \"http://localhost:7474/ehri/import/ead?scope=%s&log=%s&tolerant=true&properties=%s\"" % (file_list, scope, log_file, properties_file) )

@task
def import_ead_with_handler(scope, log, properties, file_dir, handler):
    """Import EAD files remotely via REST

    Supply the scope, log message as a file name, the path to a
    properties file and path to a directory relative to /opt/webapps/data/import-data containing
    the files to import. File paths are local on the remote machine.
    The $USER is used to run the import, and tolerant is always true when using this import.

    For example, to import all files from the Wiener Library, use:

    fab stage import_ead_with_handler:scope=gb-003348,log=wiener-log.txt,properties=wienerlib.properties,file_dir=gb/wiener-library,handler=eu.ehri.project.importers.EadHandler"""

    run("ls /opt/webapps/data/import-data/%s/*.xml > /opt/webapps/data/import-metadata/%s.txt" % (file_dir, scope))
    log_file = "/opt/webapps/data/import-data/logs/%s" % log
    properties_file = "/opt/webapps/data/import-data/properties/%s" % properties
    file_list = "/opt/webapps/data/import-metadata/%s.txt" % scope
    run("curl -m 7200 -X POST -H X-User:$USER --data-binary @%s -H \"Content-Type: text/plain\" \"http://localhost:7474/ehri/import/ead?scope=%s&log=%s&tolerant=true&properties=%s&handler=%s\"" % (file_list, scope, log_file, properties_file, handler) )

@task
def import_large_ead_with_handler(scope, log, properties, file_dir, handler):
    """Import a large number of EAD files remotely via REST

    Supply the scope, log message as a file name, the path to a
    properties file and path to a directory relative to /opt/webapps/data/import-data containing
    the files to import. File paths are local on the remote machine.
    The $USER is used to run the import, and tolerant is always true when using this import.

    For example, to import all files from the USHMM, use:

    fab stage import_large_ead_with_handler:scope=us-005578,log=us-005578.log,properties=ushmm.properties,file_dir=us/ushmm,handler=eu.ehri.project.importers.UshmmHandler"""

    log_file = "/opt/webapps/data/import-data/logs/%s" % log
    properties_file = "/opt/webapps/data/import-data/properties/%s" % properties
    with cd(env.path):
        run("./scripts/import-large-batch.sh %s %s %s %s %s" % (file_dir, scope, log_file, properties_file, handler) )

@task
def import_skos(scope, log, file):
    """Import SKOS files remotely via REST

    Supply the scope, log message as a file name or as an URL encoded message and path 
    to an RDF file relative to /opt/webapps/data/import-data containing
    the vocabulary to import. File paths are local on the remote machine.
    The $USER is used to run the import, and tolerant is always true when using this import.

    For example, to import the list of camps, use:

    fab stage import_skos:scope=ehri-camps,log=This+list+of+camps+has+been+compiled+by+EHRI+in+2014,file=authoritativeSet/camps-import.rdf"""

    full_file_path = "/opt/webapps/data/import-data/" + file
    run("curl -X POST -H X-User:$USER --data-binary @%s \"http://localhost:7474/ehri/import/skos?scope=%s&log=%s&tolerant=true\"" % (full_file_path, scope, log) )

@task
def import_csv(scope, log, importer, file):
    """Import CSV files remotely via REST

    Supply the scope, log message as a file name or as an URL encoded message, the fully
    qualified name of a CSV importer class and path to a file relative to 
    /opt/webapps/data/import-data to import. File paths are local on the remote machine.
    The $USER is used to run the import.

    For example, to import something, use:

    fab stage import_csv:scope=terezin-victims,log=terezin.log,importer=PersonalitiesImporter,file=wp2/terezin/authoritativeSet/terezin-victims.csv"""

    full_file_path = "/opt/webapps/data/import-data/" + file
    full_log_path= "/opt/webapps/data/import-data/logs/" + log
    run("curl -X POST -H X-User:$USER --data-binary @%s \"http://localhost:7474/ehri/import/csv?scope=%s&log=%s&importer=%s\"" % (full_file_path, scope, full_log_path, importer) )

@task
def online_clone_db(local_dir):
    """Copy a Neo4j DB from a server using the backup tool.
    This creates a copy of the running DB in /tmp, zips it,
    downloads the zip, extracts it to the specified DB, and
    cleans up.
    
    online_clone_db:/local/path/to/graph.db
    """
    timestamp = get_timestamp()
    with settings(tmpdst = "/tmp/" + timestamp):
        online_backup(env.tmpdst, tar = False)
        run("tar --create --gzip --file %(tmpdst)s.tgz -C %(tmpdst)s ." % env)
        get(env.tmpdst + ".tgz", env.tmpdst + ".tgz")
        run("rm -rf %(tmpdst)s %(tmpdst)s.tgz" % env)
        local("mkdir -p " + local_dir)
        local("tar xf /tmp/%s.tgz -C %s" % (timestamp, local_dir))
        local("rm " + env.tmpdst + ".tgz")


# @hosts("ehristage")
# def copy_db_to_staging(local_tar, staging_dir):
#     """
#     Do an online backup of the production graph database and copy it to the staging server.
#     
#     online_clone_to_staging:/path/on/server/backup.graph.db
#     """
#     stage()
#     put(local_tar, staging_dir + "")
#     prod()


@task
def copy_db(local_dir):
    """Copy a (not running) DB from the remote server.
    
    copy_db:/local/path/to/graph.db
    """
    if confirm("Stop Neo4j server?"):
        stop()

        remote_db_dir = "%(data_dir)s/graph.db" % env
        temp_file = our_temp_file = run("mktemp")
        if not os.path.exists(local_dir):
            os.mkdir(local_dir)

        run("tar --create --gzip --file %s -C %s ." % (temp_file, remote_db_dir))
        get(temp_file, os.path.dirname(our_temp_file))
        local("tar --extract --gzip --file %s -C %s" % (our_temp_file, local_dir))
        run("rm %s" % temp_file)
        os.unlink(our_temp_file)

        if confirm("Restart Neo4j server?"):
            start()
            
@task
def copy_db_test(local_dir):
    """Copy a (not running) DB from the remote TEST server. The TEST server runs a
    different version of mktemp and has different permissions (e.g. no sudo without tty).
    
    copy_db_test:/local/path/to/graph.db
    """
    

    remote_db_dir = "%(data_dir)s/graph.db" % env
    temp_file = our_temp_file = run("mktemp")
    run("mv %s %s" % (temp_file, temp_file + ".tgz"))
    if not os.path.exists(local_dir):
        os.mkdir(local_dir)

    run("tar --create --gzip --file %s -C %s ." % (temp_file, remote_db_dir))
    get(temp_file, os.path.dirname(our_temp_file))
    local("tar --extract --gzip --file %s -C %s" % (our_temp_file, local_dir))
    run("rm %s" % temp_file)
    os.unlink(our_temp_file)

    if confirm("Restart Neo4j server?"):
        start()

@task
def update_db(local_dir):
    """Update a Neo4j DB on a server.    
    Tar the input dir for upload, upload it, stop the server,
    move the current DB out of the way, and unzip it.
    
    update_db:/local/path/to/graph.db
    """
    # Check we have a reasonable path...
    if not os.path.exists(os.path.join(local_dir, "index.db")):
        raise Exception("This doesn't look like a Neo4j DB folder!: " + local_dir)

    remote_db_dir = "%(data_dir)s/graph.db" % env
    timestamp = get_timestamp()
    import tempfile
    tf = tempfile.NamedTemporaryFile(suffix=".tgz")
    name = tf.name
    tf.close()

    local("tar --create --gzip --file %s -C %s ." % (name, local_dir))
    remote_name = os.path.join("/tmp", os.path.basename(name))
    put(name, remote_name)

    if confirm("Stop Neo4j server?"):
        stop()
        run("mv %s %s.%s" % (remote_db_dir, remote_db_dir, timestamp))
        run("mkdir " + remote_db_dir)
        run("tar zxf %s -C %s" % (remote_name, remote_db_dir))
        run("chown %s.webadm -R %s" % (env.user, remote_db_dir))
        run("chmod -R g+w " + remote_db_dir)
        start()
        
@task
def update_db_test(local_dir):
    """Update a Neo4j DB on the TEST server.    
    Tar the input dir for upload, upload it, stop the server,
    move the current DB out of the way, and unzip it.
    
    update_db_test:/local/path/to/graph.db
    """
    # Check we have a reasonable path...
    if not os.path.exists(os.path.join(local_dir, "index.db")):
        raise Exception("This doesn't look like a Neo4j DB folder!: " + local_dir)

    remote_db_dir = "%(data_dir)s/graph.db" % env
    timestamp = get_timestamp()
    import tempfile
    tf = tempfile.NamedTemporaryFile(suffix=".tgz")
    name = tf.name
    tf.close()

    local("tar --create --gzip --file %s -C %s ." % (name, local_dir))
    remote_name = os.path.join("/tmp", os.path.basename(name))
    put(name, remote_name)

    
    run("mv %s %s.%s" % (remote_db_dir, remote_db_dir, timestamp))
    run("mkdir " + remote_db_dir)
    run("tar zxf %s -C %s" % (remote_name, remote_db_dir))
    run("chown %s.webadm -R %s" % (env.user, remote_db_dir))
    run("chmod -R g+w " + remote_db_dir)
    
@task
def reindex_users():
    "Run a partial reindex of Neo4j -> Solr data"
    all_types = ["UserProfile", "Group"]
    indexer_cmd = [
       "java", "-jar", env.index_helper,
        "--index",
        "-H", "X-User=admin",
        "--stats",
        "--solr", env.solr_url,
        "--rest", env.service_url,
    ] + all_types
    run(" ".join(indexer_cmd))

@task
def reindex_concepts():
    "Run a partial reindex of Neo4j -> Solr data"
    all_types = ["HistoricalAgent", "CvocVocabulary",
                 "CvocConcept", "AuthoritativeSet"]
    indexer_cmd = [
       "java", "-jar", env.index_helper,
        "--index",
        "-H", "X-User=admin",
        "--stats",
        "--solr", env.solr_url,
        "--rest", env.service_url,
    ] + all_types
    run(" ".join(indexer_cmd))

@task
def reindex_virtualcollections():
    "Run a partial reindex of Neo4j -> Solr data"
    all_types = ["VirtualUnit"]
    indexer_cmd = [
       "java", "-jar", env.index_helper,
        "--index",
        "-H", "X-User=admin",
        "--stats",
        "--solr", env.solr_url,
        "--rest", env.service_url,
    ] + all_types
    run(" ".join(indexer_cmd))

@task
def reindex_repository(repo_id):
    """Reindex items held by a repository.
    NB: Bit specific but very useful."""
    indexer_cmd = [
        "java", "-jar", env.index_helper,
        "--clear-key-value", "holderId=" + repo_id,
        "--index",
        "-H", "X-User=admin",
        "--stats",
        "--solr", env.solr_url,
        "--rest", env.service_url,
        "'Repository|%s'" % repo_id,
    ]
    run(" ".join(indexer_cmd))

@task
def reindex_all():
    "Run a full reindex of Neo4j -> Solr data"
    all_types = ["DocumentaryUnit", "Repository", "Country",
            "HistoricalAgent", "CvocVocabulary", "CvocConcept",
            "AuthoritativeSet", "UserProfile", "Group",
            "VirtualUnit", "Annotation", "Link"]
    indexer_cmd = [
        "java", "-jar", env.index_helper,
        "--clear-all",
        "--index",
        "-H", "X-User=admin",
        "--stats",
        "--solr", env.solr_url,
        "--rest", env.service_url,
    ] + all_types
    run(" ".join(indexer_cmd))

@task
def current_version():
    "Show the current date/revision"
    with cd(env.path):
        import re
        path = run("readlink -f current")
        m = re.search("ehri-data-(?P<timestamp>\d{14})_(?P<revision>[a-f0-9]{6})", path)
        timestamp, revision = m.groups()
        date = datetime.strptime(timestamp, TIMESTAMP_FORMAT)
        print("Timestamp: %s, revision: %s" % (date, revision))
        return date, revision

@task
def current_version_log():
    "Output git log between HEAD and the current deployed version."
    _, revision = current_version()
    local("git log %s..HEAD" % revision)

def get_artifact_version():
    """Get the current artifact version from Maven"""
    return local(
        "mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate" +
        " -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)'",
        capture=True).strip()

def get_version_stamp():
    "Get a dated and revision stamped version string"
    rev = subprocess.check_output(["git","rev-parse", "--short", "HEAD"]).strip()
    return "%s_%s" % (get_timestamp(), rev)

def get_timestamp():
    return datetime.now().strftime(TIMESTAMP_FORMAT)    

def copy_to_server():
    "Upload the app to a versioned path."
    # Ensure the deployment directory is there...

    with cd(env.path):
        srcdir = "build/target"
        srcname = "ehri-data-%(artifact)s.jar" % env
        dstname = "ehri-data-%(version)s.jar" % env
        dstdir = "deploys"
        dstfile = os.path.join(dstdir, dstname)

        # make the deploy dir
        run("mkdir -p deploys")
        # upload the uber jar
        print("Running put")
        put(os.path.join(srcdir, srcname), dstfile)

def symlink_current():
    with cd(env.path):
        run("ln --force --no-dereference --symbolic deploys/ehri-data-%(version)s.jar current" % env)

@task
def copy_lib_sh():
    "Put the lib.sh, import-large-batch.sh and cmd scripts on the server."
    with cd(env.path):
        put("scripts/lib.sh", "scripts/lib.sh")
        put("scripts/import-large-batch.sh", "scripts/import-large-batch.sh")
        run("chmod g+x scripts/import-large-batch.sh")
        put("scripts/cmd", "scripts/cmd")
        run("chmod g+x scripts/cmd")
        put("scripts/export-wienerlibrary", "scripts/export-wienerlibrary")
        run("chmod g+x scripts/export-wienerlibrary")
