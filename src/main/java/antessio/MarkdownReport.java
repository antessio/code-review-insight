package antessio;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import antessio.common.FileUtils;

public class MarkdownReport {

    public static final String NUMBER_OF_ANALYSED_MERGE_REQUESTS_PLACEHOLDER = "§number_of_analysed_merge_requests";
    public static final String TARGET_DATE_FROM_PLACEHOLDER = "§target_date_from";
    private final File outputFile;

    private final static String CONTRIBUTOR_TABLE_PLACEHOLDER = "§contributors_table";
    private final String CONTRIBUTOR_TABLE = "| user | number of contributes |\n"
                                             + "|------|-----------------------|\n";

    private final static String COMMENTERS_TABLE_PLACEHOLDER = "§commenters_table";
    private final String COMMENTERS_TABLE = "| user | number of comments |\n"
                                            + "|------|--------------------|\n";

    private final static String APPROVERS_TABLE_PLACEHOLDER = "§approvers_table";
    private final String APPROVERS_TABLE = "| user | number of approvals |\n"
                                           + "|------|---------------------|\n";
    private final static String HOTTEST_MERGE_REQUESTS_TABLE_PLACEHOLDER = "§hottest_merge_requests_table";
    private final static String HOTTEST_MERGE_REQUESTS_TABLE = "| link | title | author | comments |\n"
                                                               + "|------|-------|--------|----------|\n";
    private final static String LONGEST_MERGE_REQUESTS_TABLE_PLACEHOLDER = "§longest_merge_requests_table";
    private final static String LONGEST_MERGE_REQUESTS_TABLE = "| link | title | author | duration in minutes |\n"
                                                               + "|------|-------|--------|-------------------|\n";
    private final static String BIGGEST_MERGE_REQUESTS_TABLE_PLACEHOLDER = "§biggest_merge_requests_table";
    private final static String BIGGEST_MERGE_REQUESTS_TABLE = "| link | title | author | changes count |\n"
                                                               + "|------|-------|--------|---------------|\n";

    private final static String AVERAGE_DURATION_PLACEHOLDER = "§average_merge_requests_duration";
    private final static String AVERAGE_FIRST_COMMENT_DURATION_PLACEHOLDER = "§average_first_comment_duration";
    private final static String AVERAGE_NIT_COUNT_PLACEHOLDER = "§average_nit_count";

    private final static String TIMES_FIRST_COMMENT_WAS_NIT_PLACEHOLDER = "§times_first_comment_was_nit";
    private final Supplier<List<CodeReviewInsightService.Approver>> topApprovers;
    private final Supplier<List<CodeReviewInsightService.HotMr>> hottestMrs;
    private final Supplier<List<CodeReviewInsightService.LongMr>> longestMrs;
    private final Supplier<List<CodeReviewInsightService.BigMr>> biggestMrs;
    private final Supplier<AtomicInteger> averageMergeRequestsDurationInHours;
    private final Supplier<AtomicInteger> averageTimeToFirstCommentInHours;
    private final Supplier<AtomicReference<Double>> countNitComments;
    private final Supplier<AtomicLong> getTimesFirstCommentWasNit;
    private final CodeReviewInsightService codeReviewInsightService;
    private Supplier<List<CodeReviewInsightService.Commenter>> topCommenters;
    private Supplier<List<CodeReviewInsightService.Contributor>> topContributors;

    public MarkdownReport(
            File outputFile,
            CodeReviewInsightService codeReviewInsightService) {
        this.outputFile = outputFile;
        topCommenters = codeReviewInsightService::getTopCommenters;
        topContributors = codeReviewInsightService::getTopContributors;
        topApprovers = codeReviewInsightService::getTopApprovers;
        hottestMrs = codeReviewInsightService::getHottestMrs;
        longestMrs = codeReviewInsightService::getLongestMrs;
        biggestMrs = codeReviewInsightService::getBiggestMRs;
        averageMergeRequestsDurationInHours = codeReviewInsightService::getAverageMergeRequestsDurationInHours;
        averageTimeToFirstCommentInHours = codeReviewInsightService::getAverageTimeToFirstCommentInHours;
        countNitComments  = codeReviewInsightService::getCountNitComments;
        getTimesFirstCommentWasNit = codeReviewInsightService::getTimesFirstCommentWasNit;
        this.codeReviewInsightService = codeReviewInsightService;
    }


