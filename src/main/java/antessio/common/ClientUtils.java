package antessio.common;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class ClientUtils {
    private ClientUtils(){}

    public static String getQueryParameters(Map<String, Object> parametersMap){
        return Optional.ofNullable(parametersMap)
                       .filter(Predicate.not(Map::isEmpty))
                       .map(ClientUtils::toQueryString)
                       .map(q -> "?" + q)
                       .orElse("");
    }
    private static String toQueryString(Map<String, Object> parametersMap) {
        if (parametersMap.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        parametersMap.forEach((key, value) -> sb.append("%s=%s&".formatted(encodeKey(key), encodeValue(value))));

        return sb.toString();
    }
    private static String encodeValue(Object value) {
        if (value instanceof Collection<?> collection) {
            encode(collection.stream().map(Object::toString).reduce("", "%s,%s"::formatted), UTF_8);
        }
        return encode(value.toString(), UTF_8);
    }

    private static String encodeKey(String key) {
        return encode(key, UTF_8);
    }

}
