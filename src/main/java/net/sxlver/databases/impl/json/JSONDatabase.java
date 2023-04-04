package net.sxlver.databases.impl.json;

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.sxlver.databases.*;
import net.sxlver.databases.exception.DatabaseException;
import net.sxlver.databases.formatter.FieldNameFormatters;
import net.sxlver.databases.impl.DatabaseEntryIdentifier;
import net.sxlver.databases.impl.json.context.JSONConversionContext;
import net.sxlver.databases.util.FileSystemUtil;
import net.sxlver.databases.util.Reflection;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JSONDatabase<T> extends AbstractDatabase<T> {

    private static final String JSON_FILE_FORMAT = ".json";
    private static final String DATABASE_FOLDER = "database";
    private final ClassInfo classInfo;
    private final String databasePath;

    private final String tableName;

    private final DatabaseConverter<T, Map<String, Object>> converter;

    JSONDatabase(final @NonNull Class<?>         type,
                 final @NonNull DatabaseSettings settings,
                 final boolean                   catchMainThread,
                 final @Nullable Thread          thread
    ) {
        super(type, catchMainThread, thread);
        this.databasePath = FileSystemUtil.getJarFileDirectory(type) + settings.getDatabasePath() + File.separator + DATABASE_FOLDER;
        this.classInfo = ClassInfo.ofClass(type, FieldNameFormatters.IDENTITY);
        this.converter = (DatabaseConverter<T, Map<String, Object>>) classInfo.getConverter();
        this.tableName = Reflection.getTableName(type);
        super.init();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T read(final Object query)  {
        final String uniqueId = query.toString();
        final String path = getTypeDatabasePath();
        final String file = path + File.separator + uniqueId + JSON_FILE_FORMAT;
        if(!new File(file).exists()) {
            return null;
        }
        final Map<String, Object> map;
        try(final FileReader fileReader = new FileReader(file)) {
            map = (Map<String, Object>) getGson().fromJson(fileReader, Map.class);
        }catch(final Exception exception) {
            throw new DatabaseException(exception.getMessage());
        }
        final T object = converter.deserialize(map, JSONConversionContext.of(type));
        callMessageReceiver(object);
        return object;
    }

    @Override
    @SneakyThrows
    protected void write(final T object) {
        final Map<String, Object> serializedMap = converter.serialize(object, null);
        final String serialized = getGson().toJson(serializedMap);
        final String path = tableName;
        final String file = path + File.separator + Reflection.getUniqueIdentifier(object) + JSON_FILE_FORMAT;
        final File dataFolder = new File(getDatabasePath());
        final File destinationFile = new File(dataFolder, file);
        if(!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        final File table = new File(getTypeDatabasePath());
        if(!table.exists()) {
            table.mkdirs();
        }
        try (final FileWriter fileWriter = new FileWriter(destinationFile)) {
            fileWriter.write(serialized);
        } catch (final IOException exception) {
            throw new DatabaseException(exception.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Collection<T> readAll(final Object query) {
        final String uniqueId = query.toString();
        final String path = getTypeDatabasePath();
        // Filter files by the one's matching the unique id
        final List<File> results = Arrays.stream(FileSystemUtil.getFilesInDirectoryNonNull(new File(path)))
                .filter(file -> file.getName().startsWith(uniqueId))
                .collect(Collectors.toList());

        // initiate a new list that will hold the deserialized maps that
        // will later be converted back to their original representation
        final List<Map<String, Object>> mappedResults = Lists.newArrayList();
        // Iterate over filtered files
        for (final File result : results) {
            // temporarily store the deserialized map
            final Map<String, Object> map;
            // initiate a new FileReader for the file
            try(final FileReader fileReader = new FileReader(result)) {
                // deserialize the text that has been reed by the
                // FileReader and convert it to a Map<String,Object>
                map = (Map<String, Object>) getGson().fromJson(fileReader, Map.class);

                // add deserialized map to a list that will later be
                // deserialized all at once
                mappedResults.add(map);
            }catch(final Exception exception) {
                // Throw DatabaseException if a error is encountered
                // whilst processing the contents of the target file
                throw new DatabaseException(exception.getMessage());
            }
        }

        // Finally deserialize the maps to their original representation
        final Collection<T> objects =  mappedResults.stream().map(map
                -> converter.deserialize(map, JSONConversionContext.of(type)))
                .collect(Collectors.toList());

        for (final T object : objects) {
            callMessageReceiver(object);
        }
        return objects;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Collection<T> readAll() {
        final String path = getTypeDatabasePath();
        // Get the files in the data folder as list
        final List<File> results = Arrays.stream(FileSystemUtil.getFilesInDirectoryNonNull(new File(path))).collect(Collectors.toList());
        final List<Map<String, Object>> mappedResults = Lists.newArrayList();
        // Iterate over filtered files
        for (final File result : results) {
            // temporarily store the deserialized map
            final Map<String, Object> map;
            // initiate a new FileReader for the file
            try(final FileReader fileReader = new FileReader(result)) {
                // deserialize the text that has been reed by the
                // FileReader and convert it to a Map<String,Object>
                map = (Map<String, Object>) getGson().fromJson(fileReader, Map.class);
                // add deserialized map to a list that will later be
                // deserialized all at once
                mappedResults.add(map);
            }catch(final Exception exception) {
                // Throw DatabaseException if an error is encountered
                // whilst processing the contents of the target file
                throw new DatabaseException(exception.getMessage());
            }
        }

        // Finally deserialize the maps to their original representation
        return mappedResults.stream().map(map
                -> converter.deserialize(map, JSONConversionContext.of(type)))
                .collect(Collectors.toList());
    }

    @Override
    protected void delete(final Object query) {
        final String uniqueId = query.toString();
        Predicate<String> filterApplicator = file -> file.equals(uniqueId);
        if(query instanceof DatabaseEntryIdentifier) {
            filterApplicator = ((DatabaseEntryIdentifier) query).getFilterApplicator();
        }
        final String path = getTypeDatabasePath();
        final File dataFolder = new File(path);
        final File[] files = FileSystemUtil.getFilesInDirectoryNonNull(dataFolder);
        Predicate<String> finalFilterApplicator = filterApplicator;
        Arrays.stream(files).forEach(file -> {
            if(finalFilterApplicator.test(removeFileFormatter(file.getName()))) {
                file.delete();
            }
        });
    }

    @Override
    protected int fetchMaxId() {
        final Field autoIncrementField = Reflection.getAutoIncrementField(type, classInfo);
        if(autoIncrementField != null) {
            final ClassInfo.FieldInfo fieldInfo = classInfo.getInfo(autoIncrementField);
            final File typeDatabase = new File(getTypeDatabasePath());
            if (!typeDatabase.exists()) {
                return 0;
            }
            int maxId = 0;
            final String fieldName = fieldInfo.getFormattedName();
            for (final File file : typeDatabase.listFiles()) {
                final Map<String, Object> map;
                try (final FileReader fileReader = new FileReader(file)) {
                    map = (Map<String, Object>) getGson().fromJson(fileReader, Map.class);
                } catch (final Exception exception) {
                    throw new DatabaseException(exception.getMessage());
                }
                final int id = (Integer) map.get(fieldName);
                if(id > maxId) {
                    maxId = id;
                }
            }
        }
        return 0;
    }

    public static <T> JSONDatabase<T> of(final Class<T>         type,
                                         final DatabaseSettings settings,
                                         final boolean          catchMainThread,
                                         final Thread           thread
    ) {
        return new JSONDatabase<>(type, settings, catchMainThread, thread);
    }

    private String removeFileFormatter(final String string) {
        return string.replaceAll(JSON_FILE_FORMAT, "");
    }

    @Override
    protected ClassInfo getClassInfo() {
        return classInfo;
    }

    @Override
    protected void connect(final @NonNull DatabaseAuth auth) {}

    @Override
    public boolean isConnected() {
        return true;
    }

    public String getTypeDatabasePath() {
        return getDatabasePath() + File.separator + tableName;
    }

    public String getDatabasePath() {
        return databasePath;
    }
}
