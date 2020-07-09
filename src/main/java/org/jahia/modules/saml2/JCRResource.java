package org.jahia.modules.saml2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import javax.jcr.RepositoryException;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

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
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {
                public Boolean doInJCR(final JCRSessionWrapper session) throws RepositoryException {
                    return session.getNode(path) != null;
                }
            });
        } catch (RepositoryException ex) {
            logger.error("Impossible to check if the node exists", ex);
        }
        return false;
    }

    @Override
    public String getFilename() {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<String>() {
                public String doInJCR(final JCRSessionWrapper session) throws RepositoryException {
                    final JCRNodeWrapper node = session.getNode(path);
                    final String filename;
                    if (node == null) {
                        throw new IllegalStateException(String.format("Impossible to get filename from %s", path));
                    } else {
                        filename = node.getName();
                    }
                    return filename;
                }
            });
        } catch (RepositoryException ex) {
            logger.error("Impossible to get filename", ex);
        }
        return (new File(path)).getName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<InputStream>() {
                public InputStream doInJCR(final JCRSessionWrapper session) throws RepositoryException {
                    final JCRNodeWrapper node = session.getNode(path);
                    if (node.isFile()) {
                        return node.getFileContent().downloadFile();
                    }
                    throw new IllegalStateException(String.format("Impossible to get InputStream from %s", path));
                }
            });
        } catch (RepositoryException ex) {
            logger.error("Impossible to check if the node exists", ex);
        }
        throw new IllegalStateException(String.format("Impossible to get InputStream from %s", path));
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
