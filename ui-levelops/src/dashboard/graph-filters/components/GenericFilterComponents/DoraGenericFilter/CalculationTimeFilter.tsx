import { WORKFLOW_PROFILE_TABS } from "configurations/pages/lead-time-profiles/containers/workflowDetails/components/constant";
import UniversalTimeBasedFilter, {
  UniversalTimeBasedFilterProps
} from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { DORA_REPORT_TO_KEY_MAPPING } from "dashboard/graph-filters/components/helper";
import { get, startCase } from "lodash";
import React, { FC, useEffect, useMemo } from "react";
import { useSelector } from "react-redux";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { AntText } from "shared-resources/components";
import { DORA_CALCULATION_FIELD_LABEL, getCalculationField } from "./constants";

export interface CalculationTimeFilterProps extends UniversalTimeBasedFilterProps {
  report: string;
}

const CalculationTimeFilter: FC<CalculationTimeFilterProps> = props => {
  const { filterProps, handleMetadataChange, report } = props;
  const selectedOUState = useSelector(getSelectedOU);
  const workflowProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU: selectedOUState.id });
  const reportNameKey = DORA_REPORT_TO_KEY_MAPPING[report];

  const calculationField = useMemo(
    () => getCalculationField(workflowProfile, reportNameKey),
    [workflowProfile, reportNameKey]
  );

  const label = DORA_CALCULATION_FIELD_LABEL[calculationField];

  useEffect(() => {
    if (handleMetadataChange) {
      handleMetadataChange(calculationField, "calculation_field");
    }
  }, []);

  const updatedFilterProps = {
    ...filterProps,
    label
  };

  return (
    <>
      {workflowProfile && (
        <>
          <AntText strong>{startCase(reportNameKey.replaceAll("_", " "))} calculation</AntText>
          <UniversalTimeBasedFilter {...props} filterProps={updatedFilterProps} />
        </>
      )}
    </>
  );
};

export default CalculationTimeFilter;
