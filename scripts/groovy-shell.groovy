import org.codehaus.groovy.tools.shell.Groovysh

// Execute code at startup - got to be a better way of doing this!
//

// Get directory of current script...
def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
def init = new File(scriptDir, "init.groovy").text
Groovysh console = new Groovysh()
for (String cmd : init.split("\n")) {
  // This is MASSIVELY DUMB but I can't find any other
  // way to initialise this such as the imports remain
  // in scope when the shell starts.
  console.execute(cmd)
}
console.run()
