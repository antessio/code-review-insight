package antessio.gitlab;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AbstractUser;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.MergeRequestFilter;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;

public class Gitlab {


    private final GitLabApi gitLabApi;

    public Gitlab(String authToken, String gitlabHost) {
        this.gitLabApi = new GitLabApi(gitlabHost, authToken);
    }

    public List<User> getApprovals(long projectId, long mrIid) {

        try {
            return gitLabApi.getMergeRequestApi().getApprovals(projectId, mrIid).getApprovedBy();
        } catch (GitLabApiException e) {
            throw new RuntimeException(e);
        }

    }
    public Stream<MergeRequest> getMergedMergeRequestsStream(Instant from, Instant to, Long userId) {

        try {

            return gitLabApi.getMergeRequestApi()
                    .getMergeRequests(new MergeRequestFilter()
                                              .withAuthorId(userId)
                                              .withState(Constants.MergeRequestState.MERGED)
                                              .withCreatedAfter(Date.from(from))
                                              .withCreatedBefore(Date.from(to))
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

    public Project getProject(Long projectId) {
        try {
            return gitLabApi.getProjectApi().getProject(projectId);
        } catch (GitLabApiException e) {
            throw new RuntimeException(e);
        }
    }

}
