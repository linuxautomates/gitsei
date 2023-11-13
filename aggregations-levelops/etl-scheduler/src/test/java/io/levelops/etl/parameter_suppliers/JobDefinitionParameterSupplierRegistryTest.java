package io.levelops.etl.parameter_suppliers;

import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.commons.etl.models.JobType;
import io.levelops.etl.models.JobDefinitionParameters;
import io.levelops.ingestion.models.IntegrationType;
import lombok.NonNull;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class JobDefinitionParameterSupplierRegistryTest {
    @Test
    public void testSimple() {
        List<JobDefinitionParameterSupplier> suppliers = List.of(
                new GenericTenantTypeSupplier(),
                new GenericIntegrationTypeSupplier(),
                new GenericIntegrationTypeSupplier() {
                    @Override
                    public IntegrationType getIntegrationType() {
                        return IntegrationType.GITHUB;
                    }

                    @Override
                    public @NonNull String getEtlProcessorName() {
                        return "GenericIntegrationJob2";
                    }
                },
                new GenericIntegrationTypeSupplier() {
                    @Override
                    public IntegrationType getIntegrationType() {
                        return IntegrationType.GITHUB;
                    }

                    @Override
                    public @NonNull String getEtlProcessorName() {
                        return "GenericIntegrationJob3";
                    }
                },
                new IngestionProcessingSupplier()
        );
        var registry = new JobDefinitionParameterSupplierRegistry(suppliers);
        var genericTenantJobSuppliers = registry.getGenericTenantJobSuppliers("t");
        var genericIntegrationJobSuppliersJira= registry.getGenericIntegrationJobParameterSupplier(IntegrationType.JIRA.toString(), "t", "1");
        var genericIntegrationJobSuppliersGithub= registry.getGenericIntegrationJobParameterSupplier(IntegrationType.GITHUB.toString(), "t", "1");
        var genericIntegrationJobSuppliersSnyk= registry.getGenericIntegrationJobParameterSupplier(IntegrationType.SNYK.toString(), "t", "1");
        var ingestionProcessingJobSupplier = registry.getIngestionResultProcessingSupplier(IntegrationType.GITHUB.toString());

        assertThat(genericTenantJobSuppliers).isEqualTo(List.of(suppliers.get(0)));
        assertThat(genericIntegrationJobSuppliersJira).isEqualTo(List.of(suppliers.get(1)));
        assertThat(genericIntegrationJobSuppliersGithub).isEqualTo(List.of(suppliers.get(2), suppliers.get(3)));
        assertThat(genericIntegrationJobSuppliersSnyk).isEmpty();
        assertThat(ingestionProcessingJobSupplier).isEqualTo(suppliers.get(4));

        assertThatExceptionOfType(NotImplementedException.class).isThrownBy(() -> registry.getIngestionResultProcessingSupplier(IntegrationType.JIRA.toString()));
    }

    @Test
    public void testWhitelists() {
        List<JobDefinitionParameterSupplier> suppliers = List.of(
                new GenericTenantTypeSupplier() {
                    @Override
                    public List<String> getTenantWhitelist() {
                        return List.of("lakers");
                    }
                },
                new GenericIntegrationTypeSupplier() {
                    @Override
                    public @NonNull String getEtlProcessorName() {
                        return "GenericIntegrationJob2";
                    }

                    @Override
                    public List<IntegrationWhitelistEntry> getIntegrationWhitelistEntries() {
                        return List.of(
                                IntegrationWhitelistEntry.builder()
                                        .integrationId("1")
                                        .tenantId("warriors")
                                        .build()
                        );
                    }

                    @Override
                    public List<String> getTenantWhitelist() {
                        return List.of("lakers");
                    }
                },
                new GenericIntegrationTypeSupplier() {
                    @Override
                    public @NonNull String getEtlProcessorName() {
                        return "GenericIntegrationJob3";
                    }
                },
                new IngestionProcessingSupplier(),
                new GenericIntegrationTypeSupplier() {
                    @Override
                    public IntegrationType getIntegrationType() {
                        return IntegrationType.GITHUB;
                    }

                    @Override
                    public @NonNull String getEtlProcessorName() {
                        return "GenericIntegrationJob5";
                    }

                    @Override
                    public List<IntegrationWhitelistEntry> getIntegrationWhitelistEntries() {
                        return List.of(
                                IntegrationWhitelistEntry.builder()
                                        .integrationId("1")
                                        .tenantId("warriors")
                                        .build()
                        );
                    }
                }
        );
        var registry = new JobDefinitionParameterSupplierRegistry(suppliers);
        var lakersGenericTenantJobSuppliers = registry.getGenericTenantJobSuppliers("lakers");
        var warriorsGenericTenantJobSuppliers = registry.getGenericTenantJobSuppliers("warriors");
        var warriors1GenericIntegrationJobSuppliersJira= registry.getGenericIntegrationJobParameterSupplier(IntegrationType.JIRA.toString(), "warriors", "1");
        var warriors2GenericIntegrationJobSuppliersJira= registry.getGenericIntegrationJobParameterSupplier(IntegrationType.JIRA.toString(), "warriors", "2");
        var warriors1GenericIntegrationJobSuppliersGithub= registry.getGenericIntegrationJobParameterSupplier(IntegrationType.GITHUB.toString(), "warriors", "1");
        var warriors2GenericIntegrationJobSuppliersGithub= registry.getGenericIntegrationJobParameterSupplier(IntegrationType.GITHUB.toString(), "warriors", "2");
        var lakers1GenericIntegrationJobSuppliersGithub= registry.getGenericIntegrationJobParameterSupplier(IntegrationType.GITHUB.toString(), "lakers", "1");
        var lakers1GenericIntegrationJobSuppliersJira= registry.getGenericIntegrationJobParameterSupplier(IntegrationType.JIRA.toString(), "lakers", "1");
        var lakers2GenericIntegrationJobSuppliersJira= registry.getGenericIntegrationJobParameterSupplier(IntegrationType.JIRA.toString(), "lakers", "2");
        var ingestionProcessingJobSupplier = registry.getIngestionResultProcessingSupplier(IntegrationType.GITHUB.toString());

        // Lakers are whitelisted by warriors are not
        assertThat(lakersGenericTenantJobSuppliers).isEqualTo(List.of(suppliers.get(0)));
        assertThat(warriorsGenericTenantJobSuppliers).isEmpty();

        // Warriors integration 1 is whitelisted, but integration 2 is not
        assertThat(warriors1GenericIntegrationJobSuppliersJira).isEqualTo(List.of(suppliers.get(1), suppliers.get(2)));
        assertThat(warriors2GenericIntegrationJobSuppliersJira).isEqualTo(List.of(suppliers.get(2)));

        // Lakers tenant is whitelisted so both lakers 1 and lakers 2 should be whitelisted
        assertThat(lakers1GenericIntegrationJobSuppliersJira).isEqualTo(List.of(suppliers.get(1), suppliers.get(2)));
        assertThat(lakers2GenericIntegrationJobSuppliersJira).isEqualTo(List.of(suppliers.get(1), suppliers.get(2)));


        // Warriors integration 1 for github is whitelisted, but not integration 2
        assertThat(warriors1GenericIntegrationJobSuppliersGithub).isEqualTo(List.of(suppliers.get(4)));
        assertThat(warriors2GenericIntegrationJobSuppliersGithub).isEmpty();

        // Lakers is not whitelisted at all for github so nothing should be returned
        assertThat(lakers1GenericIntegrationJobSuppliersGithub).isEmpty();

        // Sanity check the ingestion processing supplier
        assertThat(ingestionProcessingJobSupplier).isEqualTo(suppliers.get(3));
    }

    // Create 3 JobDefinitionParameterSupplier instances
    public static class GenericTenantTypeSupplier implements JobDefinitionParameterSupplier {
        @Override
        public JobDefinitionParameters getJobDefinitionParameters() {
            return null;
        }

        @Override
        public IntegrationType getIntegrationType() {
            return null;
        }

        @Override
        public JobType getJobType() {
            return JobType.GENERIC_TENANT_JOB;
        }

        @Override
        public ShouldTakeFull shouldTakeFull(DbJobInstance jobInstance, DbJobDefinition jobDefinition, Instant now) {
            return ShouldTakeFull.of(false);
        }

        @Override
        public @NonNull String getEtlProcessorName() {
            return "GenericTenantJob";
        }
    }

    // Create 3 JobDefinitionParameterSupplier instances
    public static class GenericIntegrationTypeSupplier implements JobDefinitionParameterSupplier {
        @Override
        public JobDefinitionParameters getJobDefinitionParameters() {
            return null;
        }

        @Override
        public IntegrationType getIntegrationType() {
            return IntegrationType.JIRA;
        }

        @Override
        public JobType getJobType() {
            return JobType.GENERIC_INTEGRATION_JOB;
        }

        @Override
        public ShouldTakeFull shouldTakeFull(DbJobInstance jobInstance, DbJobDefinition jobDefinition, Instant now) {
            return ShouldTakeFull.of(false);
        }

        @Override
        public @NonNull String getEtlProcessorName() {
            return "GenericIntegrationJob";
        }
    }

    public static class IngestionProcessingSupplier implements JobDefinitionParameterSupplier {
        @Override
        public JobDefinitionParameters getJobDefinitionParameters() {
            return null;
        }

        @Override
        public IntegrationType getIntegrationType() {
            return IntegrationType.GITHUB;
        }

        @Override
        public JobType getJobType() {
            return JobType.INGESTION_RESULT_PROCESSING_JOB;
        }

        @Override
        public ShouldTakeFull shouldTakeFull(DbJobInstance jobInstance, DbJobDefinition jobDefinition, Instant now) {
            return ShouldTakeFull.of(false);
        }

        @Override
        public @NonNull String getEtlProcessorName() {
            return "GithubEtlProcessor";
        }
    }
}