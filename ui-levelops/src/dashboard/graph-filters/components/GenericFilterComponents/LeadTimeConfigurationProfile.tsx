import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Form, Icon, Select, Tooltip } from "antd";
import { get } from "lodash";
import { useDispatch } from "react-redux";
import { restapiClear, genericList } from "reduxConfigs/actions/restapi";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { VELOCITY_CONFIG_LIST_ID, VELOCITY_CONFIGS } from "reduxConfigs/selectors/velocityConfigs.selector";
import {
  AntSelect,
  AntButton,
  AntTag,
  CustomSelect,
  NewCustomFormItemLabel,
  SvgIcon
} from "shared-resources/components";
import { getStageDurationComputationOptions } from "../helper";
import { stringSortingComparator } from "../sort.helper";
import {
  CREATE_JIRA_ONLY_PROFILE,
  ITEM_TEST_ID,
  NOT_JIRA_ONLY_PROFILE_MESSAGE,
  REPORT_NOT_ALLOWED_FOR_COMPUTATIONAL_MODEL,
  SELETE_JIRA_ONLY_PROFILE_NOTE,
  SELETE_JIRA_ONLY_PROFILE_NOT_FIND,
  SELETE_JIRA_ONLY_PROFILE_TITLE,
  REPORT_NOT_ALLOWED_FOR_CONFIGURE_PROFILE_IN_WIDGET
} from "../Constants";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { JIRA_MANAGEMENT_TICKET_REPORT, LEAD_TIME_REPORTS } from "dashboard/constants/applications/names";
import { getBaseUrl, VELOCITY_CONFIGS_ROUTES } from "constants/routePaths";
import { useHistory } from "react-router-dom";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { TriggerEventType } from "classes/RestVelocityConfigs";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import widgetConstants from "dashboard/constants/widgetConstants";
import { LEAD_TIME_MTTR_REPORTS } from "dashboard/constants/applications/names";

const { Option } = Select;

interface LeadTmeConfigFilterProps {
  filters: any;
  onFilterValueChange: (value: any, key: string) => void;
  filterProps: LevelOpsFilter;
  report?: string;
  metadata: any;
  handleMetadataChange?: (val: any, type: string) => void;
  queryParamDashboardOUId?: string[];
}

