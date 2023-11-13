package io.levelops.commons.utils.dora;

import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.web.exceptions.BadRequestException;

public class DoraValidationsUtils {

    public static void validateVelocityConfig(ScmPrFilter filter, VelocityConfigDTO velocityConfigDTO) throws BadRequestException {
        if(velocityConfigDTO == null) {
            throw new BadRequestException("Error computing DORA metric, please provide velocity config..");
        }

        if((ScmPrFilter.CALCULATION.deployment_frequency).equals(filter.getCalculation())
                && (velocityConfigDTO.getDeploymentFrequency()== null
                || velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters() == null
                || velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency() == null)) {
            throw new BadRequestException("Error computing DORA metric, please configure Deployment Frequency properly for WorkFlow profile : " + velocityConfigDTO.getName());
        }

        if((ScmPrFilter.CALCULATION.failure_rate).equals(filter.getCalculation())
                && (velocityConfigDTO.getChangeFailureRate()== null
                || velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters() == null
                || velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment() == null
                || (!velocityConfigDTO.getChangeFailureRate().getIsAbsoulte()
                && velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment() == null))) {
            throw new BadRequestException("Error computing DORA metric, please configure Change Failure Rate properly for WorkFlow profile : " + velocityConfigDTO.getName());
        }

        if((ScmPrFilter.CALCULATION.lead_time_for_changes).equals(filter.getCalculation())
                && velocityConfigDTO.getLeadTimeForChanges()== null ) {
            throw new BadRequestException("Error computing DORA metric, please configure Lead Time For Changes properly for WorkFlow profile : " + velocityConfigDTO.getName());
        }
        if((ScmPrFilter.CALCULATION.mean_time_to_recover).equals(filter.getCalculation())
                && velocityConfigDTO.getMeanTimeToRestore()== null) {
            throw new BadRequestException("Error computing DORA metric, please configure Mean Time To Restore properly for WorkFlow profile : " + velocityConfigDTO.getName());
        }

    }
}
