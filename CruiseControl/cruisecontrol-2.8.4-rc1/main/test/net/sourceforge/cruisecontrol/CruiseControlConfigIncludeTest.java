package net.sourceforge.cruisecontrol;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.config.FileResolver;
import net.sourceforge.cruisecontrol.config.XmlResolver;
import net.sourceforge.cruisecontrol.util.Util;

import org.jdom.Element;

public class CruiseControlConfigIncludeTest extends TestCase {

    private Element rootElement;
    private Element includeElement;
    private XmlResolver xmlResolver;
    private FileResolver fileResolver;

    protected void setUp() throws Exception {
        StringBuffer configText = new StringBuffer(200);
        configText.append("<cruisecontrol>");
        configText.append("  <plugin name='foo.project'");
        configText.append("  classname='net.sourceforge.cruisecontrol.MockProjectInterface'/>");
        configText.append("  <include.projects file='include.xml'/>");
        configText.append("  <foo.project name='in.root'/>");
        configText.append("</cruisecontrol>");
        rootElement = elementFromString(configText.toString());

        StringBuffer includeText = new StringBuffer(200);
        includeText.append("<cruisecontrol>");
        includeText.append("  <foo.project name='in.include'/>");
        includeText.append("</cruisecontrol>");
        includeElement = elementFromString(includeText.toString());

        xmlResolver = new IncludeXmlResolver(includeElement);

        fileResolver = new EmptyFileResolver();
    }

    protected void tearDown() throws Exception {
        rootElement = null;
        includeElement = null;
        xmlResolver = null;
    }

    public void testShouldLoadIncludedProjects() throws Exception {
        CruiseControlConfig config = new CruiseControlConfig(rootElement, xmlResolver, null);
        assertEquals(2, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
        assertIsFooProject(config.getProject("in.include"));
    }
    
    public void testShouldLoadedNestedIncludes() throws Exception {        
        StringBuffer includeText = new StringBuffer(200);
        includeText.append("<cruisecontrol>");
        includeText.append("  <include.projects file='include.xml'/>");
        includeText.append("  <foo.project name='in.first.include'/>");
        includeText.append("</cruisecontrol>");
        Element includeWithNestedInclude = elementFromString(includeText.toString());
        
        Element[] elements = new Element[2];
        elements[0] = includeWithNestedInclude;
        elements[1] = includeElement;
        xmlResolver = new IncludeXmlResolver(elements);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, xmlResolver, null);        
        assertEquals(3, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
        assertIsFooProject(config.getProject("in.first.include"));
        assertIsFooProject(config.getProject("in.include"));
    }

    public void testIncludesCanDefinePlugins() throws CruiseControlException {
        String newProjectTag = "new.project.type";
        
        Element pluginElement = new Element("plugin");
        pluginElement.setAttribute("name", newProjectTag);
        pluginElement.setAttribute("classname", MockProjectInterface.class.getName());
        includeElement.addContent(pluginElement);

        Element barElement = new Element(newProjectTag);
        barElement.setAttribute("name", "bar");
        includeElement.addContent(barElement);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, xmlResolver, null);
        assertEquals(3, config.getProjectNames().size());
        assertIsFooProject(config.getProject("bar"));
    }
    
    public void testPropertiesShouldBeAvailableToIncludedProjects() throws CruiseControlException {
        Element property = new Element("property");
        property.setAttribute("name", "baz");
        property.setAttribute("value", "goo");
        rootElement.addContent(property);
        
        Element project = new Element("foo.project");
        project.setAttribute("name", "${baz}");
        includeElement.addContent(project);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, xmlResolver, fileResolver);
        assertEquals(3, config.getProjectNames().size());
        assertIsFooProject(config.getProject("goo"));
    }
    
    public void testErrorsInIncludeShouldBeContained() throws CruiseControlException {
        Element unknownPlugin = new Element("unknown.plugin.error");
        includeElement.addContent(unknownPlugin);
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, xmlResolver, fileResolver);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
    }
    
    public void testErrorsParsingIncludeShouldBeContained() throws CruiseControlException {
        XmlResolver resolverHitsError = new XmlResolver() {
            public Element getElement(String path) throws CruiseControlException {
                throw new CruiseControlException("simulate parse error");
            }
        };
        
        CruiseControlConfig config = new CruiseControlConfig(rootElement, resolverHitsError, fileResolver);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
    }

    public void testIncludeFilenameContainsProperty() throws CruiseControlException {
        XmlResolver includeFOOXmlResolver = new XmlResolver() {
            private final Element includePropertyElement;
            {
                StringBuffer includeText = new StringBuffer(200);
                includeText.append("<cruisecontrol>");
                includeText.append("  <foo.project name='in.include.withproperty'/>");
                includeText.append("</cruisecontrol>");
                includePropertyElement = elementFromString(includeText.toString());
            }

            public Element getElement(String path) throws CruiseControlException {
                assertEquals("include_FOO_.xml", path);
                return includePropertyElement;
            }
        };

        Element propertyElement = new Element("property");
        propertyElement.setAttribute("name", "filenameswitch");
        propertyElement.setAttribute("value", "_FOO_");
        rootElement.addContent(propertyElement);
        rootElement.removeChild("include.projects");
        Element includeTagElement = new Element("include.projects");
        includeTagElement.setAttribute("file", "include${filenameswitch}.xml");
        rootElement.addContent(includeTagElement);

        CruiseControlConfig config = new CruiseControlConfig(rootElement, includeFOOXmlResolver, fileResolver);
        assertEquals(2, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
        assertIsFooProject(config.getProject("in.include.withproperty"));
    }

    public void testIncludeFilenameContainsUnsetProperty() throws CruiseControlException {
        XmlResolver includeUnknownXmlResolver = new XmlResolver() {
            public Element getElement(String path) throws CruiseControlException {
                throw new CruiseControlException("failed to load file []");
            }
        };

        rootElement.removeChild("include.projects");
        Element includeTagElement = new Element("include.projects");
        includeTagElement.setAttribute("file", "include${filenameswitch}.xml");
        rootElement.addContent(includeTagElement);

        CruiseControlConfig config = new CruiseControlConfig(rootElement, includeUnknownXmlResolver, fileResolver);
        assertEquals(1, config.getProjectNames().size());
        assertIsFooProject(config.getProject("in.root"));
    }

    public static Element elementFromString(String text) throws CruiseControlException {
        InputStream is = new ByteArrayInputStream(text.getBytes());
        return Util.loadRootElement(is);
    }

    private void assertIsFooProject(ProjectInterface project) {
        assertNotNull(project);
        assertEquals(MockProjectInterface.class.getName(), project.getClass().getName());
    }
    
    private class IncludeXmlResolver implements XmlResolver {
        
        private final Element[] includeElements;
        private int count = 0;
        
        IncludeXmlResolver(Element element) {
            includeElements = new Element[] {element};
        }
        
        IncludeXmlResolver(Element[] elements) {
            includeElements = elements;
        }
        
        public Element getElement(String path) throws CruiseControlException {
            assertEquals("include.xml", path);
            Element element = includeElements[count];
            count++;
            return element;
        }
    }

    private class EmptyFileResolver implements FileResolver {

        public InputStream getInputStream(String path)
            throws CruiseControlException {
            // FIXME add correct implementation, if required!
            throw new CruiseControlException("Method not implemented yet! Fix it!");
        }
    }
}
