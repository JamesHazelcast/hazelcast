This directory contains resources for testing user code deployment use cases.

Contents:

- `ChildClass.class`, `ParentClass.class`: hierarchy of classes where `ChildClass extends ParentClass`.
- `IncrementingExternalEntryProcessor.class`: an `EntryProcessor` for `<Integer, Integer>` entries that increments value by 1 - renamed to avoid `IncrementingEntryProcessor` already on the classpath.
- `IncrementingJavaxEntryProcessor.class`: a `javax.cache.processor.EntryProcessor<K, V, T>` for `<Integer, Integer>` entries that increments value by 1.
- `IncrementingValueExtractor`: a `ValueExtractor` that increments value by 1.
- `IncrementingMapInterceptor.class`: an `MapInterceptor` that increments value by 1.
- `ShadedClasses.jar`: contains a class `com.hazelcast.core.HazelcastInstance` that defines a `main` method.
- `IncrementingEntryProcessor.jar`: contains `IncrementingEntryProcessor` class.
- `ChildParent.jar`: contains `ChildClass` and `ParentClass` as described above.
- `EntryProcessorWithAnonymousAndInner.jar`: contains class `EntryProcessorWithAnonymousAndInner`, to exercise loading classes with anonymous and named inner classes.
- `LowerCaseValueEntryProcessor`, `UpperCaseValueEntryProcessor`: `EntryProcessor` who adjust the case of the value, with deliberately overlapping class names.
- `AcceptAllIFunction`: a simple `IFunction` that always returns `true`.
- `DerbyUpperCaseStringMapLoader`: connects to an embedded Derby database instance and returns the result of an upper-casing SQL query.
- `H2WitHDataSourceBuilderVerionMapLoader`, `H2WithDriverManagerBuildVersionMapLoader`: connects to an embedded H2 database and returns the results of an SQL query for the databases' version.
- `IdentityProjection`: a `Projection` that returns the input.

Note: unless package is explicitly specified, all classes described above reside in package `usercodedeployment`.

To generate a new `.class` from from an existing `.java` file, run something like:

```shell
GROUP_ID=com.hazelcast
ARTIFACT_ID=hazelcast
VERSION=5.3.2

mvn dependency:get -DgroupId=$GROUP_ID -DartifactId=$ARTIFACT_ID -Dversion=$VERSION --quiet;
javac --release 11 *.java -cp "$HOME/.m2/repository/${GROUP_ID//.//}/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION.jar";
```