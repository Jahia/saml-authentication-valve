package org.jahia.modules.saml2;

import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class JCRResource implements Resource {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JCRResource.class);

    private final String path;

    public JCRResource(String path) {
        this.path = path;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean exists() {
        try {
            return getSession().itemExists(path);
        } catch (RepositoryException ex) {
            logger.error("Impossible to check if the node exists", ex);
        }
        return false;
    }

    @Override
    public String getFilename() {
        try {
            return getSession().getItem(path).getName();
        } catch (RepositoryException ex) {
            logger.error("Impossible to get filename", ex);
        }
        return (new File(path)).getName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return getSession().getProperty(path).getBinary().getStream();
        } catch (RepositoryException ex) {
            logger.error("Impossible to read the node", ex);
        }
        throw new IllegalStateException(String.format("Impossible to get InputStream from %s", path));
    }

    private JCRSessionWrapper getSession() throws RepositoryException {
        return JCRSessionFactory.getInstance().getCurrentSystemSession(null, null, null);
    }

    @Override
    public URL getURL() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI getURI() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getFile() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long contentLength() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long lastModified() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource createRelative(String arg0) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException();
    }
}
