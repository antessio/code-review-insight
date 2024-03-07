package antessio;

import java.time.Instant;
import java.util.List;

public interface CodeReviewDataExporter {

    List<MergeRequest> getMergeRequests();

    record Approval(String username) {

    }

    record Comment(String author, String body, Instant createdAt) {

    }

    record MergeRequest(Long id,
                               String title,
                               String webUrl,
                               String author,
                               Instant createdAt,
                               Instant mergedAt,
                               Integer changes,
                               List<Approval> approvals,
                               List<Comment> comments) {

    }

}
