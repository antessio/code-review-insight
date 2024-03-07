package antessio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import antessio.common.DateUtils;
import antessio.gitlab.GitLabExporter;

public class CodeReviewInsightService{

    private final List<Contributor> topContributors = new ArrayList<>();
    private final List<Commenter> topCommenters = new ArrayList<>();
    private final List<Approver> topApprovers = new ArrayList<>();
    private final List<HotMr> hottestMrs = new ArrayList<>();
    private final List<LongMr> longestMr = new ArrayList<>();
    private final List<BigMr> biggestMrs = new ArrayList<>();
    private final AtomicInteger averageMergeRequestsDurationInHours = new AtomicInteger(0);
    private final AtomicInteger averageTimeToFirstCommentInHours = new AtomicInteger(0);
    private final AtomicReference<Double> averageCountOfNitComments = new AtomicReference<Double>(0d);
    


    public CodeReviewInsightService(CodeReviewDataExporter codeReviewDataExporter) {
        init(codeReviewDataExporter.getMergeRequests());
    }

    private void init(List<GitLabExporter.MergeRequest> mergeRequests) {
        Map<String, List<GitLabExporter.MergeRequest>> approvedMRByUser = new HashMap<>();
        Map<String, Map<GitLabExporter.MergeRequest, List<GitLabExporter.Comment>>> discussionsOnMRByUser = new HashMap<>();
        Map<GitLabExporter.MergeRequest, Integer> firstCommentByMergeRequest = new HashMap<>();
        Map<GitLabExporter.MergeRequest, Integer> nitByMergeRequest = new HashMap<>();
        Map<GitLabExporter.MergeRequest, Integer> durationInHoursByMergeRequest = new HashMap<>();

        mergeRequests
                .forEach(mr -> {
                    // process comments
                    AtomicInteger commentCount = new AtomicInteger();
                    AtomicReference<Instant> firstComment = new AtomicReference<>(null);
                    nitByMergeRequest.put(mr, 0);
                    mr
                            .comments()
                            .forEach(n -> {
                                commentCount.addAndGet(1);
                                String commentAuthor = n.author();
                                if (!discussionsOnMRByUser.containsKey(commentAuthor)) {
                                    Map<GitLabExporter.MergeRequest, List<GitLabExporter.Comment>> commentsOnMr = new HashMap<>();
                                    commentsOnMr.put(mr, new ArrayList<>());
                                    discussionsOnMRByUser.put(commentAuthor, commentsOnMr);
                                } else if (!discussionsOnMRByUser.get(commentAuthor).containsKey(mr)) {
                                    discussionsOnMRByUser.get(commentAuthor)
                                                         .put(mr, new ArrayList<>());
                                }

                                discussionsOnMRByUser
                                        .get(commentAuthor)
                                        .get(mr)
                                        .add(n);

                                Instant commentDate = n.createdAt();
                                if (firstComment.get() == null || firstComment.get().isAfter(commentDate)) {
                                    firstComment.set(commentDate);
                                }
                                if (n.body().contains("NIT") || n.body().contains("nit")) {
                                    int newCount = nitByMergeRequest.get(mr) + 1;
                                    nitByMergeRequest.put(mr, newCount);
                                }


                            });
                    int firstCommentDuration = DateUtils.timeDiff(Date.from(firstComment.get()), Date.from(mr.createdAt()), TimeUnit.MINUTES);
                    firstCommentByMergeRequest.put(mr, firstCommentDuration);
                    // process mr data
                    hottestMrs.add(new HotMr(mr.id().toString(), mr.title(), mr.author(), commentCount.get(), mr.webUrl()));
                    biggestMrs.add(new BigMr(mr.id().toString(), mr.title(), mr.author(), mr.changes(), mr.webUrl()));
                    Optional.ofNullable(mr.mergedAt())
                            .ifPresent(mergedAt -> {
                                int mrDuration = DateUtils.timeDiff(Date.from(mergedAt), Date.from(mr.createdAt()), TimeUnit.MINUTES);
                                longestMr.add(new LongMr(mr.id().toString(), mr.title(), mr.author(), mrDuration, mr.webUrl()));
                                durationInHoursByMergeRequest.put(mr, mrDuration);
                            });
                    updateApprovalsIfApproved(mr, approvedMRByUser);

                });
        averageMergeRequestsDurationInHours.set(durationInHoursByMergeRequest.values().stream().reduce(0, Integer::sum) / durationInHoursByMergeRequest.size());
        averageCountOfNitComments.set(((double)nitByMergeRequest.values().stream().reduce(0, Integer::sum)) / ((double)nitByMergeRequest.size()));
        averageTimeToFirstCommentInHours.set(firstCommentByMergeRequest.values().stream().reduce(0, Integer::sum) / firstCommentByMergeRequest.size());
        calculateTopContributors(approvedMRByUser, discussionsOnMRByUser);
        calculateTopApprovers(approvedMRByUser);
        calculateTopCommenters(discussionsOnMRByUser);
    }


