package antessio;

import static antessio.common.FileUtils.readFileAsString;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import antessio.common.ObjectMapperJsonConverter;

public class DataExporterFromBackup implements CodeReviewDataExporter{

    private final String filename;
    private final ObjectMapperJsonConverter jsonConverter;
    private List<MergeRequest> mergeRequests = new ArrayList<>();

    public DataExporterFromBackup(String filename) {
        this.filename = filename;
        this.jsonConverter = new ObjectMapperJsonConverter();
        this.initFromBackup();
    }
    public DataExporterFromBackup(File file) {
        this.filename = file.getAbsolutePath();
        this.jsonConverter = new ObjectMapperJsonConverter();
        this.initFromBackup();
    }

    @Override
    public List<MergeRequest> getMergeRequests() {
        return mergeRequests;
    }

    public void initFromBackup() {
        try {
            String content = readFileAsString(filename);
            mergeRequests.addAll(List.of(jsonConverter.fromJson(content, MergeRequest[].class)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
