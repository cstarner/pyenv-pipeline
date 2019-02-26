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
import com.github.pyenvpipeline.jenkins.VirtualenvManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class WithPythonEnvTest {

    @Test
    public void testGetBaseDirectory() {
        VirtualenvManager virtualenvManager = VirtualenvManager.getInstance();
        /*Assert.assertEquals(".pyenv-Foo-Bar-python3", virtualenvManager.getRelativePythonEnvDirectory("C:\\Foo\\Bar\\python3"));
        Assert.assertEquals(".pyenv-foo-bar-blah-python", virtualenvManager.getRelativePythonEnvDirectory("/foo/bar/blah/python"));
        Assert.assertEquals(".pyenv-foo-bar-blah-python", virtualenvManager.getRelativePythonEnvDirectory("foo/bar/blah/python"));
        Assert.assertEquals(".pyenv-python3", virtualenvManager.getRelativePythonEnvDirectory("python3"));
        Assert.assertEquals(".pyenv-Foo-Bar-python3", virtualenvManager.getRelativePythonEnvDirectory("c:\\Foo\\Bar\\python3"));
        Assert.assertEquals(".pyenv-Foo-Bar-python3", virtualenvManager.getRelativePythonEnvDirectory("D:\\Foo\\Bar\\python3"));*/
    }
}
