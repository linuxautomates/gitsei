package io.levelops.api.controllers;

import io.levelops.api.model.KudosRequest;
import io.levelops.api.model.KudosResponse;
import io.levelops.api.model.KudosSharingRequest;
import io.levelops.api.model.KudosWidget;
import io.levelops.commons.databases.models.database.QueryFilter;
// import io.levelops.api.model.KudosRequest.KudosMetadataRequest;
import io.levelops.commons.databases.models.database.kudos.DBKudos;
import io.levelops.commons.databases.models.database.kudos.DBKudosSharing;
import io.levelops.commons.databases.models.database.kudos.DBKudosSharing.KudosSharingType;
import io.levelops.commons.databases.models.database.kudos.DBKudosWidget;
import io.levelops.commons.databases.services.KudosDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.EmailException;
import io.levelops.files.services.FileStorageService;
import io.levelops.models.EmailContact;
import io.levelops.notification.services.NotificationService;
import io.levelops.web.util.SpringUtils;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequestMapping("/v1/kudos")
@RestController
public class KudosController {

    private final KudosDatabaseService kudosService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationsService;

    @Autowired
    public KudosController(final KudosDatabaseService kudosService, final FileStorageService fileStorageService, final NotificationService notificationsService) {
        this.kudosService = kudosService;
        this.fileStorageService = fileStorageService;
        this.notificationsService = notificationsService;
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public DeferredResult<ResponseEntity<UUID>> createKudos(@SessionAttribute("company") final String company,
        @SessionAttribute("session_user") final String user,
        @RequestParam(value = "data") final KudosRequest request,
        @RequestParam(value = "screenshot") final MultipartFile fileUpload){
        return SpringUtils.deferResponse(() -> {
            var item = map(request);
            var id = UUID.randomUUID();
            // upload file
            UUID screenshotId = null;
            if (request.getWidgetId() != null) {
                screenshotId = UUID.fromString(fileStorageService.uploadNewFileForSubComponent(company, user, "kudos", id.toString(), "widget", request.getWidgetId().toString(), "screenshot", fileUpload.getOriginalFilename(), fileUpload.getContentType(), fileUpload.getInputStream()));
            }
            else {
                screenshotId = UUID.fromString(fileStorageService.uploadNewFileForComponent(company, user, "kudos", id.toString(), "screenshot", fileUpload.getOriginalFilename(), fileUpload.getContentType(), fileUpload.getInputStream()));
            }
            item = item.toBuilder().id(id).screenshotId(screenshotId).build();
            kudosService.insert(company, item);
            return ResponseEntity.ok(id);
        });
    }

    private DBKudos map(final KudosRequest request) {
        final Set<DBKudosSharing> sharings = new HashSet<>();
        if (request.getShare() != null) {
            if (CollectionUtils.isNotEmpty(request.getShare().getChannels())) {
                request.getShare().getChannels().forEach(item -> {
                    sharings.add(DBKudosSharing.builder()
                        .type(KudosSharingType.SLACK)
                        .target(item)
                        .build());
                });
            }
            else if (CollectionUtils.isNotEmpty(request.getShare().getEmails())) {
                request.getShare().getEmails().forEach(item -> {
                    sharings.add(DBKudosSharing.builder()
                        .type(KudosSharingType.EMAIL)
                        .target(item)
                        .build());
                });
            }
        }
        var widgets = new HashSet<DBKudosWidget>();
        if (request.getWidgetId() != null) {
            widgets.add(DBKudosWidget.builder()
                .widgetId(request.getWidgetId())
                .data(request.getData())
                .position(request.getPosition())
                .size(request.getSize())
                .build());
        }
        return DBKudos.builder()
            .anonymousLink(request.getAnonymousLink())
            .author(request.getAuthor())
            .body(request.getBody())
            .breadcrumbs(request.getBreadcrumbs())
            .createdAt(request.getCreatedAt())
            .dashboardId(request.getDashboardId())
            .expiration(request.getExpiration())
            .icon(request.getIcon())
            .includeWidgetDetails(request.getIncludeWidgetDetails())
            .level(request.getLevel())
            .sharings(sharings)
            .widgets(widgets)
            .build();
    }

    private KudosResponse map(final DBKudos kudos, final byte[] file) {
        return KudosResponse.builder()
            .anonymousLink(kudos.getAnonymousLink())
            .author(kudos.getAuthor())
            .body(kudos.getBody())
            .breadcrumbs(kudos.getBreadcrumbs())
            .createdAt(kudos.getCreatedAt())
            .dashboardId(kudos.getDashboardId())
            // .data(kudos.ge)
            .expiration(kudos.getExpiration())
            .icon(kudos.getIcon())
            .id(kudos.getId())
            .includeWidgetDetails(kudos.getIncludeWidgetDetails())
            .level(kudos.getLevel())
            .screenshot(file != null ? new String(Base64.getEncoder().encode(file)) : null)
            .build();
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<KudosResponse>> getKudos(
            @SessionAttribute("company") final String company,
            @PathVariable("id") final UUID id,
            @RequestParam(name = "include_screenshot", required = false) final Boolean includeScreenshot,
            @RequestParam(name = "include_data", required = false) final Boolean includeData){
        return SpringUtils.deferResponse(() -> {
            var r = kudosService.get(company, id.toString());
            byte[] file = null;
            if (includeScreenshot && r.get().getScreenshotId() != null) {
                file = fileStorageService.downloadFileForComponent(company, "kudos", id.toString(), r.get().getScreenshotId().toString());
            }
            return ResponseEntity.ok(map(r.get(), file));
        });
    }
    
    @PostMapping(path = "/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<KudosResponse>>> listKudos(
            @SessionAttribute("company") final String company,
            @RequestBody DefaultListRequest request,
            @RequestParam(name = "include_screenshot", required = false) final Boolean includeScreenshot,
            @RequestParam(name = "include_data", required = false) final Boolean includeData){
        return SpringUtils.deferResponse(() -> {
            var r = kudosService.filter(company, QueryFilter.fromRequestFilters(request.getFilter()), request.getPage(), request.getPageSize());
            var records = r.getRecords().stream().map(i -> {
                byte[] file = null;
                if (includeScreenshot) {
                    file = fileStorageService.downloadFileForComponent(company, "kudos", i.getId().toString(), i.getScreenshotId().toString());
                }
                // add screenshot
                return map(i, file);
            }).collect(Collectors.toList()); 
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), r.getTotalCount(), records));
        });
    }
    
    @PostMapping(path = "/{id}/share", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public void shareKudos(@SessionAttribute("company") final String company, @PathVariable("id") UUID kudosId, @RequestBody KudosSharingRequest request){
        if(CollectionUtils.isNotEmpty(request.getChannels())) {
            try {
                notificationsService.sendSlackNotification(company, "You have kudos", List.copyOf(request.getChannels()), "KudosBot");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(CollectionUtils.isNotEmpty(request.getEmails())) {
            EmailContact from = EmailContact.builder().email("noreply@propelo.ai").name("Propelo Kudos").build();
            request.getEmails().stream().forEach(email -> {
                try {
                    notificationsService.sendEmailNotification("You have kudos", "You have kudos!", from, email);
                } catch (EmailException e) {
                    e.printStackTrace();
                }
            });
        }
        // perform sharing
        // register sharing
    }

    // /v1/kudos/{id}/widgets
    @PostMapping(path = "/{id}/widgets", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> createWidgetKudos(
            @SessionAttribute("company") final String company,
            @SessionAttribute("session_user") final String user,
            @PathVariable("id") final UUID kudosId, 
            @RequestParam(value = "data") final KudosWidget request,
            @RequestParam(value = "screenshot") final MultipartFile fileUpload) {
        return SpringUtils.deferResponse(() -> {
            var item = map(request);
            var id = UUID.randomUUID();
            // upload file
            UUID screenshotId = UUID.fromString(fileStorageService.uploadNewFileForSubComponent(company, user, "kudos", id.toString(), "widget", request.getWidgetId().toString(), "screenshot", fileUpload.getOriginalFilename(), fileUpload.getContentType(), fileUpload.getInputStream()));
            
            item = item.toBuilder().screenshotId(screenshotId).build();
            kudosService.insertKudosWidgets(company, kudosId, Set.of(item));
            return ResponseEntity.ok("ok");
        });
    }

    private DBKudosWidget map(final KudosWidget item) {
        return DBKudosWidget.builder()
            .data(item.getData())
            .position(item.getPosition())
            .size(item.getSize())
            .widgetId(item.getWidgetId())
            .build();
    }

    // /v1/kudos/{id}/widgets/{id}
    @GetMapping(path = "/{id}/widgets/{wid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> createWidget(
            @SessionAttribute("company") final String company,
            @PathVariable("id") final UUID kudosId,
            @PathVariable("wid") final UUID widgetId) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(""));
    }

    // /v1/kudos/{id}/widgets/list
    @PostMapping(path = "/{id}/widgets/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Collection<KudosWidget>>> listKudosWidgets(
            @SessionAttribute("company") final String company,
            @PathVariable("id") final UUID kudosId,
            @RequestBody DefaultListRequest request,
            @RequestParam(name = "include_screenshot", required = false) final Boolean includeScreenshot,
            @RequestParam(name = "include_data", required = false) final Boolean includeData) {
        return SpringUtils.deferResponse(() -> {
            var dbRecords = kudosService.getKudosWidgets(company, kudosId);
            var results = dbRecords.stream().map(item -> {
                // byte[] file = null;
                // if (item.getScreenshotId() != null){
                //     try {
                //         file = fileStorageService.downloadFileForSubComponent(company, "kudos", kudosId.toString(), "widget", item.getWidgetId().toString() ,item.getScreenshotId().toString());
                //     } catch (IOException e) {
                //         // TODO Auto-generated catch block
                //         e.printStackTrace();
                //     }
                // }
                return map(item);
            }).collect(Collectors.toSet());
            return ResponseEntity.ok(results);
        });
    }

    private KudosWidget map(DBKudosWidget item) {
        return KudosWidget.builder()
            .data(item.getData())
            .position(item.getPosition())
            .size(item.getSize())
            .widgetId(item.getWidgetId())
            .build();
    }

    // /v1/kudos/{id}/widgets/{id}/image
    @GetMapping(path = "/{id}/widgets/{wid}/image", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> getWidgetImage(@SessionAttribute("company") final String company, @PathVariable("id") final UUID kudosId, @PathVariable("wid") final UUID widgetId) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(""));
    }

}