    private void calculateTopCommenters(Map<String, Map<GitLabExporter.MergeRequest, List<GitLabExporter.Comment>>> discussionsOnMRByUser) {
        discussionsOnMRByUser
                .entrySet()
                .stream()
                .flatMap(e -> e.getValue()
                               .entrySet()
                               .stream()
                               .flatMap(e2 -> e2.getValue().stream())
                               .map(GitLabExporter.Comment::author))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .forEach((key, value) -> topCommenters.add(new Commenter(key, Math.toIntExact(value))));
    }

    private void calculateTopApprovers(Map<String, List<GitLabExporter.MergeRequest>> approvedMRByUser) {
        approvedMRByUser
                .forEach((key, value) -> topApprovers.add(new Approver(key, value.size())));
    }

    private void calculateTopContributors(
            Map<String, List<GitLabExporter.MergeRequest>> approvedMRByUser,
            Map<String, Map<GitLabExporter.MergeRequest, List<GitLabExporter.Comment>>> discussionsOnMRByUser) {
        Stream.concat(
                      approvedMRByUser.keySet().stream(),
                      discussionsOnMRByUser.entrySet()
                                           .stream()
                                           .flatMap(e -> e.getValue()
                                                          .entrySet()
                                                          .stream()
                                                          .flatMap(e2 -> e2.getValue()
                                                                           .stream())
                                                          .map(GitLabExporter.Comment::author)))
              .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
              .forEach((key, value) -> topContributors.add(new Contributor(key, value)));
    }

    private void updateApprovalsIfApproved(GitLabExporter.MergeRequest mr, Map<String, List<GitLabExporter.MergeRequest>> approvedMRByUser) {
        mr.approvals()
          .stream()
          .map(GitLabExporter.Approval::username)
          .forEach(approvalUsername -> {
              List<GitLabExporter.MergeRequest> mergeRequestsApproved = Optional.ofNullable(approvedMRByUser.get(approvalUsername))
                                                                                .orElseGet(ArrayList::new);
              mergeRequestsApproved.add(mr);
              approvedMRByUser.put(approvalUsername, mergeRequestsApproved);
          });

    }

    public List<Contributor> getTopContributors() {
        return topContributors;
    }

    public List<Commenter> getTopCommenters() {
        return topCommenters;
    }

    public List<Approver> getTopApprovers() {
        return topApprovers;
    }

    public List<HotMr> getHottestMrs() {
        return hottestMrs;
    }

    public List<LongMr> getLongestMrs() {
        return longestMr;
    }

    public List<BigMr> getBiggestMRs() {
        return biggestMrs;
    }

    public AtomicInteger getAverageMergeRequestsDurationInHours() {
        return averageMergeRequestsDurationInHours;
    }

    public AtomicInteger getAverageTimeToFirstCommentInHours() {
        return averageTimeToFirstCommentInHours;
    }

    public AtomicReference<Double> getCountNitComments() {
        return averageCountOfNitComments;
    }

    public record Contributor(String name, Long contributes) {

    }

    public record Commenter(String name, Integer comments) {

    }

    public record Approver(String name, Integer approvals) {

    }

    public record HotMr(String id, String title, String author, Integer interactions, String link) {

    }

    public record LongMr(String id, String title, String author, Integer durationInHours, String link) {

    }

    public record BigMr(String id, String title, String author, Integer changes, String link) {

    }

}
