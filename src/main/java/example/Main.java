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
