package net.sxlver.databases.impl.mongodb;

import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.util.JSON;
import lombok.NonNull;
import net.sxlver.databases.*;
import net.sxlver.databases.exception.DatabaseException;
import net.sxlver.databases.formatter.FieldNameFormatters;
import net.sxlver.databases.impl.DatabaseEntryIdentifier;
import net.sxlver.databases.impl.mongodb.context.MongoDBConversionContext;
import net.sxlver.databases.util.Reflection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

public class MongoDBDatabase<T> extends AbstractDatabase<T> {

    private MongoClient client;
    private MongoDatabase database;
    private final MongoCollection<Document> mongoCollection;

    private final ClassInfo classInfo;
    private final DatabaseConverter<T, Map<String, Object>> converter;
    private final String collectionName;

    private static final String MONGO_ID = "_id";
    private static final String UNIQUE_ID = "uniqueIdentifier";

    MongoDBDatabase(final @NonNull Class<?>         type,
                    final @NonNull DatabaseSettings settings,
                    final boolean                   catchMainThread,
                    final @Nullable Thread          thread
    ) {
        super(type, catchMainThread, thread);
        this.classInfo = ClassInfo.ofClass(type, FieldNameFormatters.IDENTITY);
        this.converter = (DatabaseConverter<T, Map<String, Object>>) classInfo.getConverter();
        this.collectionName = Reflection.getTableName(type);
        try {
            connect(new MongoDBAuthentication(settings));
        }catch(final Exception exception) {
            throw new DatabaseException("Unable to connect to MongoDB database. " + exception.getMessage());
        }
        this.mongoCollection = database.getCollection(getCollectionName());
        IndexOptions indexOptions = new IndexOptions().unique(true);
        mongoCollection.createIndex(Indexes.text(UNIQUE_ID), indexOptions);
        super.init();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T read(final Object query) {
        final String uniqueId = query.toString();
        final Document search = new Document(MONGO_ID, uniqueId);
        final Document result = mongoCollection.find(search).limit(1).first();
        if(result == null) {
            return null;
        }
        removeMongoId(result);
        final String jsonResult = JSON.serialize(result);
        final Map<String, Object> map = (Map<String, Object>) getGson().fromJson(jsonResult, Map.class);
        final T object = converter.deserialize(map, MongoDBConversionContext.of(type));
        callMessageReceiver(object);
        return object;
    }

    @Override
    protected void write(final T object) {
        final DatabaseEntryIdentifier uniqueIdentifier = Reflection.getUniqueIdentifier(object);
        final Map<String, Object> map = converter.serialize(object, MongoDBConversionContext.of(type));
        final String serialized = getGson().toJson(map);
        final Document document = Document.parse(serialized);
        appendMongoId(document, uniqueIdentifier.toString());
        final Bson bson = new Document(MONGO_ID, uniqueIdentifier.toString());
        final FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().upsert(true);
        mongoCollection.findOneAndReplace(bson, document, options);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Collection<T> readAll(final Object query) {
        final String uniqueId = query.toString();
        final Document search = new Document(MONGO_ID, uniqueId);
        final FindIterable<Document> iterable = mongoCollection.find();
        final String delimiter = DatabaseEntryIdentifier.DELIMITER;
        Predicate<String> filterApplicator = mongoId -> mongoId.equals(uniqueId);
        if(query instanceof DatabaseEntryIdentifier) {
            filterApplicator = ((DatabaseEntryIdentifier) query).getFilterApplicator();
        }
        final Collection<T> collection = Lists.newArrayList();
        for (final Document document : iterable) {
            final String mongoId = document.getString(MONGO_ID);
            if(filterApplicator.test(mongoId)) {
                removeMongoId(document);
                final String json = JSON.serialize(document);
                final Map<String, Object> map = (Map<String, Object>) getGson().fromJson(json, Map.class);
                final T object = converter.deserialize(map, MongoDBConversionContext.of(type));
                callMessageReceiver(object);
                collection.add(object);
            }
        }
        return collection;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Collection<T> readAll() {
        final Collection<T> collection = Lists.newArrayList();
        for (final Document document : mongoCollection.find()) {
            final String json = JSON.serialize(document);
            final Map<String, Object> map = (Map<String, Object>) getGson().fromJson(json, Map.class);
            collection.add(converter.deserialize(map, MongoDBConversionContext.of(type)));
        }
        return collection;
    }

    @Override
    protected void delete(final Object query) {
        final String uniqueId = query.toString();
        final FindIterable<Document> iterable = mongoCollection.find();
        Predicate<String> filterApplicator = mongoId -> mongoId.equals(uniqueId);
        if(query instanceof DatabaseEntryIdentifier) {
            filterApplicator = ((DatabaseEntryIdentifier) query).getFilterApplicator();
        }
        for (final Document document : iterable) {
            final String mongoId = document.getString(MONGO_ID);
            if(filterApplicator.test(mongoId)) {
                mongoCollection.deleteOne(document);
            }
        }
    }

    public static <T> MongoDBDatabase<T> of(final Class<T>         type,
                                            final DatabaseSettings settings,
                                            final boolean          catchMainThread,
                                            final Thread           thread
    ) {
        return new MongoDBDatabase<>(type, settings, catchMainThread, thread);
    }

    private void appendMongoId(final Document document, final String uniqueId) {
        document.append(MONGO_ID, uniqueId);
    }

    private void removeMongoId(final Document document) {
        document.remove(MONGO_ID);
    }

    @Override
    protected void connect(final @NonNull DatabaseAuth auth) {
        final MongoDBAuthentication mongoAuth = (MongoDBAuthentication) auth;
        if(client == null) {
            boolean blankUri = mongoAuth.getConnectionUri().isBlank();
            if(!blankUri) {
                this.client = new MongoClient(mongoAuth.getConnectionUri());
            }else {
                final MongoCredential credentials = MongoCredential.createCredential(
                        mongoAuth.getUsername(),
                        mongoAuth.getDatabase(),
                        mongoAuth.getPassword().toCharArray()
                );

                final MongoClientOptions options = MongoClientOptions.builder().sslEnabled(mongoAuth.isUseSSL()).build();
                final ServerAddress address = new ServerAddress(mongoAuth.getHost(), mongoAuth.getPort());
                this.client = new MongoClient(address, credentials, options);
            }
            this.database = client.getDatabase(mongoAuth.getDatabase());
        }
    }

    @Override
    protected ClassInfo getClassInfo() {
        return classInfo;
    }

    @Override
    protected int fetchMaxId() {
        final Field autoIncrementField = Reflection.getAutoIncrementField(type, classInfo);
        if(autoIncrementField != null) {
            final ClassInfo.FieldInfo fieldInfo = classInfo.getInfo(autoIncrementField);
            final FindIterable<Document> iterable = mongoCollection.find();
            int maxId = 0;
            final String fieldName = fieldInfo.getFormattedName();
            for (final Document document : iterable) {
                final String json = JSON.serialize(document);
                final Map<String, Object> map = (Map<String, Object>) getGson().fromJson(json, Map.class);
                final int id = (Integer) map.get(fieldName);
                if(id > maxId) {
                    maxId = id;
                }
            }
            return maxId;
        }
        return 0;
    }

    @Override
    public boolean isConnected() {
        return client != null;
    }

    public String getCollectionName() {
        return collectionName;
    }
}
