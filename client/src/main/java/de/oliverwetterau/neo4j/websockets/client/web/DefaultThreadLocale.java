package de.oliverwetterau.neo4j.websockets.client.web;

import de.oliverwetterau.neo4j.websockets.core.i18n.ThreadLocale;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DefaultThreadLocale implements ThreadLocale {
    protected static ThreadLocal<Locale> threadLocal = new ThreadLocal<>();
    protected static Locale defaultLocale = Locale.US;

    @Override
    public void setLocale(Locale locale) {
        threadLocal.set(locale);
    }

    @Override
    public Locale getLocale() {
        Locale locale = threadLocal.get();

        return (locale == null) ? defaultLocale : locale;
    }
}
