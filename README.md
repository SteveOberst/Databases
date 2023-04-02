# Databases
A framework simplifying and abstracting database access.

# Getting started
Using this framework is as simple as it gets. Simply create a class implementing DatabaseSettings, pass it to and instantiate a database by using Database#ofType and use the provided methods to interact with the database.
```java

@Data
public class MyDatabaseSettings implements DatabaseSettings {
    private DatabaseTypes databaseType = DatabaseTypes.MySQL;
    private String username = "username";
    private String password = "password";
    private String database = "Database";
    private String host = "localhost";
    private String databasePath = "";
    private String connectionUri = "";
    private boolean useSsl = false;
    private int port = 3306;
}

public class MyDatabaseHandler {
  private final Database<User> database;

  public MyDatabaseHandler(final DatabaseSettings settings) {
    this.database = Database.ofType(User.class, settings);
  }
}
```

## But how exactly does the User class end up in the database?
That is pretty much up to you, the framework gives you a lot of control about the data model User ends up representing. For example, the User class could look like this:
```java
@Data
public class User {
  @UniqueIdentifier
  private long uuid;
  
  @AutoIncrement
  private int id;
  
  private String name;
  private int age;
  private double weight;
  private String biography
  
  public User(final long uuid, final String name, final int age, final double weight, final String biography) {
    this.uuid = uuid;
    this.name = name;
    this.age = age;
    this.weight = weight;
    this.biography = biography;
  }
}
```

As you can see, here we used the @UniqueIdentifier annotation to mark a field as unique identifier for this data set. We can also use @AutoIncrement to automatically 
assign a new ID for each new user registered in the database.

### How does the framework choose the right data type for each field that will be stored in my MySQL database?
The framework automatically maps java types to their respective MySQL type. However, these might not always be accurate
or the ones you will end up wanting to use so you can tell the framework which data type it should use to save a certain field.
```java
@Data
@TableName("FooBar")
@TableQuery("CREATE TABLE IF NOT EXISTS test (bar VARCHAR(4))")
public class Foo {
  @DataType("VARCHAR(4)")
  final Bar bar;
  
  public Foo(final Bar bar) {
    this.bar = bar;
  }
}
```
The @DataType annotation can be used to map java types to a custom MySQL type.

Note that we used the @TableQuery annotation here which is a query that will be run for MySQL databases on their creation. @TableName can be used universally 
for all default implementations to set a custom name for the collection data entries will be saved to.

## Okay, but now that I know all this, how do I actually read and write things to and from the database?
Database read/write is pretty straight forward using the methods provided by the Database<T> interface:
```java

public class MyDatabaseHandler {
  private final Database<User> database;

  public MyDatabaseHandler(final DatabaseSettings settings) {
    this.database = Database.ofType(User.class, settings);
  }
  
  public User createUser(final long uuid) {
    final User user = new User(uuid, "Steve", 18, 75.5d, "Passionate Programmer!")
    database.save(user);
    return user;
  }
  
  public void deleteUser(final long uuid) {
    database.remove(uuid);
  }
  
  public User getUser(final long uuid) {
    return database.get(uuid);
  }
  
  public void getUserAsync(final long uuid, final Callback<User> callback) {
    CompletableFuture.runAsync(() -> {
      callback.handle(getUser(uuid));
    });
  }
  
  public Collection<User> getAllUsers() {
    return database.getAll();
  }
  
  public void getAllUsersAsync(final Callback<Collection<User>> callback) {
    CompletableFuture.runAsync(() -> {
      callback.handle(getAll());
    });
  }

  // this can be particularly useful if there are multiple unique ids or we're using mysql and want to run a custom query
  public Collection<User> getAllUsersMatching(final Object query) {
    return database.getAll(query);
  }
}
```

## We can also mark fields that should not be serialized within the objects
Fields that should not be serialized are to be marked with the @Ignore annotation.
```java

@Data
@TableName("FooBar")
public class Foo {
    @UniqueIdentifier
    final FooBar fooBar;
    
    @Ignore
    final Bar bar;
    
    public Foo(final FooBar fooBar, final Bar bar) {
        this.fooBar = fooBar;
        this.bar = bar;
    }
}
```
when saving this object to the database, only FooBar will be included in the dataset.


# Challenges faced during this project
It really was tough to find a way to abstract Databases like MongoDB, Flat file storage (JSON) and MySQL as they are so fundamentally different. However, the
framework offers a variaty of ways to deal with their differences, mostly by using annotations.

# License
```
Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the “Software”), to deal
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
```
