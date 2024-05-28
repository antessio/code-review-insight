package antessio.gitlab;

import static antessio.common.FileUtils.writeToFile;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antessio.CodeReviewDataExporter;
import antessio.common.JsonConverter;
import antessio.common.ObjectMapperJsonConverter;

public class GitLabExporter implements CodeReviewDataExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitLabExporter.class);

    private final Gitlab gitlab;
    private final List<String> team;
    private final int durationInDays;
    private final int size;
    private final List<String> blackListProjectsIds;
    private final JsonConverter jsonConverter;
    private final String backupFile;
    private Clock clock;
    private final List<MergeRequest> mergeRequests;
    private boolean initialized = false;

    private final Instant from;
    private final Instant to;

    public GitLabExporter(
            Gitlab gitlab,
            List<String> team,
            int durationInDays,
            int size,
            List<String> blackListProjectsId,
            String backupFile,
            Clock clock) {
        this.gitlab = gitlab;
        this.team = team;
        this.durationInDays = durationInDays;
        this.size = size;
        this.blackListProjectsIds = blackListProjectsId;
        this.clock = clock;
        this.backupFile = backupFile;
        this.mergeRequests = new ArrayList<>();
        this.jsonConverter = new ObjectMapperJsonConverter();
        this.to = this.clock.instant();
        this.from = to.minus(durationInDays, ChronoUnit.DAYS);
    }
    public GitLabExporter(
            Gitlab gitlab,
            List<String> team,
            int size,
            List<String> blackListProjectsId,
            String backupFile,
            Instant from,
            Instant to,
            Clock clock) {
        this.gitlab = gitlab;
        this.team = team;
        this.durationInDays = Math.toIntExact(Duration.between(from, to).toDays());
        this.size = size;
        this.blackListProjectsIds = blackListProjectsId;
        this.clock = clock;
        this.backupFile = backupFile;
        this.mergeRequests = new ArrayList<>();
        this.jsonConverter = new ObjectMapperJsonConverter();
        this.from = from;
        this.to = to;
    }

    public void init() {
        Instant now = clock.instant();
        LOGGER.debug("initialization started at {} ", now);
        java.util.Map<Long, org.gitlab4j.api.models.Project> projectMap = new HashMap<>();
        this.team.stream()
                 .map(gitlab::getAuthorId)
                 .filter(Optional::isPresent)
                 .map(Optional::get)
                 .flatMap(authorId -> gitlab.getMergedMergeRequestsStream(from, to, authorId))
                 .filter(mr -> !blackListProjectsIds.contains(mr.getProjectId().toString()))
                 .limit(size)
                 .forEach(mr -> {
                     LOGGER.debug("processing mr {}", mr.getWebUrl());
                     List<Comment> comments = new ArrayList<>();
                     List<Approval> approvals = new ArrayList<>();
                     gitlab.getComments(mr)
                           .flatMap(comment -> comment.getNotes().stream())
                             .filter(comment -> team.contains(comment.getAuthor().getUsername()))
                             .filter(comment -> !comment.getSystem())
                           .forEach(note -> comments.add(new Comment(note.getAuthor().getUsername(), note.getBody(), note.getCreatedAt().toInstant())));

                     gitlab.getApprovals(mr.getProjectId(), mr.getIid())
                           .forEach(approvedBy -> approvals.add(new Approval(approvedBy.getUsername())));

                     Project project = Optional.ofNullable(projectMap.get(mr.getProjectId()))
                                               .map(GitLabExporter::convertFromGitlabProject)
                                               .orElseGet(() -> {
                                                   org.gitlab4j.api.models.Project gitlabProject = gitlab.getProject(mr.getProjectId());
                                                   projectMap.put(mr.getProjectId(), gitlabProject);
                                                   return convertFromGitlabProject(gitlabProject);
                                               });
                     mergeRequests.add(new MergeRequest(
                             mr.getIid().toString(),
                             mr.getTitle(),
                             mr.getWebUrl(),
                             mr.getAuthor().getUsername(),
                             mr.getCreatedAt().toInstant(),
                             Optional.ofNullable(mr.getMergedAt()).map(Date::toInstant).orElse(null),
                             mr.getChanges().size(),
                             approvals,
                             comments,
                             project
                     ));

                 });
        // backup
        LOGGER.debug("storing backup to {} ", backupFile);
        initialized = true;
        writeToFile(backupFile, jsonConverter.toJson(mergeRequests));
        LOGGER.debug("initialization finished at {} ", now);
    }

    private static Project convertFromGitlabProject(org.gitlab4j.api.models.Project p) {
        return new Project(p.getId().toString(), p.getName(), p.getWebUrl());
    }


    @Override
    public List<MergeRequest> getMergeRequests() {
        if (!initialized){
            init();
        }
        return mergeRequests;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

}
