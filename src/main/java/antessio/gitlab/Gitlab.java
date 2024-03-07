package antessio.gitlab;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AbstractUser;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.MergeRequestFilter;

import antessio.common.JsonConverter;
import antessio.common.ObjectMapperJsonConverter;

public class Gitlab {
    public static final String GITLAB_HOST = "https://git.treatwell.net";
    public static final String BASE_URL = GITLAB_HOST + "/api/v4";

    private final String authToken;
    private final GitLabApi gitLabApi;
    private final HttpClient http;
    private final JsonConverter jackson;

    public Gitlab(String authToken, String gitlabHost) {
        this.gitLabApi = new GitLabApi(gitlabHost, authToken);
        this.authToken = authToken;
        this.http = HttpClient.newHttpClient();
        this.jackson = new ObjectMapperJsonConverter();
    }

    private static String toString(HttpResponse<InputStream> response) throws IOException {
        return new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
    }

    public Approvals getApprovals(long projectId, long mrIid) {
        String apiUrl = BASE_URL + "/projects/" + projectId + "/merge_requests/" + mrIid + "/approvals";
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(apiUrl))
                                         .header("Private-Token", authToken)
                                         .build();

        try {
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                return jackson.fromJson(toString(response), Approvals.class);
            } else {
                System.out.println("Failed to retrieve approvals information for mr IID: " + mrIid);
                System.out.println("Response code: " + response.statusCode());
                System.out.println("Response body: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    public Stream<MergeRequest> getMergedMergeRequestsStream(Instant from, Long userId) {

        try {

            return gitLabApi.getMergeRequestApi()
                    .getMergeRequests(new MergeRequestFilter()
                                              .withAuthorId(userId)
                                              .withState(Constants.MergeRequestState.MERGED)
                                              .withCreatedAfter(Date.from(from))
                                              .withOrderBy(Constants.MergeRequestOrderBy.CREATED_AT)
                                              .withSort(Constants.SortOrder.DESC)
                                              .withScope(Constants.MergeRequestScope.ALL),
                                      20)
                    .stream()
                    .map(mr -> {
                        try {
                            return gitLabApi.getMergeRequestApi().getMergeRequestChanges(mr.getProjectId(), mr.getIid());
                        } catch (GitLabApiException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (GitLabApiException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Long> getAuthorId(String username)  {
        try {
            return Optional.ofNullable(gitLabApi.getUserApi()
                                 .getUser(username))
                    .map(AbstractUser::getId);

        } catch (GitLabApiException e) {
            throw new RuntimeException(e);
        }
    }

    public Stream<Discussion> getComments(MergeRequest mr) {
        try {
            return gitLabApi.getDiscussionsApi().getMergeRequestDiscussionsPager(
                    mr.getProjectId(), mr.getIid(),
                    20
            ).stream();
        } catch (GitLabApiException e) {
            throw new RuntimeException(e);
        }
    }

}
