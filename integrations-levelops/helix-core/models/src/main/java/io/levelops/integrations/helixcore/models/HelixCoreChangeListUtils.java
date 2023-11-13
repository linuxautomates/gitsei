package io.levelops.integrations.helixcore.models;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.IFileSize;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.option.server.GetChangelistDiffsOptions;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.option.server.GetFileSizesOptions;
import com.perforce.p4java.server.IOptionsServer;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.ChangeVolumeStats;
import io.levelops.commons.utils.NumberUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class HelixCoreChangeListUtils {
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of("zip", "tar", "rar", "cat", "dat", "exe", "tree", "xz", "7z", "png", "jpg", "jpeg", "gif", "tiff", "pdf", "iso", "lib", "pdb");

    private static final Set<String> EXTENSION_WHITE_LIST_SOURCE = Set.of("cpp", "h", "cs", "java", "swift", "go", "kt", "rs", "sig");
    private static final Set<String> EXTENSION_WHITE_LIST_SCRIPT = Set.of("py", "rb", "pl", "bat", "btm", "sh", "js", "ts", "groovy", "gvy", "gy", "gsh", "ps1", "php", "scala", "sc", "sql", "hql");
    private static final Set<String> EXTENSION_WHITE_LIST_CONFIG = Set.of("xml", "yml", "yaml", "json", "vcxproj", "vcproj", "vbproj", "cmake", "make", "drl", "tf", "toml", "txt");
    private static final Set<String> EXTENSION_WHITE_LIST = Stream.of(EXTENSION_WHITE_LIST_SOURCE, EXTENSION_WHITE_LIST_SCRIPT, EXTENSION_WHITE_LIST_CONFIG).flatMap(Set::stream).collect(Collectors.toSet());

    private static final Set<String> FILENAME_WHITE_LIST_CONFIG = Set.of("dockerfile", "jenkinsfile");
    private static final Set<String> FILENAME_WHITE_LIST = Stream.of(FILENAME_WHITE_LIST_CONFIG).flatMap(Set::stream).collect(Collectors.toSet());

    private static final Long RESPONSE_TIME_IN_MSECS_LIMIT = 350L;

    private static final String ADDED_LINES_PATTERN = "add (.*) lines";
    private static final String DELETED_LINES_PATTERN = "deleted (.*) lines";
    private static final String CHANGED_LINES_PATTERN = "changed (.*) lines";
    private static final String CHUNKS_LINES_PATTERN = "chunks (.*) lines";
    private static final String DIFFERENCES = "Differences ...";
    private static final String TOTAL = "_total_";
    private static final String UNKNOWN = "_UNKNOWN_";
    private static final boolean OUTPUT_SHELVED_DIFFS = false; //If true, output diffs of shelved files for the changelist
    private static final boolean RCS_DIFFS = false; //If true, use RCS diff; corresponds to -dn.
    private static final int DIFF_CONTEXT = -1; //Corresponds to -dc[n], with -dc generated for diffContext == 0, -dcn for diffContext > 0, where "n" is of course the value of diffContext.
    private static final boolean SUMMARY_DIFF = true; //If true, use RCS diff; corresponds to -dn.
    private static final int UNIFIED_DIFF = -1; //If true, do a unified diff; corresponds to -du[n] with -du generated for unifiedDiff == 0, -dun for unifiedDiff > 0, where "n" is of course the value of unifiedDiff.
    private static final boolean IGNORE_WHITESPACE_CHANGES = false; //If true, ignore whitespace changes; corresponds to -db
    private static final boolean IGNORE_WHITESPACE = false; //If true, ignore whitespace; corresponds to -dw.
    private static final boolean IGNORE_LINE_ENDINGS = false; //If true, ignore line endings; corresponds to -dl.

    private static final Set<FileAction> SKIPPED_FILE_ACTIONS = Set.of(FileAction.INTEGRATE, FileAction.MOVE, FileAction.MOVE_ADD, FileAction.MOVE_DELETE);

    public static HelixCoreChangeList getHelixCoreChangeList(IChangelist changelist, IOptionsServer server, int maxFileSize) {
        log.info("> Processing changelistId={}", changelist.getId());
        List<IFileSpec> files = null;
        try {
            files = changelist.getFiles(false);
        } catch (ConnectionException | RequestException | AccessException e) {
            log.warn("failed fetch changelist files for id " + changelist.getId(), e);
            return null;
        }
        if (CollectionUtils.isEmpty(files)) {
            //Change List without any files needs to be skipped.
            return null;
        }

        GetChangelistDiffsOptions options = new GetChangelistDiffsOptions(OUTPUT_SHELVED_DIFFS, RCS_DIFFS, DIFF_CONTEXT,
                SUMMARY_DIFF, UNIFIED_DIFF, IGNORE_WHITESPACE_CHANGES, IGNORE_WHITESPACE, IGNORE_LINE_ENDINGS);
        boolean isIntegrationCommit;
        final HelixCoreChangeList.HelixCoreChangeListBuilder builder = HelixCoreChangeList.builder()
                .author(changelist.getUsername())
                .id(changelist.getId())
                .description(changelist.getDescription())
                .status(HelixCoreChangeList.ChangelistStatus.fromString(changelist.getStatus().toString()))
                .lastUpdatedAt(changelist.getDate());

        Stopwatch st = Stopwatch.createStarted();
        String diff = null;
        try (InputStream diffsStream = changelist.getDiffsStream(options)) {
            diff = IOUtils.toString(diffsStream, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.error("Failed to fetch diff for changelist id {}", changelist.getId(), e);
            diff = "";
        }
        long diffFetchTime = st.elapsed(TimeUnit.MILLISECONDS);
        if (diffFetchTime > RESPONSE_TIME_IN_MSECS_LIMIT) {
            log.info("changelistId={} - diff fetch time = {}", changelist.getId(), diffFetchTime);
        }

        try {
            int indexOfDifferences = diff.indexOf(DIFFERENCES);
            String differences = indexOfDifferences != -1 ? diff.substring(indexOfDifferences + 15) : "";

            builder.filesCount(files.size());
            // storing files without diff stats in case getDiff blows up
            builder.files(files.stream()
                    .map(iFileSpec -> fromIFileSpec(iFileSpec, Map.of()))
                    .collect(Collectors.toList()));

            Map<String, ChangeVolumeStats> diffStatsMap = getDiff(changelist, differences, files, server, maxFileSize);
            int additions = diffStatsMap.get(TOTAL).getAdditions();
            int deletions = diffStatsMap.get(TOTAL).getDeletions();
            int changes = diffStatsMap.get(TOTAL).getChanges();
            log.debug("Diff for changelist with id {} and date {}: additions: {}, deletions: {}, changes:{}",
                    changelist.getId(), changelist.getDate(), additions, deletions, changes);
            List<HelixCoreFile> helixCoreFiles = files.stream()
                    .map(iFileSpec -> fromIFileSpec(iFileSpec, diffStatsMap))
                    .collect(Collectors.toList());
            List<String> fileActions = helixCoreFiles.stream().map(HelixCoreFile::getFileAction).collect(Collectors.toList());
            isIntegrationCommit = fileActions.contains(FileAction.INTEGRATE.toString());
            builder.additions(additions)
                    .deletions(deletions)
                    .changes(changes)
                    //.differences(differences) //Please Note: Do NOT enable.
                    .isIntegrationCommit(isIntegrationCommit)
                    .files(helixCoreFiles);
        } catch (Exception e) {
            builder.parseError(e.getMessage());
            log.error("Failed to get diff for changelist " + changelist.getId() + " " + e.getMessage(), e);
        }
        return builder.build();
    }

    private static Map<String, ChangeVolumeStats> getDiff(IChangelist changelist, String differences, List<IFileSpec> files,
                                                          IOptionsServer server, int maxFileSize) {
        if (CollectionUtils.isEmpty(files)) {
            return Map.of(TOTAL, getChangeVolumeStats(TOTAL, 0, 0));
        }
        Map<String, ChangeVolumeStats> diffStatsMap = new HashMap<>();
        int totalAdd = 0;
        int totalDelete = 0;
        List<String> diffLines = differences.lines()
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
        for (IFileSpec iFileSpec : files) {
            String fileName = null;
            try {
                String fullyQualifiedFileName = getFullyQualifiedFileName(iFileSpec);
                fileName = getFileName(fullyQualifiedFileName);
                String fileExtension = StringUtils.lowerCase(FilenameUtils.getExtension(fileName));
                fileExtension = (fileExtension == null) ? "" : fileExtension;
                if (BLOCKED_EXTENSIONS.contains(fileExtension)) {
                    log.debug("changelistId={} - Skipping file '{}' with blocked extension", changelist.getId(), fileName);
                    continue;
                }
                String fileNameLower = (fileName == null) ? "" : StringUtils.lowerCase(fileName);
                if ((!EXTENSION_WHITE_LIST.contains(fileExtension)) && (! FILENAME_WHITE_LIST.contains(fileNameLower))) {
                    log.debug("changelistId={} - Skipping file '{}' without allowed extension and filename", changelist.getId(), fileName);
                    continue;
                }
                int add = 0, delete = 0;
                if ((iFileSpec.getAction() != null) && SKIPPED_FILE_ACTIONS.contains(iFileSpec.getAction())) {
                    log.debug("changelistId={} - Skipping file '{}' with skipped file action '{}'", changelist.getId(), fileName, iFileSpec.getAction());
                    continue;
                }
                if (iFileSpec.getAction() == FileAction.ADD) {
                    add = (int) getLinesAddedCountForNewlyAddedFile(changelist.getId(), server, iFileSpec, maxFileSize);
                } else if (iFileSpec.getAction() == FileAction.DELETE || iFileSpec.getAction() == FileAction.DELETED) {
                    delete = 0;
                    //Please Note: Do NOT enable, customer does NOT care about lines count for deleted files.
                    //delete = getLinesDeletedCountForFile(changelist.getId(), server, iFileSpec, maxFileSize);
                } else if (CollectionUtils.isNotEmpty(diffLines)) {
                    for (int i = 0; i < diffLines.size(); i++) {
                        Pattern pattern = Pattern.compile(fileName);
                        Matcher matcher = pattern.matcher(diffLines.get(i));
                        if (matcher.find()) {
                            if (i + 1 < diffLines.size()) {
                                add = extractDiff(ADDED_LINES_PATTERN, diffLines.get(++i));
                            }
                            if (i + 1 < diffLines.size()) {
                                delete = extractDiff(DELETED_LINES_PATTERN, diffLines.get(++i));
                            }
                            if (i + 1 < diffLines.size()) {
                                ImmutablePair<Integer, Integer> diffForChanges = extractDiffForChanges(diffLines.get(++i));
                                add += diffForChanges.getRight();
                                delete += diffForChanges.getLeft();
                            }
                            break;
                        }
                    }
                }
                totalAdd += add;
                totalDelete += delete;
                diffStatsMap.put(fullyQualifiedFileName, getChangeVolumeStats(fullyQualifiedFileName, add, delete));
            } catch (Exception e) {
                log.error("Failed to get diff details for changelist {} and fileName={}", changelist.getId(), fileName, e);
            }
        }
        diffStatsMap.put(TOTAL, getChangeVolumeStats(TOTAL, totalAdd, totalDelete));
        return diffStatsMap;
    }

    public static Map<String, ChangeVolumeStats> getDiffFromHelixCoreFiles(String differences, List<HelixCoreFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            return Map.of(TOTAL, getChangeVolumeStats(TOTAL, 0, 0));
        }
        Map<String, ChangeVolumeStats> diffStatsMap = new HashMap<>();
        int totalAdd = 0;
        int totalDelete = 0;
        List<String> diffLines = differences.lines()
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
        for (HelixCoreFile file : files) {
            String fullyQualifiedFileName = getFullyQualifiedFileName(file);
            String fileName = getFileName(fullyQualifiedFileName);
            String fileExtension = StringUtils.lowerCase(FilenameUtils.getExtension(fileName));
            if (BLOCKED_EXTENSIONS.contains(fileExtension)) {
                log.debug("Skipping file '{}' with blocked extension", fileName);
                continue;
            }
            if (!EXTENSION_WHITE_LIST.contains(fileExtension)) {
                log.debug("Skipping file '{}' without allowed extension", fileName);
                continue;
            }
            Integer add = 0, delete = 0;
            if (CollectionUtils.isNotEmpty(diffLines)) {
                for (int i = 0; i < diffLines.size(); i++) {
                    Pattern pattern = Pattern.compile(fileName);
                    Matcher matcher = pattern.matcher(diffLines.get(i));
                    if (matcher.find()) {
                        if (i + 1 < diffLines.size()) {
                            add = extractDiff(ADDED_LINES_PATTERN, diffLines.get(++i));
                        }
                        if (i + 1 < diffLines.size()) {
                            delete = extractDiff(DELETED_LINES_PATTERN, diffLines.get(++i));
                        }
                        if (i + 1 < diffLines.size()) {
                            ImmutablePair<Integer, Integer> diffForChanges = extractDiffForChanges(diffLines.get(++i));
                            add += diffForChanges.getRight();
                            delete += diffForChanges.getLeft();
                        }
                        break;
                    }
                }
            }
            totalAdd += add;
            totalDelete += delete;
            diffStatsMap.put(fullyQualifiedFileName, getChangeVolumeStats(fullyQualifiedFileName, add, delete));
        }
        diffStatsMap.put(TOTAL, getChangeVolumeStats(TOTAL, totalAdd, totalDelete));
        return diffStatsMap;
    }

    private static long getLinesAddedCountForNewlyAddedFile(int changelistId, IOptionsServer server, IFileSpec iFileSpec, int maxFileSize) {
        try {
            long fileSize = getFileSize(server, iFileSpec);
            if (fileSize > maxFileSize) {
                log.debug("changelistId={} - Skipping large file '{}': {} bytes (max={} bytes)", changelistId, iFileSpec.getDepotPathString(), fileSize, maxFileSize);
                return 0;
            }
            return getLinesOfCode(server, iFileSpec, changelistId);
        } catch (Exception e) {
            log.error("changelistId={} - Failed to fetch number of lines added for file: {}, hence setting it to 0.", changelistId, iFileSpec.getDepotPathString(), e);
            return 0;
        }
    }

    private static long getLinesDeletedCountForFile(int changelistId, IOptionsServer server, IFileSpec iFileSpec, int maxFileSize) {
        try {
            IFileSpec newIFileSpec = iFileSpec;
            int prevRevision = (iFileSpec.getEndRevision() <= 1) ? iFileSpec.getEndRevision() : iFileSpec.getEndRevision() - 1;
            newIFileSpec.setEndRevision(prevRevision);
            long fileSize = getFileSize(server, iFileSpec);
            if (fileSize > maxFileSize) {
                log.warn("changelistId={} - Skipping large file '{}': {} bytes (max={} bytes)", changelistId, iFileSpec.getDepotPathString(), fileSize, maxFileSize);
                return 0;
            }
            return getLinesOfCode(server, iFileSpec, changelistId);
        } catch (Exception e) {
            log.error("changelistId={} - Failed to fetch number of lines added for file: {}, hence setting it to 0.", changelistId, iFileSpec.getDepotPathString(), e);
            return 0;
        }
    }

    private static long getFileSize(IOptionsServer server, IFileSpec iFileSpec) throws P4JavaException {
        return IterableUtils.getFirst(server.getFileSizes(List.of(iFileSpec), new GetFileSizesOptions().setMaxFiles(1)))
                .map(IFileSize::getFileSize)
                .orElse(0L);
    }

    private static long getLinesOfCode(IOptionsServer server, IFileSpec iFileSpec, int changelistId) throws P4JavaException {
        long lineCount = 0;
        GetFileContentsOptions options = new GetFileContentsOptions();
        options.setAllrevs(false);
        options.setDontAnnotateFiles(true);
        options.setNoHeaderLine(true);
        Stopwatch st = Stopwatch.createStarted();
        InputStream fileContents = server.getFileContents(List.of(iFileSpec), options);
        if(fileContents != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileContents))) {
                long downloadTime = st.elapsed(TimeUnit.MILLISECONDS);
                if(downloadTime > RESPONSE_TIME_IN_MSECS_LIMIT) {
                    log.info("changelistId={} - file={}, file download time = {} ms", changelistId, iFileSpec.getDepotPathString(), downloadTime);
                }
                st.reset();
                st.start();
                lineCount = reader.lines().count();

                long readTime = st.elapsed(TimeUnit.MILLISECONDS);
                if(readTime > RESPONSE_TIME_IN_MSECS_LIMIT) {
                    log.info("changelistId={} - file={}, file read time = {} ms", changelistId, iFileSpec.getDepotPathString(), readTime);
                }
            } catch (IOException e) {
                log.warn("Failed to download file for changelistId={}: {}", changelistId, iFileSpec.getDepotPathString());
            }
        }
        return lineCount;
    }

    private static ChangeVolumeStats getChangeVolumeStats(String fullyQualifiedFileName, Integer add, Integer delete) {
        return ChangeVolumeStats.builder()
                .fileName(fullyQualifiedFileName)
                .additions(add)
                .deletions(delete)
                .changes(0) //LEV-4751: Helix will not contain changed line. The changes chunks also contain addition & deletion info only...
                .build();
    }

    public static ImmutablePair<Integer, Integer> extractDiffForChanges(String line) {
        Pattern pattern = Pattern.compile(CHANGED_LINES_PATTERN);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            Pattern pat = Pattern.compile(CHUNKS_LINES_PATTERN);
            Matcher mat = pat.matcher(matcher.group(0));
            if (mat.find()) {
                String group = mat.group(1);
                return ImmutablePair.of(NumberUtils.toInteger(group.substring(0, group.lastIndexOf("/")).trim(), 0),
                        NumberUtils.toInteger(group.substring(group.lastIndexOf("/") + 1).trim(), 0));
            }
        }
        return ImmutablePair.of(0, 0);
    }

    public static int extractDiff(String patternString, String line) {
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            Pattern pat = Pattern.compile(CHUNKS_LINES_PATTERN);
            Matcher mat = pat.matcher(matcher.group(0));
            if (mat.find()) {
                return NumberUtils.toInteger(mat.group(1), 0);
            }
        }
        return 0;
    }

    public static String getFileName(String depotPathString) {
        final Pattern DEPOT_PATH = Pattern.compile(".*/(.*)", Pattern.CASE_INSENSITIVE);
        String fileName = UNKNOWN;
        if (depotPathString != null) {
            fileName = depotPathString;
            Matcher depotMatcher = DEPOT_PATH.matcher(depotPathString);
            if (depotMatcher.find()) {
                fileName = depotMatcher.group(1);
            }
        }
        return fileName;
    }

    public static String getFullyQualifiedFileName(IFileSpec iFileSpec) {
        String fileName = UNKNOWN;
        if (iFileSpec.getDepotPathString() != null) {
            fileName = iFileSpec.getDepotPathString();
        }
        return fileName;
    }

    public static String getFullyQualifiedFileName(HelixCoreFile file) {
        String fileName = UNKNOWN;
        if (file.getDepotPathString() != null) {
            fileName = file.getDepotPathString();
        }
        return fileName;
    }

    public static HelixCoreFile fromIFileSpec(IFileSpec iFileSpec, Map<String, ChangeVolumeStats> diffStatsMap) {
        String fileName = HelixCoreChangeListUtils.getFullyQualifiedFileName(iFileSpec);
        int additions = 0;
        int deletions = 0;
        int changes = 0;
        if (MapUtils.emptyIfNull(diffStatsMap).containsKey(fileName)) {
            additions = MoreObjects.firstNonNull(diffStatsMap.get(fileName).getAdditions(), 0);
            deletions = MoreObjects.firstNonNull(diffStatsMap.get(fileName).getDeletions(), 0);
            changes = MoreObjects.firstNonNull(diffStatsMap.get(fileName).getChanges(), 0);
        }
        return HelixCoreFile.builder()
                .depotName(iFileSpec.getRepoName())
                .changeListId(iFileSpec.getChangelistId())
                .name(HelixCoreChangeListUtils.getFileName(fileName))
                .SHA(iFileSpec.getSha())
                .commitDate(iFileSpec.getDate())
                .depotPathString(iFileSpec.getDepotPathString())
                .fileType(iFileSpec.getFileType())
                .fileAction(iFileSpec.getAction() != null ? iFileSpec.getAction().toString() : null)
                .otherAction(iFileSpec.getOtherAction() != null ? iFileSpec.getOtherAction().toString() : null)
                .additions(additions)
                .deletions(deletions)
                .changes(changes)
                .build();
    }

}
