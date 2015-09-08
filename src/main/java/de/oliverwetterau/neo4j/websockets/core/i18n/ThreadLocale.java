package de.oliverwetterau.neo4j.websockets.core.i18n;

import java.util.Locale;

/**
 * Created by oliver on 26.08.15.
 */
public interface ThreadLocale {
    void setLocale(final Locale locale);
    Locale getLocale();
}
