#!/usr/bin/env python3

# AUTHOR:
#
# Dirk Roorda, DANS, NL, dirk.roorda@dans.knaw.nl
#
# USAGE
#
# ./selective-harvest.py --help
#
# or
#
# python3 selective-harvest.py --help
#
# will show complete usage information.
#
# Shortest form:
#
# ./selective-harvest.py
#
# assuming that the config file is in config.xml
#
# Usual form
#
# ./selective-harvest.py -w work-directory -c config-file -l logfile
#
# Verbosity can be increased with -v, -vv, -vvv.
#
# All messages go to log file regardless of verbosity.

import sys
import os
import time
import datetime
import re
import argparse
from subprocess import run
import xml.etree.ElementTree as ET


LOG = None

TIMESTAMP = time.time()

VERBOSE = False

COMMAND = ('curl', '-s', '-o')

metadataPat = re.compile('<metadata[^>]*>(.*)</metadata>', re.S)
errorPat = re.compile('''<error.*code=['"]([^'"]*)['"][^>]*>(.*)</error>''', re.S)


def timestamp():
  now = time.time()
  interval = now - TIMESTAMP
  if interval < 10:
    intervalRep = "{: 2.2f}s".format(interval)
  else:
    interval = int(round(interval))
    if interval < 60:
      intervalRep = "{:>2d}s".format(interval)
    elif interval < 3600:
      intervalRep = "{:>2d}m {:>02d}s".format(interval // 60, interval % 60)
    else:
      intervalRep = "{:>2d}h {:>02d}m {:>02d}s".format(
          interval // 3600, (interval % 3600) // 60, interval % 60
      )
  return '{} = {}'.format(datetime.datetime.now().isoformat(), intervalRep)


def _msg(msg, verbosity, newline, time, log, term):
  """
  Print a message msg if the verbosity admits it.
  optionally with newline, optionally with timestamp,
  optionally to a log file including an optional timestamp,
  Writes to log file regardless of verbosity.
  """
  nl = '\n' if newline else ''
  if term and VERBOSE > verbosity:
    sys.stderr.write('{}{}'.format(msg, nl))
  if log and LOG is not None:
    ts = '{} > '.format(timestamp()) if time else ''
    LOG.write('{}{}{}'.format(ts, msg, nl))


def shout(msg):
  _msg(msg, -100, False, False, False, True)


def shoutln(msg):
  _msg(msg, -100, True, False, False, True)


def error(msg, time=True, log=True, term=True):
  _msg(msg, 0, False, time, log, term)


def errorln(msg, time=True, log=True, term=True):
  _msg(msg, 0, True, time, log, term)


def info(msg, time=True, log=True, term=True):
  _msg(msg, 1, False, time, log, term)


def infoln(msg, time=True, log=True, term=True):
  _msg(msg, 1, True, time, log, term)


def extra(msg, time=True, log=True, term=True):
  _msg(msg, 2, False, time, log, term)


def extraln(msg, time=True, log=True, term=True):
  _msg(msg, 2, True, time, log, term)


def readTasks(configPath, selectRepos):
  """
  Read an XML config file, and convert it to a tasks list.
  Each task specifies a repository with pseudo sets and ids in
  those sets to harvest, plus a location where the harvested
  documents should end up.
  """
  if not os.path.exists(configPath):
    errorln('No config file "{}"'.format(configPath))
    return False
  info('Reading config file "{}" ...'.format(configPath))
  tree = ET.parse(configPath)
  infoln('done', time=False)

  repos = []

  root = tree.getroot()

  for rElem in root.iter('repository'):
    repoName = rElem.attrib['id']
    if selectRepos is not None and repoName not in selectRepos:
      infoln('skipping repo "{}"'.format(repoName))
      continue
    repoInfo = {
        'name': repoName,
        'sets': [],
    }
    for elem in rElem.findall('baseurl'):
      repoInfo['url'] = elem.text
    for elem in rElem.findall('metadataprefix'):
      repoInfo['meta'] = elem.text
    for elem in rElem.findall('recordpath'):
      repoInfo['dest'] = elem.text
    for elem in rElem.findall('output-set'):
      setInfo = {
          'name': elem.attrib['name'],
          'ids': set(),
      }
      for iElem in elem.findall('id'):
        setInfo['ids'].add(iElem.text)
      repoInfo['sets'].append(setInfo)
    repos.append(repoInfo)

  for repo in repos:
    extraln('{} => {}'.format(
        repo.get('name', 'UNKNOWN REPO'),
        repo.get('dest'),
    ))
    for pset in repo.get('sets', set()):
      extraln('\t{}'.format(
          pset.get('name', 'UNKNOWN SET'),
      ))
      for did in sorted(pset.get('ids', set())):
        extraln('\t\t{}'.format(did))
  return repos


def harvestAll(repoTasks):
  """
  Execute all harvesting tasks.
  """
  good = True
  for repoTask in repoTasks:
    thisGood = harvestTask(repoTask)
    if not thisGood:
      good = False
  return good


def harvestTask(repoTask):
  """
  Execute a single harvesting task.
  """
  taskName = repoTask.get('name', 'UNSPECIFIED')
  infoln('Harvesting from "{}"'.format(taskName))
  dest = repoTask.get('dest', '')
  good = True
  if not os.path.exists(dest):
    try:
      os.makedirs(dest, exist_ok=True)
    except Exception:
      errorln('Cannot create directory "{}"'.format(dest))
      good = False
  else:
    if not os.path.isdir(dest):
      errorln('"{}" is not a directory'.format(dest))
      good = False
  if not good:
      return False

  for repoSet in repoTask.get('sets', []):
    setGood = True
    setName = repoSet.get('name', 'UNSPECIFIED')
    ids = sorted(repoSet.get('ids', []))
    infoln('\tHarvesting "{}" set "{}" with {} documents'.format(
        taskName, setName, len(ids),
    ))
    setDest = '{}/{}'.format(dest, setName)
    if not os.path.exists(setDest):
      try:
        os.makedirs(setDest, exist_ok=True)
      except Exception:
        errorln('Cannot create directory "{}"'.format(setDest))
        setGood = False
    else:
      if not os.path.isdir(setDest):
        errorln('"{}" is not a directory'.format(setDest))
        setGood = False
    if not setGood:
      good = False
      continue
    nError = 0
    for docId in ids:
      docError = None
      repoUrl = repoTask.get('url', '')
      meta = repoTask.get('meta', '')
      msg = '    harvesting "{}" ... '.format(docId)
      info(msg)
      docUrl = '{}?verb=GetRecord&identifier={}&metadataPrefix={}'.format(
          repoUrl,
          docId,
          meta,
      )
      docDest = '{}/{}.xml'.format(setDest, docId.replace(':', '-'))
      try:
        run(
            COMMAND + (docDest, docUrl)
        )
        error = deliver(docDest)
        if error is not None:
          docError = error
      except Exception as e:
        docError = e
      if docError and os.path.exists(docDest):
        os.unlink(docDest)
      if docError:
        setGood = False
        nError += 1
      if docError:
        if VERBOSE <= 1:
          errorln('{}XX'.format(msg), log=False)
          errorln('XX', time=False, term=False)
        else:
          errorln('XX', time=False)
      else:
        infoln('OK', time=False)
      if docError:
        docError = str(docError).rstrip('\n')
        infoln('\t\t\t{}'.format(docError))
    if not setGood:
      good = False
    infoln('\tHarvested "{}" set "{}" {} good, {} missed'.format(
        taskName, setName, len(ids) - nError, nError,
    ))
  return good


def deliver(path):
  """
  harvestTask writes the result of a harvest request to disk as is.
  This function peels the OAI-PMH wrapper off the document, and saves
  the document in the same place.
  """
  with open(path) as fh:
    text = fh.read()
  error = None
  if '</GetRecord>' in text and '</metadata>' in text:
      match = metadataPat.search(text)
      if match:
          text = match.group(1).strip()
          with open(path, 'w') as fh:
            fh.write(text)
      else:
          error = 'No metadata found'
  elif '</error>' in text:
      match = errorPat.search(text)
      if match:
          code = match.group(1)
          msg = match.group(2)
          error = '{code}: {msg}'.format(code=code, msg=msg)
      else:
          error = 'Could not parse error message'
  else:
      error = 'No record found and no error message found'
  return error


def main():
  """
  Sets up an argument parser.
  Changes to the working directory which is specified in the -w argument.
  Starts appending to the log file.
  Reads the config file which is specified in the -c argument.
  Harvests all repos and document-ids found in the config file,
  unless -r repo is given, in which case only document-ids from
  repo are harvested.
  The harvested documents are stored in a location specified
  per repo in the config file.

  Example config file:

    <config>
        <repository id="dans-easy">
            <baseurl>https://easy.dans.knaw.nl/oai/</baseurl>
            <metadataprefix>oai_dc</metadataprefix>
            <output-set name="theo1">
                <id>oai:easy.dans.knaw.nl:easy-dataset:4215</id>
                <id>oai:easy.dans.knaw.nl:easy-dataset:25037</id>
                <id>oai:easy.dans.knaw.nl:easy-dataset:25037x</id>
            </output-set>
            <output-set name="theo2">
                <id>oai:easy.dans.knaw.nl:easy-dataset:30678</id>
                <id>oai:easy.dans.knaw.nl:easy-dataset:32044</id>
            </output-set>
            <from>2006-11-01T00:00:00Z</from>
            <recordpath>_temp/oai-pmh-harvester/dans</recordpath>
        </repository>
    </config>

  Run ./selective-harvest.py --help to see complete usage information.
  """

  global VERBOSE
  global LOG

  parser = argparse.ArgumentParser(description='selective harvest arguments')
  parser.add_argument(
      '-c', '--config',
      default='config.xml',
      help='path to config file (xml)',
  )
  parser.add_argument(
      '-l', '--log',
      default='log.txt',
      help='path to log file',
  )
  parser.add_argument(
      '-w', '--workdir',
      default='',
      help=(
          'path to working directory which is'
          ' the starting point of a relative config path'
          ' and the starting point of a relative output path'
      ),
  )
  parser.add_argument(
      '-r', '--repo',
      default='',
      type=str,
      help='only do repos in comma separated list of repo ids',
  )
  parser.add_argument(
      '-v', '--verbose',
      action='count',
      default=0,
      help=(
          'print errors, messages, verbose messages.'
          ' Repeat the option to increase verbosity.'
      ),
  )
  args = parser.parse_args()
  VERBOSE = args.verbose

  workDir = os.path.abspath(args.workdir)
  os.chdir(workDir)

  logPath = os.path.abspath(args.log)
  logDir = os.path.dirname(logPath)
  if not os.path.exists(logDir):
    try:
      os.makedirs(logDir, exist_ok=True)
    except Exception:
      shoutln('Cannot create log directory "{}"'.format(logDir))
  try:
    LOG = open(logPath, 'a')
  except Exception:
    LOG = None
    shoutln('Cannot write to log file "{}"'.format(logPath))

  infoln('working in directory "{}"'.format(os.getcwd()))

  repos = None if args.repo == '' else set(args.repo.split(','))
  if repos is None:
    infoln('Harvest all repos found in "{}"'.format(args.config))
  else:
    infoln('Harvest repositories "{}" only'.format('", "'.join(repos)))
  repoTasks = readTasks(args.config, repos)
  result = 0
  if not repoTasks:
    result = 1
  if not harvestAll(repoTasks):
    result = 1
  if result != 0:
    errorln('ERROR {}'.format(result))
  if LOG:
    LOG.close()
  return result


returnCode = main()
sys.exit(returnCode)
