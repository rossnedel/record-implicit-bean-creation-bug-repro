### Expected Behavior

A record that is not declared a bean does not become one, just like the equivalent class. javac copies a record component's annotations onto the record's private final field wherever the annotation's `@Target` allows, but Micronaut injects a record through its canonical constructor, so those copies should not count as field injection points.

### Actual Behaviour

A record whose component carries `@Property` gets a bean definition of its own, `$GreetingRecord$Definition`. The equivalent class, whose constructor parameter carries the same annotation, gets none. When the type is also produced elsewhere, here by a `@Factory`, the record has two candidates and `getBean` throws `NonUniqueBeanException`, while the class has one and resolves.

`BeanDefinitionCreatorFactory.containsInjectField` counts every declared field with an `@Inject`, `@Value` or `@Property` stereotype, final ones included:

```java
private static boolean containsInjectField(ClassElement classElement) {
    return classElement.getEnclosedElement(
        ElementQuery.ALL_FIELDS
            .onlyDeclared()
            .annotated(BeanDefinitionCreatorFactory::containsInjectPoint)
    ).isPresent();
}
```

`DeclaredBeanElementCreator.visitField` never injects a final field, and on 5.3.x (#13998) it exempts records from the new final-field error because "Records and Kotlin constructor properties copy the parameter annotations to the final backing field". `containsInjectField` has no such exemption, so the copied annotation still makes the record a bean. The same exemption there would make the record behave like the class.

The record has to be processed at all: a component annotation the processor handles, such as `@Property` itself or a qualifier like `@Named`, is enough. We hit it with a record combining `@Named` on one component and, on another, our own annotation declared `@AliasFor(annotation = Property.class, member = "name")`. That alias alone did not trigger it.

### Steps To Reproduce

1. `build.gradle`:

```groovy
plugins {
    id 'java'
    id 'application'
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor 'io.micronaut:micronaut-inject-java:5.2.21'
    implementation 'io.micronaut:micronaut-inject:5.2.21'
    runtimeOnly 'org.slf4j:slf4j-nop:2.0.17'
}

application {
    mainClass = 'example.Main'
}
```

2. The sources, in `src/main/java/example/`:

```java
// Greeter.java
package example;

import jakarta.inject.Singleton;

@Singleton
public class Greeter {
}

// GreetingRecord.java
package example;

import io.micronaut.context.annotation.Property;

/** Not declared a bean: GreetingFactory produces it. */
public record GreetingRecord(Greeter greeter, @Property(name = "greeting") String greeting) {
}

// GreetingClass.java
package example;

import io.micronaut.context.annotation.Property;

/** The same type as a class. Not declared a bean: GreetingFactory produces it. */
public class GreetingClass {

    private final Greeter greeter;
    private final String greeting;

    public GreetingClass(Greeter greeter, @Property(name = "greeting") String greeting) {
        this.greeter = greeter;
        this.greeting = greeting;
    }
}

// GreetingFactory.java
package example;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class GreetingFactory {

    @Singleton
    GreetingRecord greetingRecord(Greeter greeter) {
        return new GreetingRecord(greeter, "from the factory");
    }

    @Singleton
    GreetingClass greetingClass(Greeter greeter) {
        return new GreetingClass(greeter, "from the factory");
    }
}

// Main.java
package example;

import io.micronaut.context.ApplicationContext;

import java.util.Map;

public class Main {

    public static void main(String[] args) {
        try (ApplicationContext context = ApplicationContext.run(Map.of("greeting", "hello"))) {
            for (Class<?> type : new Class<?>[]{GreetingClass.class, GreetingRecord.class}) {
                System.out.println(type.getSimpleName() + ": " + context.getBeanDefinitions(type).size() + " bean definition(s)");
                try {
                    context.getBean(type);
                    System.out.println("  getBean: OK");
                } catch (RuntimeException e) {
                    System.out.println("  getBean: " + e);
                }
            }
        }
    }
}
```

3. `./gradlew run` prints:

```
GreetingClass: 1 bean definition(s)
  getBean: OK
GreetingRecord: 2 bean definition(s)
  getBean: io.micronaut.context.exceptions.NonUniqueBeanException: Multiple possible bean candidates found: [GreetingRecord, GreetingRecord]
```

`build/classes/java/main/example/` holds `$GreetingRecord$Definition.class` and no `$GreetingClass$Definition.class`.

### Environment Information

- Operating System: macOS 26 (Darwin 25.6.0), aarch64
- JDK Version: Amazon Corretto 25.0.4.1

### Example Application

The sources above are the whole application.

### Version

5.2.21, and 5.2.20. `containsInjectField` is unchanged on the 5.3.x branch.
