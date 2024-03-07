package antessio;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import antessio.gitlab.GitLabExporter;
import antessio.gitlab.Gitlab;

/**
 * Hello world!
 */
public class App {


    public static final String OUTPUT_FILE = "output/merge_requests.json";

    public static void main(String[] args) {
        String accessKey = System.getenv("gitlab.accesskey");
        List<String> team = Arrays.stream(System.getenv("team")
                .split(","))
                .toList();
        String gitlabHost = System.getenv("gitlab.host");
        CodeReviewDataExporter exporter = Optional.of(OUTPUT_FILE)
                                                  .map(File::new)
                                                  .filter(File::exists)
                                                  .map(DataExporterFromBackup::new)
                                                  .map(CodeReviewDataExporter.class::cast)
                                                  .orElseGet(() -> new GitLabExporter(new Gitlab(accessKey, gitlabHost),
                                                                                      team,
                                                                                      90,
                                                                                      1000,
                                                                                      List.of("1389"),
                                                                                      OUTPUT_FILE));


        CodeReviewInsightService codeReviewInsightService = new CodeReviewInsightService(exporter);

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
