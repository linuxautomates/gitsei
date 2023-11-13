package io.levelops.etl.jobs.user_id_consolidation;

import io.levelops.commons.databases.models.database.scm.DbScmUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserConsolidationGraphUtils {
    public static void createGraphs(
            Collection<DbScmUser> integrationUsers,
            Map<String, List<String>> emailToUserIds,
            Map<String, List<String>> emailGraph) {
        for (DbScmUser user : integrationUsers) {
            user.getEmails().forEach(email -> {
                if (emailToUserIds.containsKey(email)) {
                    emailToUserIds.get(email).add(user.getId());
                } else {
                    emailToUserIds.put(email, new ArrayList<>(List.of(user.getId())));
                }
            });
            addEmailsToGraph(user.getEmails(), emailGraph);
        }
    }

    private static void traverse(
            Map<String, List<String>> emailGraph,
            String node,
            Set<String> visited,
            Set<String> connectedComponents) {
        if (visited.contains(node) || !emailGraph.containsKey(node)) {
            return;
        }
        visited.add(node);
        connectedComponents.add(node);
        emailGraph.get(node).forEach(neighbor -> {
            traverse(emailGraph, neighbor, visited, connectedComponents);
        });
    }

    public static List<Set<String>> getConnectedComponents(Map<String, List<String>> emailGraph) {
        Set<String> visited = new HashSet<>();
        List<Set<String>> connectedComponents = new ArrayList<>();
        emailGraph.keySet().forEach(email -> {
            if (!visited.contains(email)) {
                Set<String> connectedComponent = new HashSet<>();
                traverse(emailGraph, email, visited, connectedComponent);
                connectedComponents.add(connectedComponent);
            }
        });
        return connectedComponents;
    }

    private static void addEmailsToGraph(List<String> emails, Map<String, List<String>> emailGraph) {
        if (emails.size() == 1) {
            if (!emailGraph.containsKey(emails.get(0))) {
                emailGraph.put(emails.get(0), new ArrayList<>());
            }
            return;
        }
        for (int i = 0; i < emails.size() - 1; i++) {
            addBiDirectionalNode(emails.get(i), emails.get(i + 1), emailGraph);
        }
    }

    private static void addBiDirectionalNode(String email1, String email2, Map<String, List<String>> emailGraph) {
        if (emailGraph.containsKey(email1)) {
            emailGraph.get(email1).add(email2);
        } else {
            emailGraph.put(email1, new ArrayList<>(List.of(email2)));
        }
        if (emailGraph.containsKey(email2)) {
            emailGraph.get(email2).add(email1);
        } else {
            emailGraph.put(email2, new ArrayList<>(List.of(email1)));
        }
    }
}
