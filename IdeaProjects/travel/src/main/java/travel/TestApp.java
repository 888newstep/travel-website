package travel;

public class TestApp {
    public static void main(String[] args) {
        System.out.println("TestApp started");
        try {
            Class<?> springApplicationClass = Class.forName("org.springframework.boot.SpringApplication");
            System.out.println("SpringApplication class found: " + springApplicationClass);
        } catch (ClassNotFoundException e) {
            System.out.println("SpringApplication class not found: " + e.getMessage());
        }
        System.out.println("TestApp finished");
    }
}