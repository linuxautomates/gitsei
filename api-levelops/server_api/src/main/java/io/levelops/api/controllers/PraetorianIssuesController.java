package io.levelops.api.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.util.SpringUtils;

@RestController
@RequestMapping("/v1/praetorian/issues")
public class PraetorianIssuesController {
    
    @PostMapping("/list")
    public DeferredResult<ResponseEntity<Map<String, Object>>> listIssues(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest body){
        Map<String, Object> item1 = new HashMap<>();
        item1.put("service", "Web Service 1");
        item1.put("report_grade", "A");
        item1.put("report_security", "Good");
        item1.put("projects", List.of(Map.of("118", "Hello"), Map.of("71","Riverdale")));
        item1.put("tags", List.of(Map.of("124","test"), Map.of("302","tag50")));
        item1.put("ingested_at", 1611234000);
        item1.put("category", "injection");
        item1.put("priority", "high");
        item1.put("name", "Web application injection");
        item1.put("status", "open");
        item1.put("description", "The web application allow for direct string interpretation in queries which enables attackers to perform sql injection");

        Map<String, Object> item2 = new HashMap<>();
        item2.put("service", "Web Service 2");
        item1.put("report_grade", "A");
        item1.put("report_security", "Good");
        item2.put("projects", List.of(Map.of("118", "Hello")));
        item2.put("tags", List.of(Map.of("302","tag50")));
        item2.put("ingested_at", 1611334000);
        item2.put("category", "spoofing");
        item2.put("priority", "low");
        item2.put("name", "Missing validation for email");
        item2.put("status", "open");
        item2.put("description", "The email server doesn't provide validation mechanisms to allow email clients validate the authenticity of emails claiming to be originated by our company.");

        Map<String, Object> item3 = new HashMap<>();
        item3.put("service", "Model 3");
        item1.put("report_grade", "A");
        item1.put("report_security", "Good");
        item3.put("projects", List.of(Map.of("71","Riverdale")));
        item3.put("tags", List.of(Map.of("124","test")));
        item3.put("ingested_at", 1611234000);
        item3.put("category", "web");
        item3.put("priority", "low");
        item3.put("name", "Security Best Practices");
        item3.put("status", "open");
        item3.put("description", "The web application needs to follow the security best practices");

        Map<String, Object> item4 = new HashMap<>();
        item4.put("service", "Model 4");
        item1.put("report_grade", "A");
        item1.put("report_security", "Good");
        item4.put("projects", List.of(Map.of("118", "Hello"), Map.of("71","Riverdale")));
        item4.put("tags", List.of(Map.of("320","lol")));
        item4.put("ingested_at", 1611234000);
        item4.put("category", "authentication");
        item4.put("priority", "medium");
        item4.put("name", "Auth in Cookies");
        item4.put("status", "open");
        item4.put("description", "The web application uses basic auth with cookies which can be open to identity highjacking attacks");
        return SpringUtils.deferResponse( () -> ResponseEntity.ok(
            Map.of(
                "count", 4,
                "_metadata", Map.of(
                    "page_size", 10,
                    "page", 0,
                    "has_more", false,
                    "next_page", 0,
                    "total_count", 4),
                "records", List.of(
                    item1,
                    item2,
                    item3,
                    item4
                )
            )
            ) );
    }
    
    @PostMapping("/aggs")
    public DeferredResult<ResponseEntity<Map<String, Object>>> aggregateIssues(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest body){
        List<Map<String, Object>> priorityRecords = List.of(
            Map.of("key", "high", "count", 1),
            Map.of("key", "low", "count", 2),
            Map.of("key", "medium", "count", 1)
        );
        List<Map<String, Object>> categoryRecords = List.of(
            Map.of("key", "authentication", "count", 1),
            Map.of("key", "web", "count", 1),
            Map.of("key", "spoofing", "count", 1),
            Map.of("key", "injection", "count", 1)
        );
        List<Map<String, Object>> projectRecords = List.of(
            Map.of("key", "Riverdale", "count", 3),
            Map.of("key", "Hello", "count", 3)
        );
        List<Map<String, Object>> tagsRecords = List.of(
            Map.of("key", "lol", "count", 1),
            Map.of("key", "test", "count", 2),
            Map.of("key", "tag50", "count", 2)
        );
        List<Map<String, Object>> records = List.of();

        if (CollectionUtils.isEmpty(body.getStacks()) ) {
            if("priority".equalsIgnoreCase(body.getAcross())) {
                records = priorityRecords;
            }
            else if("category".equalsIgnoreCase(body.getAcross())) {
                records = categoryRecords;
            }
            else if("project".equalsIgnoreCase(body.getAcross())) {
                records = projectRecords;
            }
            else if("tag".equalsIgnoreCase(body.getAcross())) {
                records = tagsRecords;
            }
        }
        else {
            records = List.of(
                    Map.of("key", "spoofing", "stacks", List.of(
                        Map.of("key", "low", "count", 1),
                        Map.of("key", "medium", "count", 2),
                        Map.of("key", "high", "count", 1),
                        Map.of("key", "Hello", "count", 1),
                        Map.of("key", "Riverdale", "count", 2)
                    )),
                    Map.of("key", "web", "stacks", List.of(
                        Map.of("key", "low", "count", 2),
                        Map.of("key", "medium", "count", 0),
                        Map.of("key", "high", "count", 1),
                        Map.of("key", "Hello", "count", 3),
                        Map.of("key", "Riverdale", "count", 0)
                    )),
                    Map.of("key", "injection", "stacks", List.of(
                        Map.of("key", "low", "count", 0),
                        Map.of("key", "medium", "count", 1),
                        Map.of("key", "high", "count", 1),
                        Map.of("key", "Hello", "count", 2),
                        Map.of("key", "Riverdale", "count", 1)
                    ))
                );

        }
        var results = records;
        return SpringUtils.deferResponse( () -> ResponseEntity.ok(
            Map.of(
                "count", results.size(),
                "_metadata", Map.of(
                    "page_size", 10,
                    "page", 0,
                    "has_more", false,
                    "next_page", 0,
                    "total_count", results.size()),
                "records", results
            )
            ) );
    }
    
    @PostMapping("/values")
    public DeferredResult<ResponseEntity<Map<String, Object>>> getValues(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest body){
        return SpringUtils.deferResponse( () -> ResponseEntity.ok(
            Map.of(
                "count", 2,
                "_metadata", Map.of(
                    "page_size", 10,
                    "page", 0,
                    "has_more", false,
                    "next_page", 0,
                    "total_count", 2),
                "records", List.of(
                    Map.of("priority", List.of(Map.of("key","low"), Map.of("key", "medium"), Map.of("key", "high"))),
                    Map.of("category", List.of(Map.of("key","spoofing"), Map.of("key","repudiation"), Map.of("key","injection"), Map.of("key","web"), Map.of("key","authentication")))
                    )
                )
            ) );
    }
}
