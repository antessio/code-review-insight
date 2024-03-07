package antessio.gitlab;

import static antessio.common.FileUtils.writeToFile;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import antessio.CodeReviewDataExporter;
import antessio.common.JsonConverter;
import antessio.common.ObjectMapperJsonConverter;

public class GitLabExporter implements CodeReviewDataExporter {

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
        Instant createdAfter = clock.instant().minus(durationInDays, ChronoUnit.DAYS);

        this.team.stream()
                 .map(gitlab::getAuthorId)
                 .filter(Optional::isPresent)
                 .map(Optional::get)
                 .flatMap(authorId -> gitlab.getMergedMergeRequestsStream(createdAfter, authorId))
                 .limit(size)
                 .forEach(mr -> {
                     List<Comment> comments = new ArrayList<>();
                     List<Approval> approvals = new ArrayList<>();
                     gitlab.getComments(mr)
                           .flatMap(comment -> comment.getNotes().stream())
                           .forEach(note -> comments.add(new Comment(note.getAuthor().getName(), note.getBody(), note.getCreatedAt().toInstant())));

                     gitlab.getApprovals(mr.getProjectId(), mr.getIid())
                           .getApprovedBy()
                           .forEach(approvedBy -> approvals.add(new Approval(approvedBy.getUser().getUsername())));

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
        writeToFile(backupFile, jsonConverter.toJson(this.getMergeRequests()));
        initialized = true;
    }





    @Override
    public List<MergeRequest> getMergeRequests() {
        if (!initialized){
            init();
        }
        return mergeRequests;
    }



}
