"""
Fabric deployment script for EHRI backend webapp.
"""

import os
import sys
from datetime import datetime

from fabric import task
from invoke import run as local
from patchwork import files


deploys_dir = "/opt/neo4j/ehri-rest/deploys"
target_link = "/opt/neo4j/plugins/ehri-data.jar"
neo4j_install = "/opt/neo4j/current"

@task
def deploy(ctx, clean=False):
    """Build (optionally with clean) and deploy the distribution"""
    local("mvn package -DskipTests" if not clean else "mvn clean package -DskipTests")

    artifact_version = get_artifact_version(ctx)
    version = get_version_stamp(ctx)

    src = f"build/target/ehri-data-{artifact_version}.jar"
    dst = f"{deploys_dir}/ehri-data-{version}.jar"

    ctx.put(src, remote=dst)
    symlink_target(ctx, dst, target_link)
    restart(ctx)


@task
def rollback(ctx):
    """Set the current version to the previous version directory"""
    output = ctx.run(f"ls -1rt {deploys_dir} | tail -n 2 | head -n 1").stdout.strip()
    if output == "":
        raise Exception("Unable to get previous version for rollback!")
    symlink_target(ctx, f"{deploys_dir}/{output}", target_link)
    restart(ctx)


@task
def latest(ctx):
    """Set the current version to the latest version directory"""
    output = ctx.run(f"ls -1rt {deploys_dir} | tail -n 1").stdout.strip()
    if output == "":
        raise Exception("Unable to get previous version for rollback!")
    symlink_target(ctx, f"{deploys_dir}/{output}", target_link)
    restart(ctx)


@task
def get_artifact_version(ctx):
    """Get the current artifact version from Maven"""
    return local(
        "mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate" +
        " -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)'").stdout.strip()


@task
def symlink_target(ctx, source, target):
    """Symlink a version directory"""
    ctx.run(f"ln --force --no-dereference --symbolic {source} {target}")
    ctx.run(f"chgrp -R webadm {target_link}")


@task
def restart(ctx):
    """Restart the Neo4j process"""
    if input("Restart Neo4j (y/n)\n").lower() == "y":
        ctx.run("sudo service neo4j restart")


@task
def get_version_stamp(ctx):
    """Get the tag for a version, consisting of the current time and git revision"""
    res = local("git rev-parse --short HEAD").stdout.strip()
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    return f"{timestamp}_{res}"


@task
def online_backup(ctx, dir, tar = True):
    """Create an online backup to directory `dir`"""
    if files.exists(ctx, dir):
        print(f"Remote directory '{dir}' already exists!")
        sys.exit(1)
    tar_name = dir + ".tgz"
    dirname = os.path.dirname(dir)
    basename = os.path.basename(dir)
    ctx.run(f"{neo4j_install}/bin/neo4j-admin backup --from localhost:6362 --name={basename} --backup-dir {dirname}")
    if tar:
        ctx.run(f"tar --create --gzip --file {tar_name} -C {dir} .")
        ctx.run(f"rm -rf {dir}")


@task
def online_clone_db(ctx, dir):
    """Create an online backup of the database and download to local dir `dir`"""
    timestamp = get_timestamp()
    tmpdst = "/tmp/" + timestamp
    online_backup(ctx, tmpdst, tar = False)
    ctx.run(f"tar --create --gzip --file {tmpdst}.tgz -C {tmpdst} .")
    ctx.get(f"{tmpdst}.tgz", f"{tmpdst}.tgz")
    ctx.run(f"rm -rf {tmpdst} {tmpdst}.tgz")
    local(f"mkdir -p {dir}")
    local(f"tar xf {tmpdst}.tgz -C {dir}")
    local(f"rm {tmpdst}.tgz")


def get_timestamp():
    return datetime.now().strftime("%Y%m%d%H%M%S")