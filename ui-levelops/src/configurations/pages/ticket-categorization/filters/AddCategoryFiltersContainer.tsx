import { Empty } from "antd";
import CategoryFiltersComponentWrapper from "dashboard/components/dashboard-application-filters/category-filters/CategoryFiltersComponentWrapper";
import { AZURE_CUSTOM_FIELD_PREFIX, valuesToFilters } from "dashboard/constants/constants";
import { ReportsApplicationType } from "dashboard/constants/helper";
import { SearchInput } from "dashboard/pages/dashboard-drill-down-preview/components/SearchInput.component";
import { getFilterValue } from "helper/widgetFilter.helper";
import { cloneDeep, get, uniqBy, uniqueId } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { OrgCustomConfigData } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntText } from "shared-resources/components";
import { isSanitizedValue } from "utils/commonUtils";
import { getEIAzureCustomFieldConfig, getEICustomDataParams, getEICustomFieldConfig } from "../helper/filters.helper";
import { INTEGRAION_LIST_ID } from "../constants/ticket-categorization.constants";

interface AddCategoryFiltersProps {
  filtersConfigs: LevelOpsFilter[];
  filters: any;
  orderedFilters: string[];
  setOrderedFilters: any;
  partialFiltersErrors: any;
  ignoreFilterKeys?: string[];
  metadata: any;
  integrationIds: string[];
  application: string;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeFilterChange: (key: string, value: boolean) => void;
  handlePartialValueChange: (key: string, value: any) => void;
  onFilterRemoved: (key: string) => void;
  handleTimeRangeTypeChange: (key: string, value: any) => void;
  handleTimeRangeFilterValueChange: (value: string, type?: any, rangeType?: string, isCustom?: boolean) => void;
}

