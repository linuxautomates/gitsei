import React, { useCallback, useMemo } from "react";
import { Form } from "antd";
import { get, isArray } from "lodash";
import { AntSelect } from "shared-resources/components";
import widgetConstants from "../../../constants/widgetConstants";
import "./OUFilters.style.scss";
import { toTitleCase } from "utils/stringUtils";
import { OUFiltersMapping } from "../../../constants/ou-filters";
import { getFieldValueOrString } from "utils/commonUtils";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface OUFiltersComponentProps {
  metaData: any;
  onMetadataChange?: (val: any, type: string) => void;
  reportType: string;
  customFieldData?: any[];
}

const OUFiltersComponent: React.FC<OUFiltersComponentProps> = (props: OUFiltersComponentProps) => {
  const { reportType, metaData, onMetadataChange, customFieldData } = props;

  const HELPER_TEXT = "SEI uses the default field for Collection base aggregations. Override Collection fields here.";

  const application = get(widgetConstants, [reportType, "application"], undefined);
  const supportedFilters = get(widgetConstants, [reportType, "supported_filters"], undefined);

  const filtersByApplications = useMemo(() => {
    if (application === IntegrationTypes.JIRA) {
      return {
        jira: OUFiltersMapping.jira
      };
    } else if (application === IntegrationTypes.AZURE) {
      if (reportType === "azure_lead_time_single_stat") {
        return {
          azure_devops: OUFiltersMapping.azure_devops,
          github: OUFiltersMapping.github
        };
      }
      return {
        azure_devops: OUFiltersMapping.azure_devops
      };
    } else if (application === IntegrationTypes.GITHUB) {
      return {
        github: OUFiltersMapping.github
      };
    } else if ([IntegrationTypes.JENKINSGITHUB, IntegrationTypes.JENKINS].includes(application)) {
      return {
        jenkins: OUFiltersMapping.jenkins
      };
    } else if (application === IntegrationTypes.GITHUBJIRA) {
      return {
        jira: OUFiltersMapping.jira,
        github: OUFiltersMapping.github
      };
    } else {
      return {
        [application]: supportedFilters?.values || []
      };
    }
  }, [reportType]);

  const handleChange = useCallback(
    (application: string) => {
      return (value: any, options: any) => {
        const ouFilters = get(metaData, "ou_user_filter_designation", {});
        const _data = {
          ...ouFilters,
          [application]: value
        };
        onMetadataChange?.(_data, "ou_user_filter_designation");
      };
    },
    [metaData, onMetadataChange]
  );

  const label = useMemo(
    () => (
      <div className="ou-override-label">
        <span>OU Unit Overrides</span>
        <span className="ou-override-helper-text">{HELPER_TEXT}</span>
      </div>
    ),
    []
  );

  const renderContent = useMemo(() => {
    return Object.keys(filtersByApplications || {}).map((app: string) => {
      let defaultValue: string[] = [];
      let options = get(filtersByApplications, app, []).map((item: any) => {
        defaultValue = item?.defaultValue || [];
        return {
          label: toTitleCase(getFieldValueOrString(item, "label")),
          value: getFieldValueOrString(item, "value"),
          disabled: item.disabled ?? false
        };
      });
      const value = get(metaData, ["ou_user_filter_designation", app], []);
      return (
        <Form.Item key={app} label={toTitleCase(app)}>
          <AntSelect
            showArrow
            value={value.length === 0 ? defaultValue : value}
            options={options}
            mode="multiple"
            onChange={handleChange(app)}
          />
        </Form.Item>
      );
    });
  }, [metaData, reportType, onMetadataChange, customFieldData]);

  return (
    <>
      <Form.Item key="ou_unit_override" label={label} className="ou-unit-override">
        <div className="ou-filters-container">{renderContent}</div>
      </Form.Item>
    </>
  );
};

export default React.memo(OUFiltersComponent);
