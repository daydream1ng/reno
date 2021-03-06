/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
/*
 * Copyright  2003-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package net.sourceforge.cruisecontrol.launch;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Arrays;

import net.sourceforge.cruisecontrol.launch.util.Locator;

/**
 * Provides the means to launch CruiseControl with the appropriate classpath.
 * This code is based heavily on (some parts taken directly from) the Apache
 * Ant project.
 *
 * @author <a href="mailto:rjmpsmith@gmail.com>Robert J. Smith</a>
 */
public class Launcher {

    /** The property containing the CruiseControl home directory */
    public static final String CCHOME_PROPERTY = "cc.home";

    /** The property containing the CruiseControl dist directory */
    public static final String CCDISTDIR_PROPERTY = "cc.dist.dir";

    /** The property containing the CruiseControl library directory */
    public static final String CCLIBDIR_PROPERTY = "cc.library.dir";

    /** The directory name of the per-user CC directory */
    public static final String CC_PRIVATEDIR = ".cruisecontrol";

    /** The location of a per-user library directory */
    public static final String CC_PRIVATELIB = "lib";

    /** The location of a per-user library directory */
    public static final String USER_LIBDIR = CC_PRIVATEDIR + File.separator
            + CC_PRIVATELIB;

    /** system property with user home directory */
    public static final String USER_HOMEDIR = "user.home";

    /** The startup class that is to be run */
    public static final String MAIN_CLASS = "net.sourceforge.cruisecontrol.Main";

    /** CC Command line arguement name. */
    public static final String ARG_LOG4J_CONFIG = "log4jconfig";
    static final String ERR_MSG_LOG4J_CONFIG = "The -" + ARG_LOG4J_CONFIG + " argument must "
                        + "be followed by a log4j configuration URL (for example: \"file:/c:/mylog4j.xml\")";
    /** Log4j system property name. */
    public static final String PROP_LOG4J_CONFIGURATION = "log4j.configuration";

    private static final URL[] EMPTY_URL_ARRAY = new URL[0];

    /**
     *  Entry point for starting CruiseControl from the command line
     *
     * @param  args commandline arguments
     */
    public static void main(String[] args) {
        try {
            Launcher launcher = new Launcher();
            launcher.run(args);
        } catch (LaunchException e) {
            System.err.println(e.getMessage());
       } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Run the launcher
     *
     * @param args the command line arguments
     *
     * @throws LaunchException if CruiseControl home is not set or could not be located, or if other
     *            invalid argument values are given.
     * @throws MalformedURLException if the URLs required for the classloader
     *            cannot be created.
     */
    void run(final String[] args) throws LaunchException, MalformedURLException {

        final File sourceJar = Locator.getClassSource(this.getClass());
        final File distJarDir = sourceJar.getParentFile();

        final File ccHome = getCCHomeDir(distJarDir);

        // Process the command line arguments. We will handle the classpath
        // related switches ourself. All other arguments will be repackaged
        // and passed on the the Main class for processing.
        final List<String> libPaths = new ArrayList<String>();
        final List<String> argList = new ArrayList<String>();
        boolean noUserLib = false;

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-lib")) {
                if (i == args.length - 1) {
                    throw new LaunchException("The -lib argument must "
                        + "be followed by a library location");
                }
                libPaths.add(args[++i]);
            } else if (args[i].equals("--nouserlib") || args[i].equals("-nouserlib")) {
                noUserLib = true;
            } else if (args[i].equals("-" + ARG_LOG4J_CONFIG)) {
                if (i == args.length - 1) {
                    throw new LaunchException(ERR_MSG_LOG4J_CONFIG);
                }
                System.setProperty(PROP_LOG4J_CONFIGURATION, args[++i]);
            } else {
                argList.add(args[i]);
            }
        }

