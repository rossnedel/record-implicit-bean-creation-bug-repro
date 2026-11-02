package example;

import io.micronaut.context.annotation.Property;

/** Not declared a bean: GreetingFactory produces it. */
public record GreetingRecord(Greeter greeter, @Property(name = "greeting") String greeting) {
}
