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
env.neo4j_install = '/opt/webapps/' + 'neo4j-version'
env.index_helper = "/opt/webapps/docview/bin/indexer.jar"
env.user = os.getenv("USER")
env.use_ssh_config = True

TIMESTAMP_FORMAT = "%Y%m%d%H%M%S"

# environments
@task
def test():
    "Use the remote testing server."
    env.hosts = ['ehritest']

@task
def stage():
    "Use the remote staging server."
    env.hosts = ['ehristage']

@task
def prod():
    "Use the remote virtual server."
    env.hosts = ['ehriprod']
    env.prod = True

@task
def deploy():
    """
    Deploy the latest version of the site to the servers, install any
    required third party modules, and then restart the webserver
    """
    with settings(version = get_version_stamp()):
        copy_to_server()
        symlink_current()
        restart()

@task
def clean_deploy():
    """Build a clean version and deploy."""
    local('mvn clean package -P sparql  -DskipTests')
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
        output = run("ls -1rt deploys | tail -n 2 | head -n 1").strip()
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
        output = run("ls -1rt deploys | tail -n 1").strip()
        if output == "":
            raise Exception("Unable to get latest version for rollback!")
        with settings(version=output):
            symlink_current()
            print("Current version is now: %s" % output)
            restart()

@task
def online_backup(remote_dir):
    """
    Do an online backup to a particular directory on the server.

    online_backup:/path/on/server/backup.graph.db
    """
    if files.exists(remote_dir):
        abort("Remote directory '%s' already exists!" % remote_dir)
    with settings(dst=remote_dir):
        run("%(neo4j_install)s/bin/neo4j-backup -from single://localhost:6362 -to %(dst)s" % env)

@task
def load_queue():
    """Run the queue.sh script to run imports from the queue.
    
    """
    with cd(env.path):
        stop()
        run("./scripts/queue.sh")
        start()

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
        online_backup(env.tmpdst)
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

        remote_db_dir = "%(neo4j_install)s/data/graph.db" % env
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
    

    remote_db_dir = "%(neo4j_install)s/data/graph.db" % env
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

    remote_db_dir = "%(neo4j_install)s/data/graph.db" % env
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

    remote_db_dir = "%(neo4j_install)s/data/graph.db" % env
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
def reindex_repository(repo_id):
    """Reindex items held by a repository.
    NB: Bit specific but very useful."""
    indexer_cmd = [
        "java", "-jar", env.index_helper,
        "--clear-key-value", "holderId=" + repo_id,
        "--index",
        "-H", "Authorization=admin",
        "--stats",
        "--solr", "http://localhost:8080/ehri/portal",
        "--rest", "http://localhost:7474/ehri",
        "'repository|%s'" % repo_id,
    ]
    run(" ".join(indexer_cmd))

@task
def reindex_all():
    "Run a full reindex of Neo4j -> Solr data"
    all_types = ["documentaryUnit", "repository", "country",
            "historicalAgent", "cvocVocabulary", "cvocConcept",
            "authoritativeSet", "userProfile", "group"]
    indexer_cmd = [
        "java", "-jar", env.index_helper,
        "--clear-all",
        "--index",
        "-H", "Authorization=admin",
        "--stats",
        "--solr", "http://localhost:8080/ehri/portal",
        "--rest", "http://localhost:7474/ehri",
    ] + all_types
    run(" ".join(indexer_cmd))

@task
def current_version():
    "Show the current date/revision"
    with cd(env.path):
        path = run("readlink -f current")
        deploy = os.path.split(path)
        timestamp, revision = os.path.basename(deploy[-1]).split("_")
        date = datetime.strptime(timestamp, TIMESTAMP_FORMAT)
        print("Timestamp: %s, revision: %s" % (date, revision))
        return date, revision

@task
def current_version_log():
    "Output git log between HEAD and the current deployed version."
    _, revision = current_version()
    local("git log %s..HEAD" % revision)

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
        srcdir = "assembly/target"
        srcname = "assembly-0.1.tar.gz" # FIXME: Get this programatically...
        dstpath = "deploys/%(version)s" % env
        dstfile = os.path.join(dstpath, srcname)

        # make the deploy dir
        run("mkdir -p deploys/%(version)s" % env)
        # upload the assembly gzip
        print("Running put")
        put(os.path.join(srcdir, srcname), dstfile)
        # extract it
        with cd(dstpath):
            run("tar --extract --gzip --file %s" % srcname)
        # delete the zip
        run("rm %s" % dstfile)

def symlink_current():
    with cd(env.path):
        run("ln --force --no-dereference --symbolic deploys/%(version)s current" % env)

@task
def copy_lib_sh():
    "Put the lib.sh, import-ushmm.sh and cmd scripts on the server."
    with cd(env.path):
        put("scripts/lib.sh", "scripts/lib.sh")
        put("scripts/import-ushmm.sh", "scripts/import-ushmm.sh")
        run("chmod g+x scripts/import-ushmm.sh") 
        put("scripts/cmd", "scripts/cmd")
        run("chmod g+x scripts/cmd")  
        put("scripts/export-wienerlibrary", "scripts/export-wienerlibrary")
        run("chmod g+x scripts/export-wienerlibrary") 
