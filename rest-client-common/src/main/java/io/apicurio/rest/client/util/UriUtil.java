package io.apicurio.rest.client.util;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UriUtil {

    public static URI buildURI(String basePath, Map<String, List<String>> queryParams, List<String> pathParams) {
        final Object[] encodedPathParams = pathParams
                .stream()
                .map(UriUtil::encodeURIComponent)
                .toArray();

        String path = String.format(basePath, encodedPathParams);

        if (!queryParams.isEmpty()) {
            path = path.concat("?");

            //Iterate over query params list so we can add multiple query params with the same key
            final Iterator<String> keyIterator = queryParams.keySet().iterator();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();
                final Iterator<String> valueIterator = queryParams.get(key).iterator();

                while (valueIterator.hasNext()) {
                    String value = valueIterator.next();
                    path = path.concat(key).concat("=").concat(value);
                    path = appendAmpersandIfNeeded(valueIterator, path);
                }
                path = appendAmpersandIfNeeded(keyIterator, path);
            }
        }

        return URI.create(path);
    }

    //
    private static String appendAmpersandIfNeeded(Iterator<String> iterator, String path) {
        if (iterator.hasNext() && !(path.lastIndexOf("&") == path.length() - 1)) {
            return path.concat("&");
        } else {
            return path;
        }
    }

    private static String encodeURIComponent(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