const AddCategoryFiltersContainer: React.FC<AddCategoryFiltersProps> = (props: AddCategoryFiltersProps) => {
  const {
    application,
    integrationIds,
    filtersConfigs,
    filters,
    orderedFilters,
    metadata,
    onFilterRemoved,
    onFilterValueChange,
    setOrderedFilters,
    partialFiltersErrors,
    handlePartialValueChange,
    handleTimeRangeFilterValueChange,
    handleTimeRangeTypeChange,
    onExcludeFilterChange
  } = props;
  const [customFieldsRecords, setCustomFieldsRecords] = useState<any[]>();
  const [showFiltersPopOver, setShowFiltersPopOver] = useState<boolean>(false);
  const [fieldsLoading, setFieldsLoading] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [activePopKey, setActivePopKey] = useState<string | undefined>();
  const [dropDownOptions, setDropDownOptions] = useState<any[]>([]);
  const { fieldUri, fieldId, integConfigUri, integConfigId } = getEICustomDataParams(application);
  const integConfigListState = useParamSelector(getGenericUUIDSelector, {
    uri: integConfigUri,
    method: "list",
    uuid: integConfigId
  });
  const integFieldsListState = useParamSelector(getGenericUUIDSelector, {
    uri: fieldUri,
    method: "list",
    uuid: fieldId
  });

  const integrationState = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "list",
    uuid: INTEGRAION_LIST_ID
  });

  const dispatch = useDispatch();

  const transformCustomFieldsData = (customFields: any[], fieldList: any[]) => {
    if (application === ReportsApplicationType.AZURE_DEVOPS) {
      return (customFields ?? []).map((custom: { key: string }) => {
        const fieldListCustom: { key: string } = (fieldList ?? []).find((field: { key: string }) =>
          field?.key?.includes(custom?.key)
        );
        if (fieldListCustom) {
          const initialFieldKey = fieldListCustom.key?.replace(AZURE_CUSTOM_FIELD_PREFIX, "");
          if (initialFieldKey === custom.key) {
            return {
              ...(custom ?? {}),
              key: fieldListCustom?.key,
              metadata: {
                transformed: AZURE_CUSTOM_FIELD_PREFIX
              }
            };
          }
        }
        return custom;
      });
    }
    return customFields;
  };

  const handleActivePopkey = (key: string | undefined) => setActivePopKey(key);

  const allFilterConfig = useMemo(() => {
    const fieldLoading = get(integFieldsListState, ["loading"], true);
    const configLoading = get(integConfigListState, ["loading"], true);
    let fieldsData = [];
    let ConfigData = [];
    if (!fieldLoading && !configLoading) {
      setFieldsLoading(false);
      fieldsData = get(integFieldsListState, ["data"], []);
      ConfigData = get(integConfigListState, ["data"], []);
      if (Array.isArray(ConfigData)) {
        ConfigData = transformCustomFieldsData(ConfigData, fieldsData);
      }
      ConfigData = uniqBy(ConfigData, "key");
      setCustomFieldsRecords(ConfigData);
      if (Array.isArray(fieldsData) && Array.isArray(ConfigData)) {
        const customFieldConfig =
          application === ReportsApplicationType.AZURE_DEVOPS
            ? getEIAzureCustomFieldConfig(ConfigData, fieldsData)
            : getEICustomFieldConfig(ConfigData, fieldsData);
        return [...filtersConfigs, ...customFieldConfig];
      }
    }
    return filtersConfigs;
  }, [filtersConfigs, integConfigListState, integFieldsListState]);

  useEffect(() => {
    const options = allFilterConfig?.map((config: LevelOpsFilter) => {
      let selected = isSanitizedValue(
        getFilterValue(
          filters,
          config.beKey,
          true,
          config?.parentKey ? [config.parentKey as string] : undefined,
          config.partialKey,
          config.excludeKey
        )?.value
      );

      let isFilterHidden = false;
      const isSelected = config.isSelected;
      if (isSelected) {
        if (typeof isSelected === "function") {
          selected = isSelected({ filters, metadata: props.metadata });
        } else {
          selected = isSelected;
        }
      }
      // by default all the required filters should render
      // they don't have the delete option too
      if (config.required) {
        selected = true;
      }

      if (config.hideFilter) {
        if (typeof config.hideFilter === "function") isFilterHidden = config.hideFilter({ filters });
        else isFilterHidden = config.hideFilter;
      }

      return {
        selected,
        isFilterHidden,
        label: config.label,
        key: config.beKey
      };
    });
    setDropDownOptions(options);
  }, [allFilterConfig, filters]);

  const mappedDropdownOptions = useMemo(
    () =>
      dropDownOptions.filter(obj => !obj.selected && obj?.label?.toLowerCase().includes(searchQuery?.toLowerCase())),
    [dropDownOptions, searchQuery]
  );

  const removeFilterOptionSelected = (key: any) => {
    let _key = key;
    if (typeof key === "object") {
      _key = Object.keys(key)[0];
    }

    _key = (valuesToFilters as any)[_key] || _key;

    if (_key.includes("customfield") || _key?.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      _key = _key.split("@")[0];
    }
    const index = dropDownOptions.findIndex(filter => filter.key === _key);
    if (index !== -1) {
      let updatedOptions = [...dropDownOptions];
      updatedOptions[index].selected = false;
      setDropDownOptions(updatedOptions);
      onFilterRemoved && onFilterRemoved(_key);
      setOrderedFilters(orderedFilters.filter(orderedKey => orderedKey !== _key));
    }
  };

  const filterOptionSelected = (key: string) => {
    const index = dropDownOptions.findIndex(filter => filter.key === key);
    if (index !== -1) {
      let updatedOptions = [...dropDownOptions];
      updatedOptions[index].selected = true;
      setDropDownOptions(updatedOptions);
      setShowFiltersPopOver(false);
      const newOrder = [...orderedFilters, key];
      setOrderedFilters(newOrder);
      setSearchQuery("");
    }
  };

  const menu = useMemo(() => {
    return (
      <div style={{ width: "23.5rem" }}>
        <SearchInput value={searchQuery} onChange={(query: string) => setSearchQuery(query)} />
        <div className="widget_defaults_reports_list">
          {mappedDropdownOptions.map(filter => (
            <div className={"widget_defaults_reports_list_name"} onClick={() => filterOptionSelected(filter.key)}>
              <AntText className={"widget_defaults_reports_list_name_select text-uppercase"}>{filter.label}</AntText>
            </div>
          ))}
          {mappedDropdownOptions.length === 0 && (
            <div>
              <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />
            </div>
          )}
        </div>
      </div>
    );
  }, [mappedDropdownOptions, searchQuery, filters]);

  const handleSetShowFiltersPopOver = () => {
    setShowFiltersPopOver(!showFiltersPopOver);
  };

  const handleVisibleChange = useCallback(visible => {
    setShowFiltersPopOver(visible);
    setSearchQuery("");
  }, []);

  useEffect(() => {
    const loading = get(integrationState, ["loading"], true);
    const error = get(integrationState, ["error"], true);
    if (!loading && !error && integrationIds.length && !fieldsLoading) {
      setFieldsLoading(true);
      dispatch(OrgCustomConfigData(integrationIds as string[], fieldUri, integConfigUri, fieldId, integConfigId));
    }
  }, [integrationIds, integrationState]);

  return (
    <div>
      <CategoryFiltersComponentWrapper
        partialFiltersErrors={partialFiltersErrors}
        transformedCustomFieldRecords={customFieldsRecords ?? []}
        allFilterConfig={allFilterConfig}
        showFiltersPopOver={showFiltersPopOver}
        handleSetShowFiltersPopOver={handleSetShowFiltersPopOver}
        filters={filters}
        Menu={menu}
        activePopkey={activePopKey}
        handleActivePopKey={handleActivePopkey}
        orderedFilters={orderedFilters}
        integrationIds={integrationIds}
        handleVisibleChange={handleVisibleChange}
        onFilterValueChange={onFilterValueChange}
        handlePartialValueChange={handlePartialValueChange}
        onExcludeFilterChange={onExcludeFilterChange}
        removeFilterOptionSelected={removeFilterOptionSelected}
        handleTimeRangeFilterValueChange={handleTimeRangeFilterValueChange}
        handleTimeRangeTypeChange={handleTimeRangeTypeChange}
        metadata={metadata}
        application={application}
      />
    </div>
  );
};

export default AddCategoryFiltersContainer;
