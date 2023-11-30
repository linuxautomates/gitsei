import { Icon } from "antd";
import { filterFieldType, sectionSelectedFilterType } from "configurations/configuration-types/OUTypes";
import { useIntegrationFilterConfiguration } from "configurations/pages/Organization/Filters/useIntegrationFilterConfiguration";
import OrgUnitIntegrationFilterField from "configurations/pages/Organization/organization-unit/OrgUnitIntegrationFilterField";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { cloneDeep } from "lodash";
import React, { useEffect, useMemo } from "react";
import { AntButton, AntText, AntTooltip } from "shared-resources/components";
import { CICD_FITER_NOTE } from "../constant";
import "./CICDFilterComponent.scss";
import { integrationParameters } from "configurations/constants";

interface CICDFilterComponentProps {
  onChange: (updatedConfig: any) => void;
  integrationIds?: string[];
  integrationApplication: string;
  filterConfig?: sectionSelectedFilterType[];
  titleName: string;
  additionalFilter?: any;
  removeProjectFilter?: boolean;
  integrationType?: string;
  selectedApplication?: string;
}

const CICDFilterComponent: React.FC<CICDFilterComponentProps> = ({
  integrationApplication,
  integrationIds,
  filterConfig,
  onChange,
  titleName,
  additionalFilter,
  removeProjectFilter,
  integrationType,
  selectedApplication
}) => {
  const [allFilterConfig, selectedIntegrationFilters] = useIntegrationFilterConfiguration(
    integrationApplication,
    integrationIds?.length ? integrationIds : [""],
    {
      removeCicdJobNameFilter: true,
      removeProjectFilter,
      integrationType,
      selectedApplication
    }
  );

  useEffect(() => {
    // THIS WILL USE FOR FILTER WHO HAVE CHILD FILTER IN UPDATE CASE WHEN ONLY PARENT FILTER IS ADDED & CHILD FILTER IS NOT ADDED IN THAT CASE WE HAVE TO SHOW BUTTON TO ADD CHILD FILTER
    const nfilters = cloneDeep(filterConfig);
    if (nfilters && nfilters.length) {
      const updateFilterData = nfilters.map((filterData: sectionSelectedFilterType) => {
        const childKeysData = selectedIntegrationFilters.reduce((acc: any, data: any) => {
          if (data.value === filterData.key && data?.childKeys && data?.childKeys.length > 0) {
            const childUpdatedData = data.childKeys.map((data: any) => {
              const checkExistingChid = filterData?.childKeys
                ? filterData?.childKeys.findIndex((childData: any) => childData.key === data.value)
                : -1;
              if (checkExistingChid === -1) {
                return { key: data.value, value: "", param: "", buttonName: data.buttonName, showButton: true };
              } else {
                return filterData?.childKeys[checkExistingChid];
              }
            });
            acc.push(childUpdatedData);
          }
          return acc;
        }, []);
        if (childKeysData && childKeysData.length > 0) {
          filterData.childKeys = childKeysData[0];
        }
        return filterData;
      });
      onChange(updateFilterData);
    }
  }, [titleName]);

  const handleIntegrationFieldChanges = (type: filterFieldType, index: number, value: any, parentKeyData?: any) => {
    const nfilters = cloneDeep(filterConfig);
    if (nfilters && nfilters.length) {
      let nField: sectionSelectedFilterType;
      if (parentKeyData && parentKeyData?.childKeys && parentKeyData.childKeys.length > 0) {
        nField = cloneDeep(nfilters[parentKeyData.parentIndex]);

        let childFiledData = cloneDeep(nField.childKeys[index]);
        (childFiledData as any)[type] = value;
        if (type === "key") {
          childFiledData.param = "";
          childFiledData.value = "";
        }
        if (type === "param") {
          childFiledData.value = "";
        }
        nField.childKeys[index] = childFiledData;
        nfilters[parentKeyData.parentIndex] = nField;
      } else {
        nField = cloneDeep(nfilters[index]);

        (nField as any)[type] = value;
        if (type === "key") {
          nField.param = "";
          nField.value = "";
        }
        if (type === "param") {
          nField.value = "";
        }

        const childKeysData = selectedIntegrationFilters.reduce((acc: any, data: any) => {
          if (data.value === nField.key && data?.childKeys && data?.childKeys.length > 0) {
            let childData = data.childKeys.map((data: any) => {
              return { key: data.value, value: "", param: "", buttonName: data.buttonName, showButton: true };
            });
            acc.push(childData);
          }
          return acc;
        }, []);

        if (childKeysData && childKeysData.length > 0) {
          nField.childKeys = childKeysData[0];
        }
        nfilters[index] = nField;
      }
      onChange(nfilters);
    }
  };

  const integrationFilters = useMemo(() => {
    let supportedFilters: string[] | undefined = [
      "job_statuses",
      "instance_names",
      "projects",
      "cicd_user_ids",
      "stage_name",
      "step_name"
    ];
    switch (integrationApplication) {
      case IntegrationTypes.JENKINS:
        supportedFilters = ["job_statuses", "instance_names", "cicd_user_ids", "stage_name", "step_name"];
        break;
      case IntegrationTypes.AZURE:
        supportedFilters = ["projects", "job_statuses", "instance_names", "stage_name", "step_name"];
        break;
      case IntegrationTypes.HARNESSNG:
        supportedFilters = undefined;
        break;
    }
    return selectedIntegrationFilters.filter((intFilter: any) =>
      supportedFilters ? supportedFilters.includes(intFilter.value) : true
    );
  }, [selectedIntegrationFilters]);

  const checkDuplicateValidation = (currentKey: string) => {
    if (filterConfig) {
      const selectedFilterKeys = filterConfig.reduce((acc: any, data: any) => {
        if (data.key && data.key !== currentKey) {
          acc.push(data.key);
        }
        return acc;
      }, []);

      const finalIntigrationList = (integrationFilters || []).reduce((acc: any, data: any) => {
        if (!selectedFilterKeys.includes(data.value)) {
          acc.push(data);
        }
        return acc;
      }, []);
      return finalIntigrationList;
    }
  };

  const getChildFilterData = (currentKey: string, parentKey: string) => {
    if (filterConfig) {
      let finalIntigrationList = (integrationFilters || [])
        .filter((data: any) => data.value === parentKey && data.childKeys)
        .map((child: any) => child.childKeys)[0]
        .filter((childFilter: any) => childFilter.value === currentKey);
      return finalIntigrationList;
    }
  };

  const handleRemoveFilter = (index: number, parentKeyData?: any) => {
    const cloneConfig = cloneDeep(filterConfig);
    if (cloneConfig) {
      if (parentKeyData && parentKeyData?.childKeys && parentKeyData.childKeys.length > 0) {
        cloneConfig[parentKeyData.parentIndex].childKeys[index] = {
          ...cloneConfig[parentKeyData.parentIndex].childKeys[index],
          value: "",
          param: "",
          showButton: true
        };
      } else {
        cloneConfig.splice(index, 1);
      }
      onChange(cloneConfig);
    }
  };

  const addFilter = () => {
    onChange(filterConfig ? [...filterConfig, { key: "", param: "", value: "" }] : [{ key: "", param: "", value: "" }]);
  };

  const handleAddChildFilter = (index: number, childIndex: number) => {
    const nfilters = cloneDeep(filterConfig);
    if (nfilters && nfilters.length) {
      let nField: sectionSelectedFilterType = cloneDeep(nfilters[index]);
      nField.childKeys[childIndex].showButton = false;

      nfilters[index] = nField;
      onChange(nfilters);
    }
  };

  return (
    <div className="cicd-filter">
      <AntText strong className="d-block">
        Filters
      </AntText>
      <div className="my-5">
        <AntText className="filter-note-desc">{CICD_FITER_NOTE}</AntText>
      </div>
      {filterConfig &&
        filterConfig.length > 0 &&
        filterConfig.map((field: sectionSelectedFilterType, index: number) => {
          return (
            <>
              <OrgUnitIntegrationFilterField
                integrationApplication={integrationApplication}
                key={`${field.key}_${index}`}
                apiLoading={false}
                apiRecords={checkDuplicateValidation(field.key)}
                index={index}
                field={field}
                handleRemoveFilter={handleRemoveFilter}
                handleFieldChange={handleIntegrationFieldChanges}
                allFiltersConfig={allFilterConfig}
                integrationIds={integrationIds?.length ? integrationIds : []}
                hideFilterText={true}
                labelCase={"none"}
                integrationType="cicd"
                pageName={"workflow_profile"}
                additionalFilter={additionalFilter}
              />

              {field?.childKeys &&
                field?.childKeys.length > 0 &&
                field?.childKeys.map((childRender: any, childIndex: number) => {
                  let disabledAddChildFilterButton =
                    field.value && field.value.length > 0 && field.param !== integrationParameters.DOES_NOT_EQUAL;
                  return childRender.showButton ? (
                    <div className="child-filter-add-button">
                      <AntTooltip
                        style={{ color: "#7E7E7E" }}
                        title={disabledAddChildFilterButton ? "" : "Please add parent filter value."}>
                        <AntButton
                          style={disabledAddChildFilterButton ? { color: "#2967dd" } : { color: "#7E7E7E" }}
                          type="link"
                          onClick={disabledAddChildFilterButton ? () => handleAddChildFilter(index, childIndex) : ""}>
                          <Icon type="plus-circle" />
                          {childRender.buttonName}
                        </AntButton>
                      </AntTooltip>
                      <br></br>
                    </div>
                  ) : (
                    <OrgUnitIntegrationFilterField
                      integrationApplication={integrationApplication}
                      key={`${childRender}_${index}`}
                      apiLoading={false}
                      apiRecords={getChildFilterData(childRender.key, field.key)}
                      index={childIndex}
                      field={childRender}
                      handleRemoveFilter={handleRemoveFilter}
                      handleFieldChange={handleIntegrationFieldChanges}
                      allFiltersConfig={allFilterConfig}
                      integrationIds={integrationIds?.length ? integrationIds : []}
                      hideFilterText={true}
                      labelCase={"none"}
                      integrationType="cicd"
                      pageName={"workflow_profile"}
                      additionalFilter={additionalFilter}
                      childComponent={true}
                      parentKeyData={{ ...field, parentIndex: index }}
                    />
                  );
                })}
            </>
          );
        })}
      <AntButton type="link" onClick={addFilter}>
        <Icon type="plus-circle" />
        {`Add ${titleName.slice(0, -1)} filters`}
      </AntButton>
    </div>
  );
};

export default CICDFilterComponent;
