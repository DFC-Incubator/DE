package org.iplantc.de.server.oauth;

import org.iplantc.de.server.auth.CasUrlConnector;

public class CasOAuthCallbackServlet  extends OAuthCallbackServlet {

    public CasOAuthCallbackServlet(final String serviceUrl) {
        super(serviceUrl);
        setUrlConnector(new CasUrlConnector());
    }
}