        // Process the lib dir entries found on the command line
        final List<URL> libPathURLs = new ArrayList<URL>();
        for (final String libPath : libPaths) {
            addPath(libPath, true, libPathURLs);
        }
        final URL[] libJars = libPathURLs.toArray(new URL[libPathURLs.size()]);

        // Determine the CruiseControl directory for the distribution jars.
        // Use the system property if it was provided, otherwise make a guess
        // based upon the location of the launcher jar.
        File ccDistDir = null;
        final String ccDistDirProperty = System.getProperty(CCDISTDIR_PROPERTY);
        if (ccDistDirProperty != null) {
            ccDistDir = new File(ccDistDirProperty);
        }
        if ((ccDistDir == null) || !ccDistDir.exists()) {
            ccDistDir = distJarDir;
            System.setProperty(CCDISTDIR_PROPERTY, ccDistDir.getAbsolutePath());
        }
        final URL[] distJars = Locator.getLocationURLs(ccDistDir);

        // Determine CruiseControl library directory for third party jars.
        // Use the system property if it was provided, otherwise make a guess
        // based upon the CruiseControl home dir we found earlier.
        File ccLibDir = null;
        final String ccLibDirProperty = System.getProperty(CCLIBDIR_PROPERTY);
        if (ccLibDirProperty != null) {
            ccLibDir = new File(ccLibDirProperty);
        }
        if ((ccLibDir == null) || !ccLibDir.exists()) {
            ccLibDir = new File(ccHome, "lib");
            System.setProperty(CCLIBDIR_PROPERTY, ccLibDir.getAbsolutePath());
        }
        final URL[] supportJars = Locator.getLocationURLs(ccLibDir);
        final URL[] antJars = Locator.getLocationURLs(new File(ccLibDir, "ant"));

        // Locate any jars in the per-user lib directory
        final File userLibDir = new File(System.getProperty(USER_HOMEDIR),
                USER_LIBDIR);
        final URL[] userJars = noUserLib ? EMPTY_URL_ARRAY : Locator.getLocationURLs(userLibDir);

        // Locate the Java tools jar
        final File toolsJar = Locator.getToolsJar();

        // Concatenate our jar lists - order of precedence will be those jars
        // specified on the command line followed by jars in the per-user
        // lib directory and finally those jars found in the dist and lib
        // folders of CruiseControl home.
        int numJars = libJars.length + userJars.length + distJars.length + supportJars.length + antJars.length;
        if (toolsJar != null) {
            numJars++;
        }
        final URL[] jars = new URL[numJars];
        copyJarUrls(libJars, jars, 0);
        copyJarUrls(userJars, jars, libJars.length);
        copyJarUrls(distJars, jars, userJars.length + libJars.length);
        copyJarUrls(supportJars, jars, userJars.length + libJars.length + distJars.length);
        copyJarUrls(antJars, jars, userJars.length + libJars.length + distJars.length + supportJars.length);
        if (toolsJar != null) {
            jars[jars.length - 1] = toolsJar.toURI().toURL();
        }

        // Update the JVM java.class.path property
        final StringBuffer baseClassPath
            = new StringBuffer(System.getProperty("java.class.path"));
        if (baseClassPath.charAt(baseClassPath.length() - 1)
                == File.pathSeparatorChar) {
            baseClassPath.setLength(baseClassPath.length() - 1);
        }
        for (final URL jar : jars) {
            baseClassPath.append(File.pathSeparatorChar);
            baseClassPath.append(Locator.fromURI(jar.toString()));
        }
        baseClassPath.append(File.pathSeparatorChar);
        baseClassPath.append(".");

        // adding the homedirectory to the classpath
        baseClassPath.append(File.pathSeparatorChar);
        baseClassPath.append(ccHome.getAbsolutePath()).append(File.separatorChar);

        System.setProperty("java.class.path", baseClassPath.toString());
        System.out.println("Classpath: " + baseClassPath.toString());

        // Create a new class loader which has access to our jars
        final URLClassLoader loader = new URLClassLoader(jars);
        Thread.currentThread().setContextClassLoader(loader);

