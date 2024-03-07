package antessio.common;

public interface JsonConverter {

   <T> T fromJson(String json, Class<T> cls);

   <T> String toJson(T object);

}
