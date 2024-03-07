package antessio;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import antessio.gitlab.GitLabExporter;
import antessio.gitlab.Gitlab;

/**
 * Hello world!
 */
public class App {


    public static void main(String[] args) {
        String accessKey = System.getenv("gitlab.accesskey");
        List<String> team = Arrays.stream(System.getenv("team")
                .split(","))
                .toList();
        String gitlabHost = System.getenv("gitlab.host");
        Gitlab gitlab = new Gitlab(accessKey, gitlabHost);
        CodeReviewDataExporter gitLabExporter = new GitLabExporter(gitlab, team, 90, 1000, "output/merge_requests.json");

        CodeReviewDataExporter fromFileExporter = new DataExporterFromBackup("output/merge_requests.json");

        CodeReviewInsightService codeReviewInsightService = new CodeReviewInsightService(fromFileExporter);

        MarkdownReport markdownReport = new MarkdownReport(
                new File("output/report_%s.md".formatted(Instant.now().toString())),
                codeReviewInsightService::getTopCommenters,
                codeReviewInsightService::getTopContributors,
                codeReviewInsightService::getTopApprovers,
                codeReviewInsightService::getHottestMrs,
                codeReviewInsightService::getLongestMrs,
                codeReviewInsightService::getBiggestMRs,
                codeReviewInsightService::getAverageMergeRequestsDurationInHours,
                codeReviewInsightService::getAverageTimeToFirstCommentInHours,
                codeReviewInsightService::getCountNitComments);
        markdownReport.generateReport();
    }

}