        // Launch CruiseControl!
        try {
            final Class mainClass = loader.loadClass(MAIN_CLASS);
            final CruiseControlMain main = (CruiseControlMain) mainClass.newInstance();
            final boolean normalExit = main.start(argList.toArray(new String[argList.size()]));
            if (!normalExit) {
                exitWithErrorCode();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * System property name, when if true, bypasses the system.exit call when printing
     * the usage message. Intended for unit tests only.
     */
    public static final String SYSPROP_CCMAIN_SKIP_USAGE_EXIT = "cc.main.skip.usage.exit";

    private void exitWithErrorCode() {
        if (!Boolean.getBoolean(SYSPROP_CCMAIN_SKIP_USAGE_EXIT)) {
            System.exit(1);
        }
    }

    /** Exception message if CC Home directory couldn't be determined. */
    static final String MSG_BAD_CCHOME = "CruiseControl home is not set or could not be located.";

    /**
     * Determine and return the CC Home directory, and reset the
     * {@link #CCHOME_PROPERTY} to match if needed.
     * @param distJarDir the main CC dist directory containing
     * cruisecontrol.jar and cruisecontrol-launcher.jar, used to guess default home dir.
     * @return CruiseControl home directory
     * @throws LaunchException if CruiseControl home is not set or could not be located.
     */
    File getCCHomeDir(File distJarDir) throws LaunchException {
        // If the CruiseControl home dir was provided as a system property,
        // create a reference to it
        final File ccHome;
        final String ccHomeProperty = System.getProperty(CCHOME_PROPERTY);
        if (ccHomeProperty != null && (new File(ccHomeProperty)).exists()) {
            ccHome = new File(ccHomeProperty);

        // If the location was not specifed, or it does not exist, try to guess
        // the location based upon the location of the launcher Jar.
        } else if (distJarDir.getParentFile() != null) {
            ccHome = distJarDir.getParentFile();
            System.setProperty(CCHOME_PROPERTY, ccHome.getAbsolutePath());
            System.out.println("WARNING: " + CCHOME_PROPERTY + " reset to "
                    + System.getProperty(CCHOME_PROPERTY));
        } else {
            ccHome = null;
        }

        // If none of the above worked, give up now.
        if (ccHome == null || !ccHome.exists()) {
            throw new LaunchException(MSG_BAD_CCHOME);
        }
        if (ccHomeProperty == null) {            
            System.setProperty(CCHOME_PROPERTY, ccHome.toString());
        }
        return ccHome;
    }

    private void copyJarUrls(URL[] sourceArray, URL[] destinationArray, int destinationStartIndex) {
        System.arraycopy(sourceArray, 0, destinationArray, destinationStartIndex, sourceArray.length);
    }

    /**
     * Add a CLASSPATH or -lib to lib path urls.
     * @param path        the classpath or lib path to add to the libPathULRLs
     * @param getJars     if true and a path is a directory, add the jars in
     *                    the directory to the path urls
     * @param libPathURLs the list of paths to add to
     * @throws MalformedURLException if the URLs required for the classloader
     *            cannot be created.
     */
   private void addPath(final String path, final boolean getJars, final List<URL> libPathURLs)
       throws MalformedURLException {

       final StringTokenizer myTokenizer
           = new StringTokenizer(path, System.getProperty("path.separator"));
       while (myTokenizer.hasMoreElements()) {
           final String elementName = myTokenizer.nextToken();
           final File element = new File(elementName);
           if (elementName.indexOf("%") != -1 && !element.exists()) {
               continue;
           }
           if (getJars && element.isDirectory()) {
               // add any jars in the directory
               final URL[] dirURLs = Locator.getLocationURLs(element);
               libPathURLs.addAll(Arrays.asList(dirURLs));
           }

           libPathURLs.add(element.toURI().toURL());
       }
   }
}
