/*
 * The MIT License
 *
 * Copyright 2017 Colin Starner.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.github.pyenvpipeline.jenkins.steps;

import org.junit.Assert;
import org.junit.Test;

public class PyEnvShellStepTest {

    @Test
    public void testGetArgumentList() throws Exception {
        PyEnvShellStep shellStep = new PyEnvShellStep("python --version");
        String commandString = shellStep.getFullScript("/home/username/.pyenv-python2.7/");
        Assert.assertEquals(". /home/username/.pyenv-python2.7/bin/activate; python --version", commandString);

        // Test that quotes in the string are handled correctly
        PyEnvShellStep shellStepWithQuotes = new PyEnvShellStep("python -c \"import sys; import platform; " +
                "sys.stdout.write(platform.python_version())\"");
        commandString = shellStepWithQuotes.getFullScript("/home/username/.pyenv-python2.7/");
        Assert.assertEquals(". /home/username/.pyenv-python2.7/bin/activate; python -c " +
                "\"import sys; import platform; sys.stdout.write(platform.python_version())\"", commandString);

    }

    @Test
    public void testGetArgumentListSpaces() {
        PyEnvShellStep shellStep = new PyEnvShellStep("python --version");
        String commandString = shellStep.getFullScript("/home/test with spaces/.pyenv-python2.7/");
        Assert.assertEquals(". \"/home/test with spaces/.pyenv-python2.7/bin/activate\"; python --version", commandString);
    }
}
