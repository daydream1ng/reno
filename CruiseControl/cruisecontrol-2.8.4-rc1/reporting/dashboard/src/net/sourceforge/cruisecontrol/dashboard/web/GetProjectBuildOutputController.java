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
package net.sourceforge.cruisecontrol.dashboard.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.cruisecontrol.dashboard.service.BuildLoopQueryService;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import java.io.PrintWriter;

public class GetProjectBuildOutputController implements Controller {

    private static final Logger LOGGER = Logger.getLogger(GetProjectBuildOutputController.class);

    /** Url parameter name. */
    static final String PARAM_OUTPUT_ID = "outputid";
    /** Default value initialized in build_detail_observer.js for outputid. */
    static final String DEFAULT_OUTPUT_ID = "emptyid";

    private final BuildLoopQueryService buildLoopQueryService;

    public GetProjectBuildOutputController(BuildLoopQueryService service) {
        this.buildLoopQueryService = service;
    }

    public ModelAndView handleRequest(final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {
        
        final String projectName = request.getParameter("project");

        final String oldOutputID = request.getParameter(PARAM_OUTPUT_ID);

        final String start = request.getParameter("start");
        final int startAsInt = (start == null) ? 0 : Integer.parseInt(start);

        final String[] output = buildLoopQueryService.getBuildOutput(projectName, startAsInt);

        final String newOutputID;
        if (output == null || output.length == 0
                // see build_detail_observer.js for how outputid default and how is set as a parameter
                || DEFAULT_OUTPUT_ID.equals(oldOutputID)) {
            newOutputID = buildLoopQueryService.getLiveOutputID(projectName);
        } else {
            // only read new ID if output is empty
            newOutputID = oldOutputID;
        }

        final int nextStart = calculateNextStart(oldOutputID, newOutputID, startAsInt, output);

        if (LOGGER.isDebugEnabled()) {
            final String msg = "*********** ----------- **************"
                    + "\nparam start: " + start
                    + "\nparam oldOutputID: " + oldOutputID
                    + "\nnewOutputID: " + newOutputID
                    + "\nnextStart: " + nextStart
                    + "\noutput: " + (output == null ? null : "lines: " + output.length)
                    + "\n*********** ----------- **************";
            LOGGER.debug(msg);
        }
        
        response.addHeader("X-JSON", "[" + nextStart + ", \"" + newOutputID + "\"]");
        response.setContentType("text/plain");
        if (output != null) {
            final PrintWriter writer = response.getWriter();
            try {
                writer.write(StringUtils.join(output, "\n"));
                if (output.length > 0) {
                    response.getWriter().write("\n");
                }
            } finally {
                writer.close();
            }
        }
        return null;
    }

    int calculateNextStart(final String oldOutputID, final String newOutputID,
                           final int start, final String[] outputs) {

        if (outputs == null || outputs.length == 0) {
            // Use OutputID change to determine if logger changed, and reset start line if needed.
            if (newOutputID == null || newOutputID.equals(oldOutputID)) {
                return start;
            } else {
                return 0;
            }
        }
        final String firstLine = outputs[0];
        if (firstLine.startsWith("Skipped") && firstLine.endsWith("lines")) {
            final String skippedLines = StringUtils.remove(StringUtils.remove(firstLine, "Skipped"), "lines").trim();
            return start + Integer.parseInt(skippedLines) + outputs.length - 1;
        }
        return start + outputs.length;
    }
}