    public void generateReport() {
        String template = FileUtils.loadFileAsString("report_template.md");
        StringBuilder contributorsTableBuilder = new StringBuilder(CONTRIBUTOR_TABLE);
        StringBuilder commentersTableBuilder = new StringBuilder(COMMENTERS_TABLE);
        StringBuilder approversTableBuilder = new StringBuilder(APPROVERS_TABLE);
        StringBuilder hottestMergeRequestsTableBuilder = new StringBuilder(HOTTEST_MERGE_REQUESTS_TABLE);
        StringBuilder longestMergeRequestsTableBuilder = new StringBuilder(LONGEST_MERGE_REQUESTS_TABLE);
        StringBuilder biggestMergeRequestsTableBuilder = new StringBuilder(BIGGEST_MERGE_REQUESTS_TABLE);

        topCommenters
                .get()
                .stream()
                .sorted(Comparator.comparing(CodeReviewInsightService.Commenter::comments).reversed())
                .map(c -> "|%s|%d|\n".formatted(c.name(), c.comments()))
                .forEach(commentersTableBuilder::append);

        topContributors
                .get()
                .stream()
                .sorted(Comparator.comparing(CodeReviewInsightService.Contributor::contributes).reversed())
                .map(c -> "|%s|%d|\n".formatted(c.name(), c.contributes()))
                .forEach(contributorsTableBuilder::append);
        topApprovers
                .get()
                .stream()
                .sorted(Comparator.comparing(CodeReviewInsightService.Approver::approvals).reversed())
                .map(c -> "|%s|%d|\n".formatted(c.name(), c.approvals()))
                .forEach(approversTableBuilder::append);
        hottestMrs
                .get()
                .stream()
                .sorted(Comparator.comparing(CodeReviewInsightService.HotMr::interactions).reversed())
                .limit(10)
                .map(mr -> "|%s|%s|%s|%d|\n".formatted(mr.link(), mr.title(), mr.author(), mr.interactions()))
                .forEach(hottestMergeRequestsTableBuilder::append);
        longestMrs
                .get()
                .stream()
                .sorted(Comparator.comparing(CodeReviewInsightService.LongMr::durationInHours).reversed())
                .limit(10)
                .map(mr -> "|%s|%s|%s|%d|\n".formatted(mr.link(), mr.title(), mr.author(), mr.durationInHours()))
                .forEach(longestMergeRequestsTableBuilder::append);
        biggestMrs
                .get()
                .stream()
                .sorted(Comparator.comparing(CodeReviewInsightService.BigMr::changes).reversed())
                .limit(10)
                .map(mr -> "|%s|%s|%s|%d|\n".formatted(mr.link(), mr.title(), mr.author(), mr.changes()))
                .forEach(biggestMergeRequestsTableBuilder::append);
        String report = template
                .replace(
                        CONTRIBUTOR_TABLE_PLACEHOLDER,
                        contributorsTableBuilder.toString())
                .replace(
                        COMMENTERS_TABLE_PLACEHOLDER,
                        commentersTableBuilder.toString())
                .replace(
                        APPROVERS_TABLE_PLACEHOLDER,
                        approversTableBuilder.toString())
                .replace(
                        HOTTEST_MERGE_REQUESTS_TABLE_PLACEHOLDER,
                        hottestMergeRequestsTableBuilder.toString())
                .replace(
                        LONGEST_MERGE_REQUESTS_TABLE_PLACEHOLDER,
                        longestMergeRequestsTableBuilder.toString())
                .replace(
                        BIGGEST_MERGE_REQUESTS_TABLE_PLACEHOLDER,
                        biggestMergeRequestsTableBuilder.toString())
                .replace(
                        AVERAGE_DURATION_PLACEHOLDER,
                        averageMergeRequestsDurationInHours.get().toString())
                .replace(
                        AVERAGE_FIRST_COMMENT_DURATION_PLACEHOLDER,
                        averageTimeToFirstCommentInHours.get().toString())
                .replace(
                        AVERAGE_NIT_COUNT_PLACEHOLDER,
                        countNitComments.get().toString())
                .replace(TIMES_FIRST_COMMENT_WAS_NIT_PLACEHOLDER,
                         getTimesFirstCommentWasNit.get().toString())
                .replace(
                        NUMBER_OF_ANALYSED_MERGE_REQUESTS_PLACEHOLDER,
                         codeReviewInsightService.mergeRequestsCount()+"")
                .replace(
                        TARGET_DATE_FROM_PLACEHOLDER,
                        codeReviewInsightService.mergeRequestsFrom().toString())
                ;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(report);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}
