"""
Fabric deployment script for EHRI front-end webapp.
"""

from __future__ import with_statement

import os
import datetime
import subprocess
from fabric.api import *
from fabric.contrib.console import confirm
from contextlib import contextmanager as _contextmanager

# globals
env.project_name = 'ehri-rest'
env.service_name = 'neo4j-service'
env.prod = False
env.path = '/opt/webapps/' + env.project_name
env.user = os.getenv("USER")
env.use_ssh_config = True

# environments
def test():
    "Use the remote testing server"
    env.hosts = ['ehritest']

def stage():
    "Use the remote staging server"
    env.hosts = ['ehristage']

def prod():
    "Use the remote virtual server"
    env.hosts = ['ehriprod']
    env.prod = True

def deploy():
    """
    Deploy the latest version of the site to the servers, install any
    required third party modules, and then restart the webserver
    """
    with settings(version = get_version_stamp()):
        copy_to_server()
        symlink_current()
        restart()

def clean_deploy():
    """Build a clean version and deploy."""
    local('mvn clean package -P sparql  -DskipTests')
    deploy()

def get_version_stamp():
    "Get a dated and revision stamped version string"
    rev = subprocess.check_output(["git","rev-parse", "--short", "HEAD"]).strip()
    timestamp = datetime.datetime.now().strftime("%Y%m%d%H%M%S")    
    return "%s_%s" % (timestamp, rev)

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

def start():
    "Start Neo4j"
    # NB: This doesn't use sudo() directly because it insists on asking
    # for a password, even though we should have NOPASSWD in visudo.
    run('sudo service %(service_name)s start' % env, pty=False, shell=False)

def stop():
    "Stop docview"
    # NB: This doesn't use sudo() directly because it insists on asking
    # for a password, even though we should have NOPASSWD in visudo.
    run('sudo service %(service_name)s stop' % env, pty=False, shell=False)

def restart():
    "Restart docview"
    # NB: This doesn't use sudo() directly because it insists on asking
    # for a password, even though we should have NOPASSWD in visudo.
    if confirm("Restart Neo4j server?"):
        run('sudo service %(service_name)s restart' % env, pty=False, shell=False)

def rollback():
    "Rollback to the last versioned dir and restart"
    with cd(env.path):
        output = run("ls -1rt deploys | tail -n 2 | head -n 1").strip()
        if output == "":
            raise Exception("Unable to get previous version for rollback!")
        with settings(version=output):
            symlink_current()
            print("Current version is now: %s" % output)
            restart()

def latest():
    "Point symlink at latest version"
    with cd(env.path):
        output = run("ls -1rt deploys | tail -n 1").strip()
        if output == "":
            raise Exception("Unable to get latest version for rollback!")
        with settings(version=output):
            symlink_current()
            print("Current version is now: %s" % output)
            restart()

def copy_db():
    "Copy a Neo4j DB from a server"
    raise NotImplementedError

def update_db():
    "Update a Neo4j DB on a server"
    raise NotImplementedError


