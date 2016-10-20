package eu.ehri.project.test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

/**
 * XML resource resolver that tries to resolve links
 * to local resources, and complains if it can't find
 * them.
 * <p>
 * Largely borrowed from <a href="http://stackoverflow.com/a/2342859/285374">
 * this StackOverflow answer</a>.
 */
class LocalResourceResolver implements LSResourceResolver {

    public LSInput resolveResource(String type, String namespaceURI,
            String publicId, String systemId, String baseURI) {

        // note: in this sample, the XSD's are expected to be in the root of the classpath
        String id = systemId.substring(systemId.lastIndexOf("/") + 1);
        try {
            try {
                URL resourceUrl = Resources.getResource(id);
                String s = Resources.toString(resourceUrl, Charsets.UTF_8);
                return new Input(publicId, systemId, s);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("No local resource found for id: " + systemId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Input implements LSInput {

        private String publicId;

        private String systemId;

        public String getPublicId() {
            return publicId;
        }

        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        public String getBaseURI() {
            return null;
        }

        public InputStream getByteStream() {
            return null;
        }

        public boolean getCertifiedText() {
            return false;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public String getEncoding() {
            return null;
        }

        public String getStringData() {
            return stringData;
        }

        public void setBaseURI(String baseURI) {
        }

        public void setByteStream(InputStream byteStream) {
        }

        public void setCertifiedText(boolean certifiedText) {
        }

        public void setCharacterStream(Reader characterStream) {
        }

        public void setEncoding(String encoding) {
        }

        public void setStringData(String stringData) {
        }

        public String getSystemId() {
            return systemId;
        }

        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        private final String stringData;

        Input(String publicId, String sysId, String stringData) {
            this.publicId = publicId;
            this.systemId = sysId;
            this.stringData = stringData;
        }
    }
}
