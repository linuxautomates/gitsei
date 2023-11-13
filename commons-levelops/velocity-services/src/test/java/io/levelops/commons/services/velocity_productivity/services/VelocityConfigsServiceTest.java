package io.levelops.commons.services.velocity_productivity.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.velocity.OrgProfile;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VelocityConfigsServiceTest {
    private static final String company = "test";

    private static final ObjectMapper m = DefaultObjectMapper.get();

    @Mock
    private static VelocityConfigsDatabaseService velocityConfigsDatabaseService;
    @Mock
    private static OrgProfileDatabaseService orgProfileDatabaseService;
    private static VelocityConfigsService velocityConfigService;

    @BeforeClass
    public static void setup() {
        velocityConfigsDatabaseService = mock(VelocityConfigsDatabaseService.class);
        orgProfileDatabaseService = mock(OrgProfileDatabaseService.class);
        velocityConfigService = new VelocityConfigsService(
                velocityConfigsDatabaseService, orgProfileDatabaseService
        );
        MockitoAnnotations.openMocks(VelocityConfigsServiceTest.class);
    }


    @Test
    public void testGetByOuRefIdOnSuccess() throws SQLException {
        // configure
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .defaultConfig(false)
                .preDevelopmentCustomStages(List.of())
                .postDevelopmentCustomStages(List.of())
                .build();
        UUID profileId = UUID.randomUUID();
        VelocityConfig velocityConfig = VelocityConfig.builder()
                .id(profileId)
                .defaultConfig(false)
                .isNew(true)
                .config(velocityConfigDTO)
                .build();

        String ouRefId = "1";
        when(velocityConfigsDatabaseService.getByOuRefId(company, 1)).thenReturn(Optional.of(velocityConfig));
        when(orgProfileDatabaseService.getByProfileId(company, profileId.toString(), OrgProfile.ProfileType.WORKFLOW))
                .thenReturn(Optional.of(List.of(ouRefId)));
        VelocityConfigDTO expectedVelocityConfigDTO = velocityConfigDTO.toBuilder()
                .id(profileId)
                .associatedOURefIds(List.of(ouRefId))
                .isNew(true)
                .build();

        // execute
        Optional<VelocityConfigDTO> actualVelocityDTO = velocityConfigService.getByOuRefId(company, 1);

        // assert
        Assert.assertEquals(expectedVelocityConfigDTO, actualVelocityDTO.get());
    }

    @Test
    public void testGetForNewDFAndCFR() throws SQLException, IOException {

        String velocityString = ResourceUtils.getResourceAsString("v2_workflow_profile_scm.json");
        VelocityConfigDTO velocityConfigDTO = m.readValue(velocityString, VelocityConfigDTO.class);

        UUID profileId = UUID.randomUUID();
        VelocityConfig velocityConfig = VelocityConfig.builder()
                .id(profileId)
                .defaultConfig(false)
                .isNew(true)
                .config(velocityConfigDTO)
                .build();

        String ouRefId = "1";
        when(velocityConfigsDatabaseService.get(company, profileId.toString())).thenReturn(Optional.of(velocityConfig));
        when(orgProfileDatabaseService.getByProfileId(company, profileId.toString(), OrgProfile.ProfileType.WORKFLOW))
                .thenReturn(Optional.of(List.of(ouRefId)));
        VelocityConfigDTO expectedVelocityConfigDTO = getExpectedVelocityConfig(velocityConfigDTO, profileId, ouRefId);

        Optional<VelocityConfigDTO> actualVelocityDTO = velocityConfigService.get(company, profileId.toString());
        Assert.assertTrue(actualVelocityDTO.isPresent());
        Assert.assertEquals(expectedVelocityConfigDTO, actualVelocityDTO.get());

    }

    @Test
    public void testGetByOuRefIdForNewDFAndCFR() throws SQLException, IOException {

        String velocityString = ResourceUtils.getResourceAsString("v2_workflow_profile_scm.json");
        VelocityConfigDTO velocityConfigDTO = m.readValue(velocityString, VelocityConfigDTO.class);

        UUID profileId = UUID.randomUUID();
        VelocityConfig velocityConfig = VelocityConfig.builder()
                .id(profileId)
                .defaultConfig(false)
                .isNew(true)
                .config(velocityConfigDTO)
                .build();

        String ouRefId = "1";
        when(velocityConfigsDatabaseService.getByOuRefId(company, 1)).thenReturn(Optional.of(velocityConfig));
        when(orgProfileDatabaseService.getByProfileId(company, profileId.toString(), OrgProfile.ProfileType.WORKFLOW))
                .thenReturn(Optional.of(List.of(ouRefId)));
        VelocityConfigDTO expectedVelocityConfigDTO = getExpectedVelocityConfig(velocityConfigDTO, profileId, ouRefId);

        Optional<VelocityConfigDTO> actualVelocityDTO = velocityConfigService.getByOuRefId(company, 1);
        Assert.assertTrue(actualVelocityDTO.isPresent());
        Assert.assertEquals(expectedVelocityConfigDTO, actualVelocityDTO.get());

    }

    public VelocityConfigDTO getExpectedVelocityConfig(VelocityConfigDTO velocityConfigDTO, UUID profileId, String ouRefId) {
        return velocityConfigDTO.toBuilder()
                .id(profileId)
                .associatedOURefIds(List.of(ouRefId))
                .isNew(true)
                .defaultConfig(false)
                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().toBuilder()
                                .deploymentFrequency(velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged)
                                        .build())
                                .build())
                        .build())
                .changeFailureRate(velocityConfigDTO.getChangeFailureRate().toBuilder()
                        .velocityConfigFilters(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().toBuilder()
                                .failedDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .build())
                                .totalDeployment(velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().toBuilder()
                                        .calculationField(VelocityConfigDTO.CalculationField.pr_merged_at)
                                        .deploymentCriteria(VelocityConfigDTO.DeploymentCriteria.pr_merged)
                                        .deploymentRoute(VelocityConfigDTO.DeploymentRoute.pr)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Test
    public void testGetOnSuccess() throws SQLException {
        // configure
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .defaultConfig(false)
                .preDevelopmentCustomStages(List.of())
                .postDevelopmentCustomStages(List.of())
                .build();
        UUID profileId = UUID.randomUUID();
        VelocityConfig velocityConfig = VelocityConfig.builder()
                .id(profileId)
                .defaultConfig(false)
                .isNew(true)
                .config(velocityConfigDTO)
                .build();

        String ouRefId = "123";
        when(velocityConfigsDatabaseService.get(company, profileId.toString())).thenReturn(Optional.of(velocityConfig));
        when(orgProfileDatabaseService.getByProfileId(company, profileId.toString(), OrgProfile.ProfileType.WORKFLOW))
                .thenReturn(Optional.of(List.of(ouRefId)));
        VelocityConfigDTO expectedVelocityConfigDTO = velocityConfigDTO.toBuilder()
                .id(profileId)
                .associatedOURefIds(List.of(ouRefId))
                .isNew(true)
                .build();

        // execute
        Optional<VelocityConfigDTO> actualVelocityDTO = velocityConfigService.get(company, profileId.toString());

        // assert
        Assert.assertEquals(expectedVelocityConfigDTO, actualVelocityDTO.get());
    }

    @Test
    public void testUpdateOnSuccess() throws SQLException, BadRequestException {
        // configure
        UUID profileId = UUID.randomUUID();
        VelocityConfigDTO.Stage stage = VelocityConfigDTO.Stage.builder()
                .name("stage-1")
                .description("description-of-stage-1")
                .order(1)
                .event(VelocityConfigDTO.Event.builder()
                        .type(VelocityConfigDTO.EventType.CICD_JOB_RUN)
                        .values(List.of(UUID.randomUUID().toString()))
                        .build())
                .build();
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .id(profileId)
                .isNew(true)
                .preDevelopmentCustomStages(List.of(stage))
                .leadTimeForChanges(
                        VelocityConfigDTO.LeadTimeForChange.builder()
                                .preDevelopmentCustomStages(List.of())
                                .fixedStages(List.of())
                                .postDevelopmentCustomStages(List.of())
                                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                                .build()
                )
                .meanTimeToRestore(
                        VelocityConfigDTO.LeadTimeForChange.builder()
                                .preDevelopmentCustomStages(List.of())
                                .fixedStages(List.of())
                                .postDevelopmentCustomStages(List.of())
                                .issueManagementIntegrations(EnumSet.of(IntegrationType.JIRA))
                                .build()
                )
                .deploymentFrequency(
                        VelocityConfigDTO.DeploymentFrequency.builder()
                                .integrationId(234)
                                .build()

                )
                .changeFailureRate(
                        VelocityConfigDTO.ChangeFailureRate.builder()
                                .integrationId(567)
                                .velocityConfigFilters(
                                        VelocityConfigDTO.VelocityConfigFilters.builder()
                                                .failedDeployment(
                                                        VelocityConfigDTO.FilterTypes.builder()
                                                                .integrationType("SCM").build()
                                                )
                                                .build()
                                )
                                .build()

                )
                .build();

        VelocityConfig velocityConfig = VelocityConfig.builder()
                .id(profileId)
                .defaultConfig(false)
                .cicdJobIds(List.of())
                .config(velocityConfigDTO)
                .build();


        when(velocityConfigsDatabaseService.update(any(String.class), any(VelocityConfig.class))).thenReturn(true);

        // execute
        String actualProfileId = velocityConfigService.update(company, velocityConfigDTO);

        // assert
        Assert.assertEquals(profileId.toString(), actualProfileId);
    }

    @Test
    public void testGetByOuRefIdOnSuccessForDfCfr() throws SQLException, IOException {
        // configure
        String velocityString = ResourceUtils.getResourceAsString("v2_workflow_profile_cicd.json");
        VelocityConfigDTO velocityConfigDTO = m.readValue(velocityString, VelocityConfigDTO.class);

        UUID profileId = UUID.randomUUID();
        VelocityConfig velocityConfig = VelocityConfig.builder()
                .id(profileId)
                .defaultConfig(false)
                .isNew(true)
                .config(velocityConfigDTO)
                .build();

        String ouRefId = "1";
        when(velocityConfigsDatabaseService.getByOuRefId(company, 1)).thenReturn(Optional.of(velocityConfig));
        when(orgProfileDatabaseService.getByProfileId(company, profileId.toString(), OrgProfile.ProfileType.WORKFLOW))
                .thenReturn(Optional.of(List.of(ouRefId)));

        String expectedResult = ResourceUtils.getResourceAsString("v3_workflow_profile_cicd.json");
        VelocityConfigDTO expectedVelocityConfigDTO = m.readValue(expectedResult, VelocityConfigDTO.class);
        expectedVelocityConfigDTO = expectedVelocityConfigDTO.toBuilder()
                .id(profileId)
                .defaultConfig(false)
                .build();

        // execute
        Optional<VelocityConfigDTO> actualVelocityDTO = velocityConfigService.getByOuRefId(company, 1);

        // assert
        Assert.assertEquals(expectedVelocityConfigDTO, actualVelocityDTO.get());
    }
}
