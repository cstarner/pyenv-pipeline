# Pyenv Pipeline Plugin
A Jenkins plugin that provides a way to execute <code>sh</code> and 
<code>bat</code> Pipeline DSL commands within a specified Python
virtualenv.

## Overview
This plugin provides 1 new Pipeline DSL method:

* <code>withPythonEnv</code>: Specifies a Python virtualenv to execute
  any <code>sh</code> and <code>bat</code> DSL commands contained 
  within its block.
  
  <code>withPythonEnv</code> takes a single String argument, which
  specifies the Python executable to use for the virtualenv.
  pyenv-pipeline will use the executable to generate a corresponding
  virtualenv. At runtime, it will take a snapshot of environmental 
  variables with and without the virtualenv active. From this it generates
  a diff, and applies the environmental variable changes within the
  <code>withPythonEnv</code> block (reverting them after the block completes)
  
  The argument provided to <code>withPythonEnv</code> will first attempt
  to match it against the name of a <code>ToolInstallation</code> that
  is described by a <code>ToolDescriptor</code> with an ID that is contained
  within a pre-defined list of known Jenkins Python Tool plugin IDs. Currently,
  this plugin only looks to see if [ShiningPanda](https://github.com/jenkinsci/shiningpanda-plugin) is installed. If a 
  <code>ToolInstallation</code> is matched, the location of that tool is used
  as the Python executable to generate the virtualenv.
  
  If no <code>ToolInstallation</code> is matched, we attempt to treat the argument as 
  the location of an already existing virtualenv. A directory lookup is attempted with the
  string argument, and if the argument is determined to point to a virtualenv, we go
  ahead and use that. 
  
  * In order for this feature to work, the passed argument must end with the OS appropriate
  file separator.
  
  Lastly, the argument is treated as the literal location of the Python executable to be used. This can be used
  to specify a specific Python installation (if the location is known beforehand),
  or to fallback and use the systems default Python installation.
  
  * <pre><code>withPythonEnv('python') {
        // Uses the default system installation of Python
        // Equivalent to withPythonEnv('/usr/bin/python') 
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
    
  * <pre><code>withPythonEnv('/home/user/managed_virtualenv/'){
        // Uses the virtualenv that already exists at /home/user/managed_virtualenv/
        ...
    }</code></pre>


## Warnings:

  * Earlier version of this plugin relied on using <code>pysh</code> and
  <code>pybat</code> steps to execute code within <code>withPythonEnv</code>
  blocks. These steps are no longer necessary. To migrate, simply remove the <code>py</code>
  prefix from any such steps, and the command should work as intended. The steps are
  still included, and are copies of the <code>sh</code> and <code>bat</code> steps. Eventually,
  the steps will be removed altogether.


  * Multibranch pipeline builds will occasionally generate very long path names
  triggering pypa/virtualenv#596. In these instances, use of this plugin is not
  an option, at least at this time. 
  
