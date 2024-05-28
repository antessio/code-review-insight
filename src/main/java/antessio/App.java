package antessio;

import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
        Boolean useBackup = Optional.ofNullable(System.getenv("useBackup"))
                .map(Boolean::parseBoolean)
                .orElse(false);
        CodeReviewDataExporter exporter;
        Clock clock = Clock.systemUTC();
        Instant now = clock.instant();
        Instant from = now.atZone(ZoneId.of("UTC")).minusMonths(12).toInstant();
        Instant to = now.atZone(ZoneId.of("UTC")).minusMonths(6).toInstant();
        if (useBackup){
            exporter = Optional.of(OUTPUT_FILE)
                               .map(File::new)
                               .filter(File::exists)
                               .map(DataExporterFromBackup::new)
                               .map(CodeReviewDataExporter.class::cast)
                    .orElseThrow(()-> new IllegalArgumentException("no backup found"));
        }else{
            exporter = new GitLabExporter(new Gitlab(accessKey, gitlabHost),
                                          team,
                                          1000,
                                          List.of("1389"),
                                          OUTPUT_FILE,
                                          from,
                                          to,
                                          clock);
        }




        CodeReviewInsightService codeReviewInsightService = new CodeReviewInsightService(exporter);

        MarkdownReport markdownReport = new MarkdownReport(new File("output/report_%s_%s-%s.md".formatted(Instant.now().toString(), from, to)), codeReviewInsightService);
        markdownReport.generateReport();
    }

}
