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
package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * This class allows you to start up ClearCase dynamic views and mount VOBs
 * before you initiate your build. If your view has been stopped, a VOB
 * unmounted or your machine rebooted, the likelihood is that your build will
 * fail when using dynamic views. The class therefore allows you to specify a
 * viewpath, from which it works out the view tag and starts it, optionally you
 * can specify voblist, a comma separated list of VOBs to mount. The reason a
 * viewpath is used rather than just the view path is that you can reuse a
 * CruiseControl property which defines the source of your build. You should
 * always specify the viewpath via the root location, i.e. M:\... on Windows or
 * /view/... on Unix Usage: &lt;clearcaseviewstrapper
 * viewpath="M:\dynamic_view\some_vob\src" voblist="\SourceVOB,\ReleaseVOB"/%gt;
 * 
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class ClearCaseViewstrapper implements Bootstrapper {

    private static final Logger LOG = Logger.getLogger(ClearCaseViewstrapper.class);

    private String viewpath;
    private String voblist;

    /**
     * set the path to the view to be started
     * 
     * @param path
     *            path to view to be started
     */
    public void setViewpath(String path) {
        viewpath = path;
    }

    /**
     * set the list of VOBs to mount, the list is comma separated
     * 
     * @param list
     *            comma separated list of VOBs to mount
     */
    public void setVoblist(String list) {
        voblist = list;
    }

    /**
     * start the specified view and VOBs.
     */
    public void bootstrap() throws CruiseControlException {
        buildStartViewCommand().executeAndWait(LOG);

        // have we got some VOBs to mount
        if (voblist != null) {
            String[] vobs = getVobsFromList(voblist);
            for (int i = 0; i < vobs.length; i++) {
                // mount the vob(s)
                try {
                    LOG.debug("mount vob=" + vobs[i]);
                    buildMountVOBCommand(vobs[i]).executeAndWait(LOG);
                } catch (CruiseControlException cceMount) {
                    // log the exception, do not throw.
                    // Note: warn because the exception may result from mounting
                    // a vob that was already mounted or it
                    // could be that the vob does not exist
                    LOG.warn("mount exception=" + cceMount);
                }
            }

            // test if the view path is now available
            try {
                LOG.debug("ls viewpath=" + viewpath);
                buildListVOBCommand(viewpath).executeAndWait(LOG);
            } catch (CruiseControlException cceList) {
                // log the exception, do not throw
                // Note: error because the exception is most likely a problem
                LOG.error("ls exception=" + cceList);
            }
        }
    }

    private String[] getVobsFromList(String voblist) {
        return voblist.split(",");
    }

    /**
     * check whether the appropriate attributes have been set
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(viewpath, "viewpath", this.getClass());
    }

    /**
     * build a command line for starting the view
     */
    protected Commandline buildStartViewCommand() {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");

        commandLine.createArguments("startview", getViewName());

        return commandLine;
    }

    /**
     * build a command line for starting a VOB
     */
    protected Commandline buildMountVOBCommand(String vob) {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");

        commandLine.createArguments("mount", vob);

        return commandLine;
    }

    /**
     * build a command line to list (ls) a file/folder
     */
    protected Commandline buildListVOBCommand(String f) {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("cleartool");
        commandLine.createArguments("ls", f);
        return commandLine;
    }

    /**
     * work out the view tag from the viewpath
     */
    private String getViewName() {
        String viewname;
        if (isWindows()) {
            viewname = getWindowsViewname(viewpath);
        } else {
            viewname = getUnixViewname(viewpath);
        }
        return viewname;
    }

    // second part after /view, i.e. /view/viewname
    private String getUnixViewname(String viewpath) {
        String[] details = viewpath.split("/", 4);
        return details.length < 3 ? null : details[2];
    }

    // first part after M: drive, i.e. M:\viewname
    private String getWindowsViewname(String viewpath) {
        String[] details = viewpath.split("\\\\", 3);
        return details.length < 2 ? null : details[1];
    }

    protected boolean isWindows() {
        return Util.isWindows();
    }
}
