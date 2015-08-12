package com.zhy.base.imageloader;

import java.util.Locale;

public enum Scheme
{
    HTTP("http"), HTTPS("https"), FILE("file"), CONTENT("content"), ASSETS("assets"), DRAWABLE("drawable"), UNKNOWN("");

    private String scheme;
    private String uriPrefix;

    Scheme(String scheme)
    {
        this.scheme = scheme;
        uriPrefix = scheme + "://";
    }

    /**
     * Defines scheme of incoming URI
     *
     * @param uri URI for scheme detection
     * @return Scheme of incoming URI
     */
    public static Scheme ofUri(String uri)
    {
        if (uri != null)
        {
            for (Scheme s : values())
            {
                if (s.belongsTo(uri))
                {
                    return s;
                }
            }
        }
        return UNKNOWN;
    }

    public boolean belongsTo(String uri)
    {
        return uri.toLowerCase(Locale.US).startsWith(uriPrefix);
    }

    /**
     * Appends scheme to incoming path
     */
    public String wrap(String path)
    {
        return uriPrefix + path;
    }

    /**
     * Removed scheme part ("scheme://") from incoming URI
     */
    public String crop(String uri)
    {
        if (!belongsTo(uri))
        {
            throw new IllegalArgumentException(String.format("URI [%1$s] doesn't have expected scheme [%2$s]", uri, scheme));
        }
        return uri.substring(uriPrefix.length());
    }
}