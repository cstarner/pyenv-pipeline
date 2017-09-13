# Pyenv Pipeline Plugin
A Jenkins plugin that provides a way to execute <code>sh</code> and 
<code>bat</code> Pipeline DSL commands within a specified Python
virtualenv.

## Overview
This plugin provides 3 new Pipeline DSL methods:

* <code>withPythonEnv</code>: Specifies a Python virtualenv to execute
  any <code>pysh</code> and <code>pybat</code> DSL commands contained 
  within its block.
  
  <code>withPythonEnv</code> takes a single String argument, which
  specifies the Python executable to use for the virtualenv.
  pyenv-pipeline will use the executable to generate a corresponding
  virtualenv, and store it's location in the 
  <code>PYENVPIPELINE_VIRTUALENV_RELATIVE_DIRECTORY</code> environmental
  variable.
  
  The argument provided to <code>withPythonEnv</code> will first attempt
  to match it against the name of a <code>ToolInstallation</code> that
  is described by a <code>ToolDescriptor</code> with an ID that is contained
  within a pre-defined list of known Jenkins Python Tool plugin IDs. Currently,
  this plugin only looks to see if ShiningPanda is installed. If a 
  <code>ToolInstallation</code> is matched, the location of that tool is used
  as the Python executable to generate the virtualenv.
  
  If no <code>ToolInstallation</code> is matched, then the argument is treated
  as the literal location of the Python executable to be used. This can be used
  to specify a specific Python installation (if the location is known beforehand),
  or to fallback and use the systems default Python installation.
  
  * <pre><code>withPythonEnv('python') {
        // Uses the default system installation of Python
        // Equivalent to withPythonEnv('/usr/bin/python') 
        // or withPythonEnv('/usr/bin') on Ubuntu
        ...
    }
    </code></pre>
  * <pre><code>withPythonEnv('/usr/bin/python3.5') {
        // Uses the specific python3.5 executable located in /usr/bin
        ...
    }</code></pre>
  * <pre><code>withPythonEnv('CPython-2.7'){
        // Uses the ShiningPanda registered Python installation named 'CPython-2.7'
        ...
    }</code></pre>  
* <code>pysh</code>: Functions as the provided <code>sh</code> Pipeline DSL
  command, expect that if it finds the <code>PYENVPIPELINE_VIRTUALENV_RELATIVE_DIRECTORY</code
  environmental variable, it activates the virtualenv located there prior
  to running the provided script.
* <code>pybat</code>: Works just like <code>pysh</code>, expect for Windows
  build environments