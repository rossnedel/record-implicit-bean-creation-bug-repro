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
