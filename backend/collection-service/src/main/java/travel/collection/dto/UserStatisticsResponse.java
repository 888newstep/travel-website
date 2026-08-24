package travel.collection.dto;

public record UserStatisticsResponse(
        int totalNotes,
        int totalCollections,
        int totalShares) {
}
