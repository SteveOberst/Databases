package net.sxlver.databases.impl;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.Map;

public class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> token) {
        final Class<?> type = token.getRawType();
        if(ConfigurationSerializable.class.isAssignableFrom(type)) {
            return (TypeAdapter<T>) new BukkitTypeAdapter(gson.getAdapter(Map.class));
        }
        return null;
    }
}
