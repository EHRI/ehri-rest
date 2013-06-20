# Scripting the EHRI environment

## Introduction

While in the process of stabilising the server environment, it is useful to have some scripting
tools at our disposal which are able to interact with the various Java tools we have,
without requiring the writing and deployment of more Java code. This is particularly the case where one-off
migrations of the graph data is required as its ontology evolves, or importing of WIP data acquisitions.

Another important consideration of a scripting environment was access to a REPL, which would allow us to investigate
issues with the graph data interactively via a command-line prompt.

A few different environments have so far been investigated:

* Jython
* Groovy
* JRuby

While these investigations have not been exhaustive, a few impressions resulted:

### Jython

The Java version of Python (2.7b1) works fine, but does not seem to have a very active development community,
and it was difficult to find up-to-date information. One particular advantage of Python would potentially be access
to IPython, which is a really good REPL with auto-complete and many other nice features,
but as of this writing it does not seem to work with the Jython environment. Jython development also seems to lag the
 standard C-Python development by quite a lot.

### Groovy

Groovy is a language with very tight Java integration, but setting up a workable scripting environment that
integrated the EHRI tools proved difficult. While some tools do use Groovy as a REPL environment,
there seem to be quite a lot of hoops to jump through before this works smoothly. More work may follow on this...

### JRuby

JRuby proved easy to set up, up-to-date, and includes a workable (if not IPython-level) REPL environment (called
`jirb`). One particular bonus of JRuby is that its creators have gone to great lengths to make Java integration as
tight as possible, and moreover, make it easy to write idiomatic Ruby code that works with Java tools. What follows
is mainly an overview of writing JRuby scripts to interact with the EHRI environment.

## Setting up JRuby

As of this writing the current version of JRuby is 1.7.4. The distribution provides an installer and should work on
Linux, Mac OS X, and Windows.

Once JRuby is installed it's useful to add it's `bin` directory to your `$PATH` environment var. Once done,
you should be able to launch scripts with `jruby script.rb` and start the interactive prompt with `jirb`.

## JRuby basics

For the basics of how JRuby integrates with Java, the following document is essential reading:

https://github.com/jruby/jruby/wiki/CallingJavaFromJRuby

The TL;DR is basically:

```ruby
# The magic require Java line
require 'java'

# You can then refer to Java stuff as normal:
puts java.util.Locale.getDefault.getLanguage # prints "en" on my system

# Note: Jruby makes things look more idiomatic by converting automatically
# between CamelCase and the more Ruby-ish under_score naming, so this also
# works
puts java.util.Locale.get_default.get_language # also prints "en"!
```

To import stuff in a manner similar to how you would in Java, you can use the `java_import` statement:

```ruby
java_import "eu.ehri.project.models.UserProfile"
```

(The quotes are necessary if the root package is outside of the Java environemt.)

## Integration with the EHRI environment

#### Prerequisites

As well as having JRuby installed, for the EHRI/Ruby environment to work you need a couple of env vars set:

* `$NEO4J_HOME`
* `$CLASSPATH`

As usual, `$NEO4J_HOME` should point to a version of Neo4j with the EHRI plugin installed. The `$CLASSPATH` var can
be exported like so:

```bash
source ./scripts/lib.sh      # Source the EHRI bash tools
buildclasspath               # This function builds a Java-ish string of dependencies from the $NEO4J_HOME
export CLASSPATH=$CLASSPATH  # Exports the CLASSPATH var created by buildclasspath
```

#### Using the 'Ehri' module

Much of the heavy lifting of setting up the EHRI environment has been placed in a module called `Ehri` inside the
`scripts/ruby/lib` directory of the EHRI Neo4j server code root. You can pull this into your Ruby script environment
like so:

```ruby
require "scripts/lib/ehri"
include Ehri
```

The `Ehri` module does the following when `required`:

* Checks the `$NEO4J_HOME` and `$CLASSPATH` env vars look okay
* `java_import`s some key useful stuff, like the Tinkerpop Neo4jGraph and FramedGraph
* Creates some Ruby modules as shortcuts through which EHRI packages can be accessed
* Initialises the graph database and an EHRI GraphManager as the constants `Graph` and `Manager` (subject to change)

The shortcut modules within `Ehri` are:

* `Core` - eu.ehri.project.core
* `Models` - eu.ehri.project.models
* `Persistance` - eu.ehri.project.persistance
* `Importers` - eu.ehri.project.importers
* `Acl` - eu.ehri.project.acl

Putting all this together, you can then do stuff like this from the REPL:

```bash
03:24 PM > jirb
irb(main):001:0> require "scripts/ruby/lib/ehri"
=> true
irb(main):002:0> include Ehri
=> Object
irb(main):003:0> mike = Manager.get_frame("mike", Models::UserProfile.java_class)
=> #<Java::Default::$Proxy30:0x5820e5f8>
irb(main):004:0> mike.name
=> "Mike Bryant"
irb(main):005:0>
```

For example, a script to list the names of all repositories in the United Kingdom:

```ruby
require "scripts/ruby/lib/ehri"

include Ehri

gb = Manager.get_frame("gb", Models::Country.java_class)
gb.get_repositories.each { |repository|
  puts " - #{repository.get_descriptions.first.name}"
}

# This prints:
# - Department of Documents, Imperial War Museum
# - Archiwum Studium Polski Podziemnej
# - MOVIETONE
# - Manchester Jewish Museum
# - Jewish Museum London
# - British Library
# - University of Southampton
# - Oxford Centre for Hebrew and Jewish Studies
# - The National Archives
# - London Metropolitan Archives
# - Imperial War Museums
# - Island Archives
# - British PathéNews
# - Polish Institute and Sikorski Museum
# - Central British Fund
# - Jersey Archive
# - Department for Business Innovation and Skills, "Enemy Property"
# - Association of Jewish Refugees, Serving Holocaust Refugees and Survivors Nationwide
# - The Wiener Library for the Study of the Holocaust&Genocide

```

Lots of other useful things are possible. For more examples, look at some of the import tools in `scripts/ruby/lib`.
These are themselves modules that extent the `Ehri` module, for example:

```ruby
# Require the Ehri module, which we know is alongside our current one in the lib dir
require "#{File.dirname(__FILE__)}/ehri"

module Ehri
  module MyImporter

    class Importer
      def initialize(directory)
        @dir = directory
      end

      def import
        # do lots of stuff with `Graph` and `Manager` etc
      end
    end

    # Entry point function
    def self.import(directory)
      Importer.new(directory).import
    end
  end
end
```

From another script, this can then be launched like so:

```ruby
require "scripts/ruby/lib/myimporter"

DIRECTORY = ARGV.shift # get the dir from a argument
Ehri::MyImporter::import(DIRECTORY)
```

In the EHRI Neo4j server dev system we have some existing management commands, for doing things like initialising the
 graph and importing files. There can be run directly (although perhaps not in the nicest manner) like so:

```ruby
# Runs the 'Initialize' command, with an empty set of arguments
# as if they were provided via the command-line...
#
# Note the the [].to_java(:string) bit is how a Java String[] is
# constructed in JRuby...

Commands::Initialize.new.exec(Graph, [].to_java(:string))
```

Note that the `Graph` constant (which is a `FramedGraph<Neo4jGraph>` initialised when `require`ing the `Ehri` module)
is the first argument.

Another, more complex example, is running the LoadFixtures command, with an argument that gives the YAML fixture file
 to load:

```ruby
Commands::LoadFixtures.new.exec(Graph, ["#{ENV["HOME"]}/Dropbox/EHRI/users.yaml"].to_java(:string))
```

More documentation to follow...