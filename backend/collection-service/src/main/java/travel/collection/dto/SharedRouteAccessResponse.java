package travel.collection.dto;

public record SharedRouteAccessResponse(
        Integer routeId,
        String shareTitle,
        String shareDescription) {
}
