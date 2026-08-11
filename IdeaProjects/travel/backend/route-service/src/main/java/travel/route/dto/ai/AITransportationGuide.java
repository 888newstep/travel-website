package travel.route.dto.ai;

/**
 * 旅游攻略中的交通信息。
 */
public class AITransportationGuide {

    private String airport;
    private String train;
    private String localTransport;
    private String tips;

    public AITransportationGuide() {
    }

    public AITransportationGuide(String airport, String train, String localTransport, String tips) {
        this.airport = airport;
        this.train = train;
        this.localTransport = localTransport;
        this.tips = tips;
    }

    public String getAirport() {
        return airport;
    }

    public void setAirport(String airport) {
        this.airport = airport;
    }

    public String getTrain() {
        return train;
    }

    public void setTrain(String train) {
        this.train = train;
    }

    public String getLocalTransport() {
        return localTransport;
    }

    public void setLocalTransport(String localTransport) {
        this.localTransport = localTransport;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String airport;
        private String train;
        private String localTransport;
        private String tips;

        public Builder airport(String airport) {
            this.airport = airport;
            return this;
        }

        public Builder train(String train) {
            this.train = train;
            return this;
        }

        public Builder localTransport(String localTransport) {
            this.localTransport = localTransport;
            return this;
        }

        public Builder tips(String tips) {
            this.tips = tips;
            return this;
        }

        public AITransportationGuide build() {
            return new AITransportationGuide(airport, train, localTransport, tips);
        }
    }
}
