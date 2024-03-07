package antessio.gitlab;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Approvals {

    private final List<ApprovedBy> approvedBy;

    @JsonCreator
    public Approvals(@JsonProperty("approved_by") List<ApprovedBy> approvedBy) {
        this.approvedBy = approvedBy;
    }

    public static class ApprovedBy {

        private final User user;

        @JsonCreator
        public ApprovedBy(@JsonProperty("user") User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }

        public static class User {
            private final long id;
            private final String name;
            private final String webUrl;

            private final String username;

            @JsonCreator
            public User(@JsonProperty("id") long id,
                        @JsonProperty("name") String name,
                        @JsonProperty("username") String username,
                        @JsonProperty("web_url") String webUrl) {
                this.id = id;
                this.name = name;
                this.username = username;
                this.webUrl = webUrl;
            }

            public long getId() {
                return id;
            }

            public String getName() {
                return name;
            }

            public String getWebUrl() {
                return webUrl;
            }

            public String getUsername() {
                return username;
            }

        }

    }

    public List<ApprovedBy> getApprovedBy() {
        return approvedBy;
    }
}

