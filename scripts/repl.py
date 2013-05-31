#!/bin/env python

"""
Interactive shell launcher for EHRI db apps.
Used with Jython 2.7b1 and later.
"""

import os
import sys

NEO4J_HOME = os.environ["NEO4J_HOME"]
NEO4J_DB = os.environ["NEO4J_DB"]

if NEO4J_HOME is None:
    raise Exception("NEO4J_HOME not set!")

if NEO4J_DB is None:
    NEO4J_DB = os.path.join(NEO4J_HOME, "data", "graph.db")

from com.tinkerpop.blueprints.impls.neo4j import Neo4jGraph
from com.tinkerpop.frames import FramedGraph
from eu.ehri.project.core import GraphManagerFactory
from eu.ehri.project.models import *
from eu.ehri.project.models.EntityClass import *

graph = FramedGraph(Neo4jGraph(NEO4J_DB))
manager = GraphManagerFactory.getInstance(graph)

print("Welcome to the EHRI management shell...")
print

