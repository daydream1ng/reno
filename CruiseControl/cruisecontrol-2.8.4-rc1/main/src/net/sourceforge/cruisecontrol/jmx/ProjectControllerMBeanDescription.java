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
package net.sourceforge.cruisecontrol.jmx;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import mx4j.MBeanDescriptionAdapter;

/**
 * @author <a href="mailto:joriskuipers@xs4all.nl">Joris Kuipers</a>
 */
public class ProjectControllerMBeanDescription extends MBeanDescriptionAdapter {

    private static final Map<String, String> METHOD_DESCRIPTIONS;

    static {
        METHOD_DESCRIPTIONS = new HashMap<String, String>();

        METHOD_DESCRIPTIONS.put("pause", "Pauses the project");
        METHOD_DESCRIPTIONS.put("resume", "Resumes the project when it's paused");
        METHOD_DESCRIPTIONS.put("build", "Forces a build of the project");
        METHOD_DESCRIPTIONS.put("buildWithTarget", "Forces a build of the project using the given target");
        METHOD_DESCRIPTIONS.put("serialize", "Persists the state of the project to disk");
        METHOD_DESCRIPTIONS.put("commitMessages", "Gets the commit messages which"
                 + " include the user name and the message.");

        METHOD_DESCRIPTIONS.put("getLogLabelLines", "Lines from the given firstLine up to max lines, "
                + "or an empty array if no more lines exist");

        METHOD_DESCRIPTIONS.put("getBuildOutput", "Output from the live output buffer, after line "
                + "specified (inclusive)");
    }

    private static final Map<String, String> METHOD_PARAMETER_NAME;
    
    static {
        METHOD_PARAMETER_NAME = new HashMap<String, String>();

        METHOD_PARAMETER_NAME.put("buildWithTarget-0", "target");
        METHOD_PARAMETER_NAME.put("buildWithTarget-1", "addedProperties");

        METHOD_PARAMETER_NAME.put("getLogLabelLines-0", "logLabel");
        METHOD_PARAMETER_NAME.put("getLogLabelLines-1", "firstLine");

        METHOD_PARAMETER_NAME.put("getBuildOutput-0", "firstLine");

    }
    
    private static final Map<String, String> METHOD_PARAMETER_DESCRIPTIONS;
    
    static {
        METHOD_PARAMETER_DESCRIPTIONS = new HashMap<String, String>();

        METHOD_PARAMETER_DESCRIPTIONS.put("buildWithTarget-0", "The target to invoke");
        METHOD_PARAMETER_DESCRIPTIONS.put("buildWithTarget-1",
                "The additional properties that will be passed to the build");

        METHOD_PARAMETER_DESCRIPTIONS.put("getLogLabelLines-0", "A valid build label, must exist in the list "
                + "returned by getLogLabels()");
        METHOD_PARAMETER_DESCRIPTIONS.put("getLogLabelLines-1", "The starting line to skip to in the log for the given "
                + "build label");

        METHOD_PARAMETER_DESCRIPTIONS.put("getBuildOutput-0", "The starting line to skip to");

    }

    private static final Map<String, String> ATTR_DESCRIPTIONS;

    static {
        ATTR_DESCRIPTIONS = new HashMap<String, String>();
        ATTR_DESCRIPTIONS.put("ConfigFileName",
                              "The name of the config file this project reads its settings from");

        ATTR_DESCRIPTIONS.put("Label", "The current build label");

        ATTR_DESCRIPTIONS.put("LabelIncrementer",
                              "The classname of the LabelIncrementer used to determine the build label. "
                            + "Changes to this attribute are not persisted");

        ATTR_DESCRIPTIONS.put("LastBuild",
                              "Time of the last build, using the format 'yyyyMMddHHmmss'");

        ATTR_DESCRIPTIONS.put("LastSuccessfulBuild",
                              "Time of the last successful build, using the format 'yyyyMMddHHmmss'");

        ATTR_DESCRIPTIONS.put("LogDir",
                              "The directory where the log files for this project are written to. "
                            + "Changes to this attribute are not persisted");

        ATTR_DESCRIPTIONS.put("LogLabels", 
                              "A list with the names of the available log files.");

        ATTR_DESCRIPTIONS.put("OutputLoggerID",
                              "A unique (for this VM) identifying string for a logger instance. Intended to allow "
                                      + "reporting apps (eg: Dashboard) to check if the live output log file has been "
                                      + "reset and to start asking for output from the first line "
                                      + "of the current output file if the logger has changed.");

        ATTR_DESCRIPTIONS.put("ProjectName", "The name of this project");

        ATTR_DESCRIPTIONS.put("BuildInterval",
                              "The build interval in milliseconds. Changes to this attribute are not persisted");

        ATTR_DESCRIPTIONS.put("Status", "The current status of the project");

        ATTR_DESCRIPTIONS.put("Paused", "Indicates if the project is paused");

        ATTR_DESCRIPTIONS.put("BuildStartTime",
                              "Start Time of the last build, using the format 'yyyyMMddHHmmss'");
    }

    public String getOperationDescription(final Method method) {
        final String methodName = method.getName();
        if (METHOD_DESCRIPTIONS.containsKey(methodName)) {
            return METHOD_DESCRIPTIONS.get(methodName);
        }
        return super.getOperationDescription(method);
    }
    

    public String getOperationParameterName(final Method method, final int index) {
        if (method != null) {
            final String methodName = method.getName() + "-" + index;
            if (METHOD_PARAMETER_NAME.containsKey(methodName)) {
                return METHOD_PARAMETER_NAME.get(methodName);
            }
        }
        return super.getOperationParameterName(method, index);
    }
    
    public String getOperationParameterDescription(final Method method, final int index) {
        if (method != null) {
            final String methodName = method.getName() + "-" + index;
            if (METHOD_PARAMETER_DESCRIPTIONS.containsKey(methodName)) {
                return METHOD_PARAMETER_DESCRIPTIONS.get(methodName);
            }
        }
        return super.getOperationParameterDescription(method, index);
    }
    
    

    public String getAttributeDescription(final String attr) {
        if (ATTR_DESCRIPTIONS.containsKey(attr)) {
            return ATTR_DESCRIPTIONS.get(attr);
        }
        return super.getAttributeDescription(attr);
    }

    public String getMBeanDescription() {
        return "Controller for a CruiseControl project";
    }
}