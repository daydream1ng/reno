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
package net.sourceforge.cruisecontrol.dashboard.web.command;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.dashboard.BuildSummary;
import net.sourceforge.cruisecontrol.dashboard.PreviousResult;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.utils.CCDateFormatter;

import org.apache.commons.lang.StringUtils;

public class RSSBuildSummaryAdapterTest extends TestCase {
    private static final String PASSING_LOGFILE = DataUtils.PASSING_BUILD_LBUILD_0_XML;
    private static final String FAILED_LOGFILE = DataUtils.FAILING_BUILD_XML;

    public void testShouldConvertPassedToBuildSucceed() throws Exception {
        BuildSummary summary = new BuildSummary("", PreviousResult.PASSED, PASSING_LOGFILE);
        RSSBuildSummaryAdapter adapter = new RSSBuildSummaryAdapter("", summary);
        assertEquals(summary.getPreviousBuildResult().getStatus().toLowerCase(), adapter.getStatus());
    }

    public void testShouldConvertFailedToBuildFailed() throws Exception {
        BuildSummary summary = new BuildSummary("", PreviousResult.FAILED, FAILED_LOGFILE);
        RSSBuildSummaryAdapter adapter = new RSSBuildSummaryAdapter("", summary);
        assertEquals(summary.getPreviousBuildResult().getStatus().toUpperCase(), adapter.getStatus());
    }

    public void testShouldConvertDateIntoCruiseControlRSSFormate() throws Exception {
        BuildSummary summary = new BuildSummary("", PreviousResult.PASSED, PASSING_LOGFILE);
        String expected = CCDateFormatter.format(summary.getBuildDate(), "EEE, dd MMM yyyy HH:mm:ss Z");
        RSSBuildSummaryAdapter adapter = new RSSBuildSummaryAdapter("", summary);
        assertEquals(expected, adapter.getPubDate());
    }

    public void testShouldReturnXmlIncludeNecessaryInfomationForFailedBuild() throws Exception {
        BuildSummary summary =
                new BuildSummary("connectfour", PreviousResult.FAILED, FAILED_LOGFILE);
        RSSBuildSummaryAdapter adapter =
                new RSSBuildSummaryAdapter("http://localhost:8080/dashboard/", summary);
        String xml = adapter.toXml();
        String expected = CCDateFormatter.format(summary.getBuildDate(), "EEE, dd MMM yyyy HH:mm:ss Z");
        assertTrue(StringUtils.contains(xml, "<title>connectfour FAILED " + expected + "</title>"));
        assertTrue(StringUtils.contains(xml, "<description>Build FAILED</description>"));
        assertTrue(StringUtils.contains(xml, "<pubDate>" + expected + "</pubDate>"));
        assertTrue(StringUtils.contains(xml,
                "<link>http://localhost:8080/dashboard/tab/build/detail/connectfour</link>"));
    }

    public void testShouldReturnXmlIncludeNecessaryInfomationForPassedBuild() throws Exception {
        BuildSummary summary =
                new BuildSummary("connectfive", PreviousResult.PASSED, PASSING_LOGFILE);
        RSSBuildSummaryAdapter adapter =
                new RSSBuildSummaryAdapter("http://localhost:8080/dashboard", summary);
        String xml = adapter.toXml();
        assertTrue(StringUtils.contains(xml, "<title>connectfive passed"));
        assertTrue(StringUtils.contains(xml, "<description>Build passed</description>"));
    }

    public void testShouldNotReturnPubDateWhenProjectIsInactive() throws Exception {
            BuildSummary summary =
                    new BuildSummary("connectfive", PreviousResult.PASSED, PASSING_LOGFILE) {
                        public PreviousResult getPreviousBuildResult() {
                            return PreviousResult.UNKNOWN;
                        }
                    };

        RSSBuildSummaryAdapter adapter = new RSSBuildSummaryAdapter("localost", summary);
        assertEquals("", adapter.getPubDate());
    }

    public void testShouldReturnPubDateWhenProjectIsActive() throws Exception {
        BuildSummary summary = new BuildSummary("", PreviousResult.PASSED, PASSING_LOGFILE);
        RSSBuildSummaryAdapter adapter = new RSSBuildSummaryAdapter("", summary);
        assertEquals(CCDateFormatter.format(summary.getBuildDate(),
                "EEE, dd MMM yyyy HH:mm:ss Z"), adapter.getPubDate());
    }


    public void testShouldReturnCruiseControlCompatibleStatusForPassedBuild() throws Exception {
        BuildSummary summary = new BuildSummary("", PreviousResult.PASSED, PASSING_LOGFILE);
        RSSBuildSummaryAdapter adapter = new RSSBuildSummaryAdapter("", summary);
        assertEquals("passed", adapter.getStatus());
    }
    
    public void testShouldReturnCruiseControlCompatibleStatusForFailedBuild() throws Exception {
        BuildSummary summary = new BuildSummary("", PreviousResult.FAILED, PASSING_LOGFILE);
        RSSBuildSummaryAdapter adapter = new RSSBuildSummaryAdapter("", summary);
        assertEquals("FAILED", adapter.getStatus());
    }
}
