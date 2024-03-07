package antessio.gitlab;

import static antessio.common.FileUtils.writeToFile;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
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
    private final JsonConverter jsonConverter;
    private final String backupFile;
    private Clock clock;
    private final List<MergeRequest> mergeRequests;
    private boolean initialized = false;

    public GitLabExporter(
            Gitlab gitlab,
            List<String> team,
            int durationInDays,
            int size,
            String backupFile) {
        this.gitlab = gitlab;
        this.team = team;
        this.durationInDays = durationInDays;
        this.size = size;
        this.clock = Clock.systemUTC();
        this.backupFile = backupFile;
        this.mergeRequests = new ArrayList<>();
        this.jsonConverter = new ObjectMapperJsonConverter();
    }

    public void init() {
        LOGGER.debug("initialization started at {} ", clock.instant());
        Instant createdAfter = clock.instant().minus(durationInDays, ChronoUnit.DAYS);

        this.team.stream()
                 .map(gitlab::getAuthorId)
                 .filter(Optional::isPresent)
                 .map(Optional::get)
                 .flatMap(authorId -> gitlab.getMergedMergeRequestsStream(createdAfter, authorId))
                 .limit(size)
                 .forEach(mr -> {
                     LOGGER.debug("processing mr {}", mr.getWebUrl());
                     List<Comment> comments = new ArrayList<>();
                     List<Approval> approvals = new ArrayList<>();
                     gitlab.getComments(mr)
                           .flatMap(comment -> comment.getNotes().stream())
                           .forEach(note -> comments.add(new Comment(note.getAuthor().getName(), note.getBody(), note.getCreatedAt().toInstant())));

                     gitlab.getApprovals(mr.getProjectId(), mr.getIid())
                           .forEach(approvedBy -> approvals.add(new Approval(approvedBy.getUsername())));

                     mergeRequests.add(new MergeRequest(
                             mr.getIid(),
                             mr.getTitle(),
                             mr.getWebUrl(),
                             mr.getAuthor().getUsername(),
                             mr.getCreatedAt().toInstant(),
                             Optional.ofNullable(mr.getMergedAt()).map(Date::toInstant).orElse(null),
                             mr.getChanges().size(),
                             approvals,
                             comments
                     ));

                 });
        // backup
        LOGGER.debug("storing backup to {} ", backupFile);
        initialized = true;
        writeToFile(backupFile, jsonConverter.toJson(mergeRequests));
        LOGGER.debug("initialization finished at {} ", clock.instant());
    }





    @Override
    public List<MergeRequest> getMergeRequests() {
        if (!initialized){
            init();
        }
        return mergeRequests;
    }



}
