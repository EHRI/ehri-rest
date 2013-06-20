
module EhriUtils

  # Make check_env callable without including the module
  # This is necessary when you want to do something that
  # requires the envs before the graph is initialized.
  def self.check_env
    # Abort if we don't have NEO4J_HOME
    if ENV['NEO4J_HOME'].nil? or ENV['NEO4J_HOME'].empty? then
      abort "Error: NEO4J_HOME environment variable must be defined."
    end

    # Abort if we don't have NEO4J_HOME
    if ENV['CLASSPATH'].nil? or ENV['CLASSPATH'].empty? then
      abort "Error: CLASSPATH environment variable must be defined."
    end

    # set the NEO4J_DB path if not already set
    ENV['NEO4J_DB'] ||= "#{ENV['NEO4J_HOME']}/data/graph.db" 
  end
end

