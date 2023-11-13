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
@RequestMapping("/v1/ncc/issues")
public class NccIssuesController {
    
    @PostMapping("/list")
    public DeferredResult<ResponseEntity<Map<String, Object>>> listIssues(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest body){
        Map<String, Object> item1 = new HashMap<>();
        item1.put("component", "All");
        item1.put("projects", List.of(Map.of("118", "Hello"), Map.of("71","Riverdale")));
        item1.put("tags", List.of(Map.of("124","test"), Map.of("302","tag50")));
        item1.put("ingested_at", 1611234000);
        item1.put("category", "patching");
        item1.put("impact", "high");
        item1.put("impact_description", "Obsolete or outdated open source libraries can result in easily exploited vulnerabilities.");
        item1.put("location", "zcutil/fetch-params.sh:17");
        item1.put("exploitability", "medium_identiﬁer_ncc-zcash2016-017");
        item1.put("recomendation", "Guarantee that corrupted keys are discarded upon checksum validation failure, and do not");
        item1.put("risk", "high");
        item1.put("name", "Use of Obsolete Open Source Libraries");
        item1.put("status", "reported");
        item1.put("description", "The fetch-params.sh script retrieves the veriﬁcation and proving keys from the z.cash");

        Map<String, Object> item2 = new HashMap<>();
        item2.put("component", "zcash");
        item2.put("projects", List.of(Map.of("118", "Hello")));
        item2.put("tags", List.of(Map.of("302","tag50")));
        item2.put("ingested_at", 1611334000);
        item1.put("category", "cryptography");
        item1.put("impact", "high");
        item1.put("impact_description", "An adversary who is able to conduct a man-in-the-middle attack could tamper with");
        item1.put("location", "• depends/packages/bdb.mk");
        item1.put("exploitability", "high_identiﬁer_ncc-zcash2016-007");
        item1.put("recomendation", "NCC Group recommends following a continuous integration process whereby new, stable");
        item1.put("risk", "high");
        item2.put("name", "Key Retrieval Script Subject to Man-in-the-Middle Attack");
        item2.put("status", "open");
        item2.put("description", "The email server doesn't provide validation mechanisms to allow email clients validate the authenticity of emails claiming to be originated by our company.");

        Map<String, Object> item3 = new HashMap<>();
        item3.put("component", "bitcoin");
        item3.put("projects", List.of(Map.of("71","Riverdale")));
        item3.put("tags", List.of(Map.of("124","test")));
        item3.put("ingested_at", 1611234000);
        item1.put("category", "Data Validation");
        item1.put("impact", "high");
        item1.put("impact_description", "Object types have alignment requirements that place restrictions on the addresses at which");
        item1.put("location", "• uint256.h:22");
        item1.put("exploitability", "low_identiﬁer_ncc-zcash2016-004");
        item1.put("recomendation", "Align data to have the same alignment as uint32_t:");
        item1.put("risk", "high");
        item3.put("name", "Vulnerability");
        item3.put("status", "open");
        item3.put("description", "Line 22 of ﬁle uint256.h includes the following deﬁnition");

        Map<String, Object> item4 = new HashMap<>();
        item4.put("component", "bitcoin");
        item4.put("projects", List.of(Map.of("118", "Hello"), Map.of("71","Riverdale")));
        item4.put("tags", List.of(Map.of("320","lol")));
        item4.put("ingested_at", 1611234000);
        item1.put("category", "Data Exposure");
        item1.put("impact", "medium");
        item1.put("impact_description", "Reading deallocated memory can result in leaking sensitive information.");
        item1.put("location", "src/scheduler.cpp:58");
        item1.put("exploitability", "medium_identiﬁer_ncc-zcash2016-011");
        item1.put("recomendation", "Upgrade to Boost 1.62.0 to integrate the repair.");
        item1.put("risk", "medium");
        item4.put("name", "Heap Use After Free");
        item4.put("status", "fixed");
        item4.put("description", "The following code at src/scheduler.cpp:58 results in a read of 8 bytes after the memory has");
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
        List<Map<String, Object>> riskRecords = List.of(
            Map.of("key", "high", "count", 1),
            Map.of("key", "low", "count", 2),
            Map.of("key", "medium", "count", 1)
        );
        List<Map<String, Object>> categoryRecords = List.of(
            Map.of("key", "data validation", "count", 1),
            Map.of("key", "data exposure", "count", 1),
            Map.of("key", "cryptography", "count", 1),
            Map.of("key", "patching", "count", 1)
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
                records = riskRecords;
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
                    Map.of("key", "data validation", "stacks", List.of(
                        Map.of("key", "low", "count", 1),
                        Map.of("key", "medium", "count", 2),
                        Map.of("key", "high", "count", 1),
                        Map.of("key", "Hello", "count", 1),
                        Map.of("key", "Riverdale", "count", 2)
                    )),
                    Map.of("key", "data exposure", "stacks", List.of(
                        Map.of("key", "low", "count", 2),
                        Map.of("key", "medium", "count", 0),
                        Map.of("key", "high", "count", 1),
                        Map.of("key", "Hello", "count", 3),
                        Map.of("key", "Riverdale", "count", 0)
                    )),
                    Map.of("key", "cryptography", "stacks", List.of(
                        Map.of("key", "low", "count", 0),
                        Map.of("key", "medium", "count", 1),
                        Map.of("key", "high", "count", 1),
                        Map.of("key", "Hello", "count", 2),
                        Map.of("key", "Riverdale", "count", 1)
                    )),
                    Map.of("key", "patching", "stacks", List.of(
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
                    Map.of("component", List.of(Map.of("key","all"), Map.of("key", "zcash"), Map.of("key", "bitcoin"))),
                    Map.of("risk", List.of(Map.of("key","low"), Map.of("key", "medium"), Map.of("key", "high"))),
                    Map.of("category", List.of(Map.of("key","data validation"), Map.of("key","patching"), Map.of("key","cryptography"), Map.of("key","web"), Map.of("key","data exposure")))
                    )
                )
            ) );
    }
}