const LeadTimeConfigProfileFilter: React.FC<LeadTmeConfigFilterProps> = props => {
  const {
    filters,
    metadata,
    onFilterValueChange,
    handleMetadataChange: onMetadataChange,
    filterProps,
    report,
    queryParamDashboardOUId
  } = props;
  const { label, beKey } = filterProps;
  const uri = VELOCITY_CONFIGS;
  const method = "list";
  const uuid = VELOCITY_CONFIG_LIST_ID;

  const dispatch = useDispatch();
  const history = useHistory();

  const [searchFilterQuery, setSearchFilterQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<any[]>([]);
  const [showLeadTimeAdvancedSettings, setShowLeadTimeAdvancedSettings] = useState<boolean>(false);
  // ENTITLEMENT FOR JIRA RELESE PROFILE
  const velocityJiraReleaseProfile = useHasEntitlements(
    Entitlement.VELOCITY_JIRA_RELEASE_PROFILE,
    EntitlementCheckType.AND
  );

  const genericSelector = useParamSelector(getGenericRestAPISelector, {
    uri,
    method,
    uuid
  });

  const queryParamOU = queryParamDashboardOUId ? queryParamDashboardOUId[0] : "";
  const workspaceOuProfilestate = useParamSelector(workflowProfileDetailSelector, { queryParamOU });

  const isDoraReportType = useMemo(() => {
    if (LEAD_TIME_MTTR_REPORTS.includes(report as any)) {
      return true;
    }
    return false;
  }, [report]);

  const moreFilters = useMemo(() => {
    if (searchFilterQuery) {
      return {
        partial: {
          name: {
            $contains: searchFilterQuery
          }
        }
      };
    }
    return {};
  }, [searchFilterQuery]);

  const checkforJiraProfileOnly = useMemo(() => {
    if (
      velocityJiraReleaseProfile &&
      (report === JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT ||
        report === LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT)
    ) {
      return true;
    }
    return false;
  }, [report, velocityJiraReleaseProfile]);

  const defaultConfigId = useMemo(() => {
    if (isDoraReportType) {
      return undefined;
    }
    const defaultConfig = data.find((item: any) => !!item.default_config);
    if (defaultConfig) {
      return defaultConfig.id;
    }
    return undefined;
  }, [data, isDoraReportType]);

  const configId = filters?.velocity_config_id || defaultConfigId;

  useEffect(() => {
    let jiraFilter = {};
    if (report === JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT) {
      jiraFilter = { jira_only: true };
    }
    dispatch(restapiClear(uri, method, "-1"));
    dispatch(genericList(uri, method, { filter: { ...(moreFilters || {}), ...jiraFilter } }, null, uuid));
    setLoading(true);
  }, [searchFilterQuery]);

  useEffect(() => {
    if (loading) {
      const getDoraLeadTimeMeanTimeData = get(
        widgetConstants,
        [report as string, "getDoraLeadTimeMeanTimeData"],
        undefined
      );
      if (getDoraLeadTimeMeanTimeData) {
        const getDoraLeadTimeMeanTime = getDoraLeadTimeMeanTimeData({ workspaceOuProfilestate });
        setData(getDoraLeadTimeMeanTime);
      } else {
        const { loading, error } = genericSelector;
        if (!loading && !error && Object.keys(genericSelector?.data || {}).length) {
          let notAllowJiraProfile = [
            JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT,
            LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT
          ].includes(report as any)
            ? false
            : true;

          let records = get(genericSelector, ["data", "records"], []).filter((record: any) => record?.is_new === false);
          if (notAllowJiraProfile) {
            records = records.filter((record: any) => !record?.jira_only);
          }
          setData(records);
          setLoading(false);
        }
      }
    }
  }, [genericSelector, workspaceOuProfilestate, report]);

  const tagStyle = useMemo(() => ({ marginLeft: "8px" }), []);

  const modifiedOptions = useMemo(() => {
    if (loading) {
      return [];
    }

    return data.sort(stringSortingComparator("name")).map((item: any) => (
      <Option key={item.id} value={item.id} disabled={checkforJiraProfileOnly && !item.jira_only ? true : false}>
        {item.name}
        {!!item.default_config && (
          <AntTag color="purple" style={tagStyle}>
            Default
          </AntTag>
        )}
        {checkforJiraProfileOnly && !item.jira_only && (
          <Tooltip title={NOT_JIRA_ONLY_PROFILE_MESSAGE}>
            &nbsp;
            <Icon type="question-circle-o" />
          </Tooltip>
        )}
      </Option>
    ));
  }, [loading, data, report, checkforJiraProfileOnly]);

  const stages = useMemo(() => {
    let selectedConfig;
    if (isDoraReportType) {
      selectedConfig = data;
    } else {
      selectedConfig = data.find((item: any) => item.id === configId);
    }

    if (selectedConfig) {
      const configStages = [
        ...(selectedConfig.pre_development_custom_stages || []),
        ...(selectedConfig.fixed_stages || []),
        ...(selectedConfig.post_development_custom_stages || [])
      ];

      let stageData = configStages.reduce((acc: any, obj: any) => {
        if (report === JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT) {
          if (obj?.event?.type !== TriggerEventType.JIRA_RELEASE) acc.push(obj.name);
        } else {
          acc.push(obj.name);
        }
        return acc;
      }, []);

      return (stageData = [
        JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT,
        LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT
      ].includes(report as any)
        ? [...stageData, "Other"]
        : stageData);
    }

    return [];
  }, [data, configId, report, isDoraReportType]);

  const renderConfigField = useMemo(
    () => (
      <Form.Item
        key="lead_time_configuration_profile"
        label={<NewCustomFormItemLabel label={label} />}
        data-filterselectornamekey={`${ITEM_TEST_ID}-lead-config-profile`}
        data-filtervaluesnamekey={`${ITEM_TEST_ID}-lead-config-profile`}>
        {checkforJiraProfileOnly && (
          <>
            <p className="widget-velocity-detail">{SELETE_JIRA_ONLY_PROFILE_TITLE}</p>
            <p className="widget-velocity-note">{SELETE_JIRA_ONLY_PROFILE_NOTE}</p>
          </>
        )}
        <AntSelect
          dropdownTestingKey={`${ITEM_TEST_ID}-lead-config-profile_dropdown`}
          placeholder={label}
          showArrow
          showSearch
          filterOption={false}
          loading={loading}
          value={configId}
          onChange={(value: any, options: any) => onFilterValueChange(value, beKey)}
          onBlur={(e: any) => setSearchFilterQuery("")}
          onSearch={(value: any) => {
            setSearchFilterQuery(value);
          }}>
          {modifiedOptions}
        </AntSelect>
      </Form.Item>
    ),
    [loading, data, filters, report, checkforJiraProfileOnly]
  );

  const handleRedirectToVelocityProfileAddPage = useCallback(() => {
    history.push(`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}?configId=new&profileType=old&jira_only=${true}`);
  }, [history]);

  const renderCreateProfile = useMemo(() => {
    if (isDoraReportType) {
      return "";
    }

    const jiraOnlyProfile = data.find((item: any) => item.jira_only);
    if (!jiraOnlyProfile && !loading) {
      return (
        <>
          <p className="widget-velocity-create-profile">{SELETE_JIRA_ONLY_PROFILE_NOT_FIND} </p>
          <p className="widget-velocity-create-profile">{CREATE_JIRA_ONLY_PROFILE}</p>
          <AntButton
            className={"velocity-create-profile-button"}
            onClick={() => handleRedirectToVelocityProfileAddPage()}>
            <span className="widget-velocity-create-text">+ Create a Profile </span>
            <span className="widget-velocity-create-btn">
              <SvgIcon className="widget-velocity-create-btn-icon" icon="externalLink" />
            </span>
          </AntButton>
          <br></br>
        </>
      );
    }
    return "";
  }, [data, handleRedirectToVelocityProfileAddPage, loading, isDoraReportType]);

  const renderShowHideBtn = useMemo(
    () => (
      <AntButton
        className={"velocity-advanced-settings-button"}
        onClick={() => setShowLeadTimeAdvancedSettings(state => !state)}>
        {showLeadTimeAdvancedSettings ? "- Hide Advanced Settings" : "+ Show Advanced Settings"}
      </AntButton>
    ),
    [showLeadTimeAdvancedSettings]
  );

  const renderAdvanceSettings = useMemo(() => {
    if (!showLeadTimeAdvancedSettings) {
      return null;
    }

    return (
      <>
        <Form.Item
          label={
            <NewCustomFormItemLabel
              label="Exclude Stages"
              withInfo={{
                showInfo: true,
                description: "Exclude selected stages from Lead Time computation"
              }}
            />
          }
          data-filterselectornamekey={`${ITEM_TEST_ID}-exclude-stages`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-exclude-stages`}>
          <CustomSelect
            dataFilterNameDropdownKey={`${ITEM_TEST_ID}-exclude-stages_dropdown`}
            sortOptions
            mode="multiple"
            createOption={true}
            labelCase={"none"}
            options={stages}
            showArrow={true}
            value={metadata?.hide_stages || []}
            onChange={(value: any) => onMetadataChange?.(value, "hide_stages")}
          />
        </Form.Item>
        {!REPORT_NOT_ALLOWED_FOR_COMPUTATIONAL_MODEL.includes(report as string) && (
          <Form.Item
            key="limit_to_only_applicable_data"
            label={<NewCustomFormItemLabel label="Computation Model" />}
            data-filterselectornamekey={`${ITEM_TEST_ID}-computational-model`}
            data-filtervaluesnamekey={`${ITEM_TEST_ID}-computational-model`}>
            <AntSelect
              showArrow
              dropdownTestingKey={`${ITEM_TEST_ID}-computational-model_dropdown`}
              value={!!filters?.limit_to_only_applicable_data}
              options={getStageDurationComputationOptions(filters?.calculation).sort(stringSortingComparator("label"))}
              mode="single"
              onChange={(value: any, options: any) => onFilterValueChange(value, "limit_to_only_applicable_data")}
            />
          </Form.Item>
        )}
      </>
    );
  }, [filters, showLeadTimeAdvancedSettings, data, metadata]);

  return (
    <>
      {!REPORT_NOT_ALLOWED_FOR_CONFIGURE_PROFILE_IN_WIDGET.includes(report as string) && renderConfigField}
      {checkforJiraProfileOnly && renderCreateProfile}
      {renderShowHideBtn}
      {renderAdvanceSettings}
    </>
  );
};

export default LeadTimeConfigProfileFilter;
