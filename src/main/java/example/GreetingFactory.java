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
