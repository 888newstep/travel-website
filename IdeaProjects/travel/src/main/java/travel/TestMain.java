package travel;

public class TestMain {
    public static void main(String[] args) {
        System.out.println("TestMain started");
        try {
            // 尝试加载TravelApplication类
            Class<?> travelApplicationClass = Class.forName("travel.TravelApplication");
            System.out.println("TravelApplication class loaded successfully");
            
            // 尝试调用main方法
            travelApplicationClass.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("TestMain finished");
    }
}