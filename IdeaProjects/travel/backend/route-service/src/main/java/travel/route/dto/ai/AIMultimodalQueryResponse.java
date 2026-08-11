package travel.route.dto.ai;

public class AIMultimodalQueryResponse {

    private String response;

    private String queryType;

    private String source;

    public AIMultimodalQueryResponse() {
    }

    public AIMultimodalQueryResponse(String response, String queryType, String source) {
        this.response = response;
        this.queryType = queryType;
        this.source = source;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String response;
        private String queryType;
        private String source;

        public Builder response(String response) {
            this.response = response;
            return this;
        }

        public Builder queryType(String queryType) {
            this.queryType = queryType;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public AIMultimodalQueryResponse build() {
            return new AIMultimodalQueryResponse(response, queryType, source);
        }
    }

}