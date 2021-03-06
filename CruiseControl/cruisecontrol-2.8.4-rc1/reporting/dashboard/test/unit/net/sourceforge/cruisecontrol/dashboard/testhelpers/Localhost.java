/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.testhelpers;

import net.sourceforge.cruisecontrol.dashboard.service.SystemPropertyConfigService;
import net.sourceforge.cruisecontrol.dashboard.service.DashboardConfigFileFactory;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.jmxstub.BuildLoopQueryServiceStub;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

public final class Localhost {

    private static final int PORT = 9090;

    private final Server server;

    public static void main(String[] args) throws Exception {
        DataUtils.cloneCCHome();
        Localhost localhost = new Localhost();
        localhost.server.start();
    }

    private Localhost() throws Exception {
        System.setProperty(DashboardConfigFileFactory.PROPS_CC_DASHBOARD_CONFIG, DataUtils
                .getDashboardConfigXmlOfWebApp().getAbsolutePath());
        System.setProperty(SystemPropertyConfigService.PROPS_CC_CONFIG_LOG_DIR, DataUtils.getLogDirAsFile()
                .getAbsolutePath());
        System.setProperty(SystemPropertyConfigService.PROPS_CC_CONFIG_ARTIFACTS_DIR, DataUtils
                .getArtifactsDirAsFile().getAbsolutePath());
        System.setProperty(BuildLoopQueryServiceStub.PROPS_CC_CONFIG_FILE, DataUtils.getConfigXmlAsFile()
                .getAbsolutePath());
        System.setProperty(SystemPropertyConfigService.PROPS_CC_CONFIG_FORCEBUILD_ENABLED, "enabled");
        server = new Server(PORT);
        server.addHandler(new WebAppContext("webapp", "/dashboard"));
    }

}
