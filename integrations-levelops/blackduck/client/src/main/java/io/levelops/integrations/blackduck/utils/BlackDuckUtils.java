package io.levelops.integrations.blackduck.utils;

import io.levelops.integrations.blackduck.models.BlackDuckProject;
import io.levelops.integrations.blackduck.models.BlackDuckVersion;

import java.util.List;
import java.util.stream.Collectors;

public class BlackDuckUtils {

    public static List<String> extractIdsFromProjects(List<BlackDuckProject> blackDuckProjects) {
        return blackDuckProjects.stream().map(project -> project.getBlackDuckMetadata().getProjectHref())
                .map(str -> str.substring(str.lastIndexOf('/') + 1).strip()).collect(Collectors.toList());
    }

    public static String extractIdFromProject(BlackDuckProject project) {
        String projectHref = project.getBlackDuckMetadata().getProjectHref();
        return projectHref.substring(projectHref.lastIndexOf('/') + 1).strip();
    }

    public static String extractIdFromVersion(BlackDuckVersion version) {
        String projectHref = version.getBlackDuckMetadata().getProjectHref();
        return projectHref.substring(projectHref.lastIndexOf('/') + 1).strip();
    }

    public static List<String> extractIdsFromVersions(List<BlackDuckVersion> blackDuckVersions) {
        return blackDuckVersions.stream().map(project -> project.getBlackDuckMetadata().getProjectHref())
                .map(str -> str.substring(str.lastIndexOf('/') + 1).strip()).collect(Collectors.toList());
    }
}
