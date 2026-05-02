package travel;

import travel.mapper.route_planning_mapper.RouteAttractionMapper;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;

@Configuration
@ComponentScan(
    basePackages = "travel",
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Component.class)
)
public class TestMapperBean {
    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestMapperBean.class)) {
            RouteAttractionMapper mapper = context.getBean(RouteAttractionMapper.class);
            System.out.println("Successfully obtained RouteAttractionMapper: " + mapper);
        } catch (Exception e) {
            System.out.println("Error obtaining RouteAttractionMapper: " + e.getMessage());
            e.printStackTrace();
        }
    }
}