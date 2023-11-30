import React, { useCallback, useMemo, useState } from "react";
import { Col, Form, Icon, Row, Select, Spin } from "antd";
import { get, unset } from "lodash";
import { v1 as uuid } from "uuid";
import useDeepCompareEffect from "use-deep-compare-effect";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { useGetSupportedFiltersAndApplication } from "custom-hooks/useGetSupportedFiltersAndApplication";
import { AZURE_CUSTOM_FIELD_PREFIX, valuesToFilters } from "dashboard/constants/constants";
import { toTitleCase } from "utils/stringUtils";
import { AntTextComponent } from "shared-resources/components/ant-text/ant-text.component";
import { AntButton, AntIcon, AntInput, AntText } from "shared-resources/components";
import CustomFieldSelect from "./customFieldSelect";
import { azureCustomHygienesUtilityConfigType } from "configurations/constants";
import { useDispatch, useSelector } from "react-redux";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { genericApiState } from "reduxConfigs/selectors/generic.selector";
import { jiraIntegrationConfigListSelector } from "reduxConfigs/selectors/jira.selector";
import {
  AZURE_FILTER_KEY_MAPPING,
  AZURE_ID_FILTER_KEY_MAPPING,
  AZURE_ID_REVERSE_FILTER_KEY_MAPPING,
  AZURE_PRIORITY_FILTER_KEY_MAPPING,
  AZURE_REVERSE_FILTER_KEY_MAPPING,
  AZURE_REVERSE_PRIORITY_FILTER_KEY_MAPPING
} from "dashboard/reports/azure/constant";
import {
  JIRA_FILTER_KEY_MAPPING,
  JIRA_ID_FILTER_KEY_MAPPING,
  JIRA_ID_REVERSE_FILTER_KEY_MAPPING,
  JIRA_REVERSE_FILTER_KEY_MAPPING
} from "dashboard/reports/jira/constant";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { INTEGRATION_FIELD_NUMERIC_TYPES, workItemFieldForAzureMapping } from "./constant";
import { getKeyForFilter } from "dashboard/constants/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const { Option } = Select;

const KEY_EQUALS = "$eq";
const KEY_LESS_THAN = "$lt";
const KEY_GREATER_THAN = "$gt";
const FIELD_SIZE = "field_size";
const FIELD_EXCLUDE = "field_exclude";

const FIELD_VALUES = [
  {
    label: "Missing",
    value: true
  },
  {
    label: "Present",
    value: false
  },
  {
    label: "Equals",
    value: KEY_EQUALS
  },
  {
    label: "Exclude",
    value: FIELD_EXCLUDE
  },
  {
    label: "Length",
    value: FIELD_SIZE
  }
];

interface IntegrationFieldsProps {
  isAzure: boolean;
  integrationId: string;
  custom_hygienes: CustomHygieneType[];
  onCustomHygienesChange: (custom_hygienes: CustomHygieneType[]) => void;
  loading: boolean;
}

interface CustomHygieneType {
  id: string;
  name: string;
  missing_fields?: any;
  selectedFields?: { key: string; label: string; value: string | boolean }[];
  filter?: any;
}

const azureReportConfig: azureCustomHygienesUtilityConfigType = {
  defaultReport: "azure_tickets_report",
  moreFilters: [
    {
      label: "Workitem Story Points",
      value: "workitem_story_points"
    },
    {
      label: "Workitem Acceptance Criteria",
      value: "acceptance_criteria"
    }
  ]
};

const IntegrationFields: React.FC<IntegrationFieldsProps> = (props: IntegrationFieldsProps) => {
  const customHygienes = props.custom_hygienes;
  const { isAzure } = props;
  const integrationIds = useMemo(() => [props.integrationId], [props.integrationId]); // eslint-disable-line react-hooks/exhaustive-deps
  const dispatch = useDispatch();
  const [application, supportedFilters] = useGetSupportedFiltersAndApplication(
    isAzure ? azureReportConfig.defaultReport : "bounce_report"
  );

  const [apiSelectedCustomFields, setApiSelectedCustomFields] = useState<any[]>([]);
  const [apiSelectedFields, setApiSelectedFields] = useState<any[]>([]);
  const [loadingFilterGetValues, setLoadingFilterGetValues] = useState<boolean>(false);
  const [loadingFilterGetCustomFieldsValues, setLoadingFilterGetCustomFieldsValues] = useState<boolean>(false);

  const [apiData, setApiData] = useState<any[]>([]);
  const [customFieldsKeys, setCustomFieldsKeys] = useState<any[]>([]);
  const [customFieldsValuesData, setCustomFieldsValuesData] = useState<any[]>([]);
  const integrationConfigListState = useSelector(jiraIntegrationConfigListSelector);

  const filterValuesState = useSelector(state =>
    genericApiState(state, {
      uri: (() => {
        if (application === IntegrationTypes.AZURE) return "issue_management_workitem_values";
        return "jira_filter_values";
      })() as string,
      method: "list",
      id: "0"
    })
  );
  const customFieldsValuesState = useSelector(state =>
    genericApiState(state, {
      uri: (() => {
        if (application === IntegrationTypes.AZURE) return "issue_management_custom_field_values";
        return "jira_custom_filter_values";
      })() as string,
      method: "list",
      id: "0"
    })
  );

  const formStyle = useMemo(() => ({ marginTop: "1rem" }), []); // eslint-disable-line react-hooks/exhaustive-deps
  const titleStyle = useMemo(
    () => ({ fontSize: "12px", color: "f1f1f1", display: "block", marginLeft: "5px", marginTop: "5px" }),
    []
  ); // eslint-disable-line react-hooks/exhaustive-deps
  const fieldRequiredStyle = useMemo(() => ({ fontSize: "12px", height: "12px" }), []); // eslint-disable-line react-hooks/exhaustive-deps
  const rowStyle = useMemo(
    () => ({
      margin: "0.5rem 0",
      display: "flex",
      alignItems: "flex-end",
      justifyContent: "space-between"
    }),
    []
  ); // eslint-disable-line react-hooks/exhaustive-deps
  const columnStyle = useMemo(() => ({ paddingRight: "1rem" }), []); // eslint-disable-line react-hooks/exhaustive-deps
  const fullWidth = useMemo(() => ({ width: "100%" }), []); // eslint-disable-line react-hooks/exhaustive-deps
  const closeStyle = useMemo(() => ({ fontSize: "18px", float: "right" }), []); // eslint-disable-line react-hooks/exhaustive-deps
  const spinnerStyle = useMemo(
    () => ({ display: "flex", justifyContent: "center", alignItems: "center", marginTop: "1rem" }),
    []
  ); // eslint-disable-line react-hooks/exhaustive-deps
  const addBtnStyle = useMemo(
    () => ({ margin: 0, paddingLeft: "1.2rem", fontWeight: 400, display: "inline-block" }),
    []
  ); // eslint-disable-line react-hooks/exhaustive-deps
  const headerStyle = useMemo(() => ({ display: "flex", alignItems: "center", maxHeight: "20px" }), []); // eslint-disable-line react-hooks/exhaustive-deps
  const headerTitleStyle = useMemo(() => ({ fontSize: "1rem", fontWeight: "bold", textTransform: "uppercase" }), []); // eslint-disable-line react-hooks/exhaustive-deps
  const noHygieneTextStyle = useMemo(
    () => ({ fontSize: "12px", color: "f1f1f1", display: "block", margin: "0.5rem" }),
    []
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const getNoHygieneText = useMemo(() => {
    return <AntText style={noHygieneTextStyle}>No custom hygiene found</AntText>;
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const tokenSeparator = useMemo(() => [","], []); // eslint-disable-line react-hooks/exhaustive-deps

  const FILTER_MAPPING = useMemo(
    () =>
      isAzure
        ? { ...AZURE_FILTER_KEY_MAPPING, ...AZURE_REVERSE_FILTER_KEY_MAPPING }
        : { ...JIRA_FILTER_KEY_MAPPING, ...JIRA_REVERSE_FILTER_KEY_MAPPING },
    [isAzure]
  );

  const ID_FILTER_MAPPING = useMemo(
    () =>
      isAzure
        ? { ...AZURE_ID_FILTER_KEY_MAPPING, ...AZURE_ID_REVERSE_FILTER_KEY_MAPPING }
        : { ...JIRA_ID_FILTER_KEY_MAPPING, ...JIRA_ID_REVERSE_FILTER_KEY_MAPPING },
    [isAzure]
  );

  const AZURE_PRIORITY_MAPPING = useMemo(
    () => ({ ...AZURE_PRIORITY_FILTER_KEY_MAPPING, ...AZURE_REVERSE_PRIORITY_FILTER_KEY_MAPPING }),
    []
  );

  useDeepCompareEffect(() => {
    const { loading, error } = get(integrationConfigListState, ["data"], { loading: true, error: false });
    if (!loading && !error) {
      const data = get(integrationConfigListState, ["data"], {});
      const apiData = data.records[0];
      if (apiData && apiData.config) {
        if ((apiData.config.agg_custom_fields || []).length) {
          setCustomFieldsKeys(apiData.config.agg_custom_fields);
        }
      }
    }
  }, [integrationConfigListState]);

  useDeepCompareEffect(() => {
    if (customHygienes && customHygienes.length > 0) {
      let customFields = [];
      let fields = [];
      for (let hygiene of customHygienes) {
        if (hygiene.selectedFields?.length) {
          for (let field of hygiene.selectedFields) {
            if (field.key) {
              if ([KEY_EQUALS, FIELD_EXCLUDE].includes(field?.value as string)) {
                if (field.key in FILTER_MAPPING) {
                  fields.push(getKeyForFilter(FILTER_MAPPING[field.key]));
                } else {
                  if (field.key.includes(AZURE_CUSTOM_FIELD_PREFIX))
                    field.key = field.key.split(AZURE_CUSTOM_FIELD_PREFIX)[1];
                  if (
                    field.key.includes("customfield_") ||
                    (customFieldsKeys || []).find(_field => _field.key === field.key)
                  ) {
                    customFields.push(field.key);
                  }
                }
              }
            }
          }
        } else if (hygiene.filter) {
          for (let filter of Object.keys(hygiene.filter)) {
            if (filter.includes("customfield_")) {
              customFields.push(filter);
            } else if (FILTER_MAPPING[filter]) {
              fields.push(getKeyForFilter(FILTER_MAPPING[filter]));
            }
          }
        }
      }
      if (fields.length) {
        setApiSelectedFields([...new Set(fields)]);
      }
      if (customFields.length) {
        setApiSelectedCustomFields([...new Set(customFields)]);
      }
    }
  }, [customHygienes, customFieldsKeys]);

  useDeepCompareEffect(() => {
    // Calling for non-custom field jira/azure values
    if (apiSelectedFields.length) {
      let fields = apiSelectedFields;
      if (application === IntegrationTypes.AZURE)
        fields = fields.map(field => {
          if (field in workItemFieldForAzureMapping) {
            return workItemFieldForAzureMapping[field];
          }
        });
      const filters = {
        fields,
        integration_ids: integrationIds,
        filter: {
          integration_ids: integrationIds
        }
      };
      let uri = isAzure ? "issue_management_workitem_values" : "jira_filter_values";
      dispatch(restapiClear(uri, "list", "0"));
      dispatch(actionTypes.genericList(uri, "list", filters, null, "0"));
      setLoadingFilterGetValues(true);
    }
  }, [apiSelectedFields]);

  useDeepCompareEffect(() => {
    // Calling for custom field jira/azure values
    if (apiSelectedCustomFields.length) {
      const filters = {
        fields: apiSelectedCustomFields,
        integration_ids: integrationIds,
        filter: {
          integration_ids: integrationIds
        }
      };
      let uri = isAzure ? "issue_management_custom_field_values" : "jira_custom_filter_values";
      dispatch(restapiClear(uri, "list", "0"));
      dispatch(actionTypes.genericList(uri, "list", filters, null, "0"));
      setLoadingFilterGetCustomFieldsValues(true);
    }
  }, [apiSelectedCustomFields]);

  useDeepCompareEffect(() => {
    if (loadingFilterGetValues) {
      const loading = get(filterValuesState, ["loading"], true);
      const error = get(filterValuesState, ["error"], true);
      if (!loading) {
        setLoadingFilterGetValues(false);
        if (!error) {
          const data = get(filterValuesState, ["data"], {});
          if (data && data.records) {
            combineApiData(data.records);
          }
        }
      }
    }
  }, [filterValuesState, loadingFilterGetValues]);

  useDeepCompareEffect(() => {
    if (loadingFilterGetCustomFieldsValues) {
      const loading = get(customFieldsValuesState, ["loading"], true);
      const error = get(customFieldsValuesState, ["error"], true);
      if (!loading) {
        setLoadingFilterGetCustomFieldsValues(false);
        if (!error) {
          const data = get(customFieldsValuesState, ["data"], {});
          if (data && data.records) {
            combineApiData(data.records);
          }
        }
      }
    }
  }, [customFieldsValuesState, loadingFilterGetCustomFieldsValues]);

  useDeepCompareEffect(() => {
    if (customHygienes && customHygienes.length > 0) {
      const customHygienesWithLabel: CustomHygieneType[] = [];
      customHygienes.map(customHygiene => {
        let selectedFields: any[] = [];
        let missingFieldsKeys = Object.keys(customHygiene.missing_fields || {});

        if (missingFieldsKeys.length) {
          for (let field of missingFieldsKeys) {
            let filter = allFilters().filter(f => f.value === field);
            if (isAzure && !filter.length && field.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
              filter = allFilters().filter(f => f.value === field.split(AZURE_CUSTOM_FIELD_PREFIX)[1]);
            }
            if (filter && filter.length > 0) {
              const label = filter[0].label;
              selectedFields.push({ key: field, label, value: customHygiene.missing_fields[field] });
            }
          }
        }

        let filterFieldsKeys = Object.keys(customHygiene.filter || {});
        if (filterFieldsKeys.length > 0) {
          for (let field of filterFieldsKeys) {
            let isAzureCustom = false;
            if (isAzure && field.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
              field = field.split(AZURE_CUSTOM_FIELD_PREFIX)[1];
              isAzureCustom = true;
            }

            const filter = allFilters().filter(f => ((valuesToFilters as any)[f.value] || f.value) === field);
            if (filter && filter.length > 0) {
              const label = filter[0].label;
              let res = { key: field, label, value: undefined };
              if (isAzureCustom) {
                let _fieldData = Object.keys(customHygiene.filter || {}).find(_field => {
                  return (
                    _field.includes(AZURE_CUSTOM_FIELD_PREFIX) && _field.split(AZURE_CUSTOM_FIELD_PREFIX)[1] === field
                  );
                });
                if (_fieldData) {
                  (res as any).value = Array.isArray(customHygiene.filter[_fieldData])
                    ? KEY_EQUALS
                    : Object.keys(customHygiene.filter[_fieldData][0]);
                }
              } else {
                (res as any).value = Array.isArray(customHygiene.filter[field])
                  ? KEY_EQUALS
                  : Object.keys(customHygiene.filter[field])[0];
              }
              selectedFields.push(res);
            }
          }

          let excludeFilterKeys = Object.keys(customHygiene.filter.exclude);
          if (excludeFilterKeys.length > 0) {
            for (let excludedField of excludeFilterKeys) {
              const excludedFilter = allFilters().filter(
                f => ((valuesToFilters as any)[f.value] || f.value) === excludedField
              );
              if (excludedFilter && excludedFilter.length > 0) {
                const label = excludedFilter[0].label;
                selectedFields.push({
                  key: excludedField,
                  label,
                  value: FIELD_EXCLUDE
                });
              }
            }
          }

          let fieldSizeFields = Object.keys(customHygiene.filter?.field_size || {});
          if (fieldSizeFields.length > 0) {
            for (let field of fieldSizeFields) {
              const filter = allFilters().filter(f => f.value === field);
              if (filter && filter.length > 0) {
                const label = filter[0].label;
                selectedFields.push({
                  key: field,
                  label,
                  value: FIELD_SIZE
                });
              }
            }
          }
        }

        // Adding empty input fields, recently added fields/hygienes
        for (let field of customHygiene?.selectedFields || []) {
          let fieldsAddedAlready = selectedFields.map(field => field.label);
          if (!fieldsAddedAlready.includes(field.label)) {
            selectedFields.push(field);
          }
        }

        customHygienesWithLabel.push({ ...customHygiene, selectedFields });
      });
      props.onCustomHygienesChange(customHygienesWithLabel);
    }
  }, [customHygienes, props.loading, customFieldsKeys, apiData]); // eslint-disable-line react-hooks/exhaustive-deps

  const combineApiData = (newRecords: Array<any>) => {
    let combined = [...apiData];
    if ([IntegrationTypes.AZURE].includes(application)) {
      let azureKeysMapped = (newRecords || []).map((keyData: any) => {
        let fieldKey = Object.keys(keyData)[0];
        return { [fieldKey]: keyData[fieldKey].records };
      });
      combined = [...combined, ...azureKeysMapped];
    } else {
      combined = [...combined, ...newRecords];
    }
    if (combined.length) {
      setApiData(combined);
    }
  };

  const updateCustomHygienes = useCallback(
    (customHygienes: CustomHygieneType[]) => {
      props.onCustomHygienesChange(customHygienes);
    },
    [props.onCustomHygienesChange]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const getSupportedFiltersData = useCallback(() => {
    if (supportedFilters) {
      let allPossibleFields = [
        ...(supportedFilters.values || []),
        ...(apiData || []).map(field => Object.keys(field)[0]),
        ...(customFieldsKeys || []).map(customField => customField.key)
      ];

      // remove any possible duplicates
      allPossibleFields = [...new Set(allPossibleFields)];
      return (allPossibleFields || []).map((field: any) => {
        let dataFromApi = [];
        for (let fieldObj of apiData) {
          if (field in fieldObj) {
            dataFromApi = fieldObj[field];
            break;
          }
        }
        return { [field]: dataFromApi };
      });
    }
    return [];
  }, [apiData, supportedFilters, customFieldsKeys]); // eslint-disable-line react-hooks/exhaustive-deps

  const allFilters = useCallback(() => {
    const supportedFiltersMap = (getSupportedFiltersData() || []).map((item: any) => {
      let field = Object.keys(item)[0];
      let customFieldByKey = (customFieldsKeys || []).map(field => field.key);
      if (customFieldByKey.length && customFieldByKey.includes(field)) {
        return {
          label: customFieldsKeys.find(_field => field === _field.key).name,
          value: field
        };
      }
      return {
        label: toTitleCase(field),
        value: field
      };
    });

    let _moreOptions = [
      {
        label: "Story Point",
        value: "story_points"
      },
      {
        label: "Parent Story Point",
        value: "parent_story_points"
      }
    ];
    if (isAzure) {
      _moreOptions = azureReportConfig.moreFilters;
    }
    return [...supportedFiltersMap, ..._moreOptions];
  }, [supportedFilters, isAzure, customFieldsKeys]); // eslint-disable-line react-hooks/exhaustive-deps

  const unselectedFields = useCallback(
    (customHygiene: CustomHygieneType) => {
      if (!customHygiene) {
        return allFilters();
      }
      const selectedValues: string[] = [];
      customHygiene.selectedFields?.forEach((i: { label: string; key: string; value: string | boolean }) => {
        const mappedKey = FILTER_MAPPING[i.key] ?? i.key;
        selectedValues.push(i.key);
        if (mappedKey !== i.key) {
          selectedValues.push(mappedKey);
        }
      });
      return allFilters().filter(({ value }) => !selectedValues.includes(value));
    },
    [supportedFilters, customFieldsKeys]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleCustomHygieneDelete = useCallback(
    (ch: CustomHygieneType) => {
      return (e: any) => {
        updateCustomHygienes(customHygienes.filter(c => c.id !== ch.id));
      };
    },
    [customHygienes, apiData, supportedFilters]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleCustomHygieneNameChange = useCallback(
    (index: number) => {
      return (e: any) => {
        const customHygiene = customHygienes[index];
        customHygiene.name = e.target.value;
        customHygienes[index] = customHygiene;
        updateCustomHygienes([...customHygienes]);
      };
    },
    [customHygienes, apiData, supportedFilters]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleAdd = useCallback(() => {
    const newCustomHygiene = {
      id: uuid(),
      name: "",
      selectedFields: [{ key: "", label: "", value: "" }]
    };
    props.onCustomHygienesChange([...customHygienes, newCustomHygiene]);
  }, [customHygienes]); // eslint-disable-line react-hooks/exhaustive-deps

  const renderADDButton = useMemo(() => {
    return (
      <div onClick={handleAdd} className="add-btn" style={addBtnStyle}>
        <Icon className="add-icon" type="plus-circle" />
        Add Hygiene Miss Criteria
      </div>
    );
  }, [customHygienes]); // eslint-disable-line react-hooks/exhaustive-deps

  const renderOptions = useCallback(
    (customHygiene: CustomHygieneType) => {
      return unselectedFields(customHygiene).map((field: { value: string; label: string }) => {
        return (
          <Option key={field.value} value={field.value}>
            {field.label}
          </Option>
        );
      });
    },
    [apiData, supportedFilters, customFieldsValuesData]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const getHeader = useMemo(() => {
    return (
      <div style={headerStyle}>
        <AntText style={headerTitleStyle}>Custom Hygiene Misses</AntText>
        {renderADDButton}
      </div>
    );
  }, [customHygienes]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleFieldChanges = useCallback(
    (type: "field" | "value", value: any, index: number, itemIndex: number) => {
      const _customHygienes = [...customHygienes];
      const ch: any = _customHygienes[index];
      let item = ch.selectedFields[itemIndex];
      let prevKey = item.key;

      if (item && (item || {}).hasOwnProperty("key")) {
        const toRemoveFilterKeys = [item.key, (valuesToFilters as any)[item.key]];
        for (let keyToRemove of toRemoveFilterKeys) {
          unset(ch, ["filter", keyToRemove]);
          unset(ch, ["filter", FIELD_SIZE, keyToRemove]);
          unset(ch, ["filter", "exclude", keyToRemove]);
          unset(ch, ["missing_fields", keyToRemove]);
        }
        if (type === "field") {
          const filter = allFilters().find((i: any) => i.value === value);
          if (filter) {
            item = { value: "", key: value, label: filter.label };
          }
        }
        if (type === "value") {
          item = { ...item, value };

          /**
           * this is fix the singular filter key for missing_fields and plural filter key anywhere else
           */
          if ([true, false].includes(item.value)) {
            item.key = prevKey;
            let updatedFilterKey = Object.keys(valuesToFilters).filter(
              (field: string) => (valuesToFilters as any)[field] === (item.key as string)
            );
            if (updatedFilterKey.length) item.key = updatedFilterKey[0];
          }
        }

        const _selectedFields = ch.selectedFields || [];
        _selectedFields[itemIndex] = item;
        ch.selectedFields = [..._selectedFields];
        _customHygienes[index] = { ...ch };
        updateCustomHygienes([..._customHygienes]);
      }
    },
    [customHygienes, apiData, supportedFilters]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleCustomHygieneItemDelete = useCallback(
    (ch: CustomHygieneType, index: number = 0, itemIndex: number) => {
      return (e: any) => {
        const _customHygienes = [...customHygienes];
        const filterKey = ch.selectedFields && ch.selectedFields[itemIndex] && ch.selectedFields[itemIndex].key;

        if (filterKey && ch.missing_fields && filterKey in ch.missing_fields) {
          let value = ch.selectedFields && ch.selectedFields[itemIndex] && ch.selectedFields[itemIndex].value;
          if (ch.missing_fields[filterKey] === value) {
            delete ch.missing_fields[filterKey];
          }
        }

        if (filterKey && ch.filter && ch.filter.field_size) {
          delete ch.filter.field_size[filterKey];
        }

        if (filterKey && ch.filter && ch.filter[filterKey]) {
          delete ch.filter[filterKey];
        }

        if (filterKey && ch.filter && ch.filter.exclude) {
          delete ch.filter.exclude[filterKey];
        }

        ch.selectedFields = ch?.selectedFields?.filter((v: any, i: number) => i !== itemIndex);
        _customHygienes[index] = { ...ch };
        updateCustomHygienes([..._customHygienes]);
      };
    },
    [customHygienes]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleAddFieldItem = useCallback(
    (ch: CustomHygieneType, index: number) => {
      return (e: any) => {
        const _customHygienes = [...customHygienes];
        if (!ch.selectedFields) {
          ch.selectedFields = [];
        }
        ch.selectedFields.push({ key: "", label: "", value: "" });
        _customHygienes[index] = { ...ch };
        updateCustomHygienes([..._customHygienes]);
      };
    },
    [customHygienes]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const inputFieldChanged = useCallback(
    (ch: CustomHygieneType, type: string, index: number, key: string) => {
      return (e: any) => {
        const value = e.target ? e.target.value : e;
        const _customHygienes = [...customHygienes];
        if (type === "$eq") {
          ch.filter = { ...ch.filter, [(valuesToFilters as any)[key] || key]: value };
        } else if (type === FIELD_EXCLUDE) {
          ch.filter = {
            ...ch.filter,
            exclude: {
              ...(ch?.filter?.exclude || {}),
              [(valuesToFilters as any)[key] || key]: value
            }
          };
        } else {
          ch.filter = { ...ch.filter, [key]: { [type]: value } };
        }
        _customHygienes[index] = { ...ch };
        updateCustomHygienes([..._customHygienes]);
      };
    },
    [customHygienes]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const validHygienes = useCallback((ch: CustomHygieneType) => {
    return (
      ch &&
      ch.selectedFields &&
      ch.selectedFields.every((i: any) => i.key && i.value !== null && i.value !== undefined && i.value !== "")
    );
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const mapOptionsForWorkItemPriorities = useCallback((options: Array<any>, valueKey: string) => {
    const mapped = (options || []).map((item: any) => ({
      ...item,
      label: get(staticPriorties, [item[valueKey]], item[valueKey])
    }));
    return mapped;
  }, []);

  const multiDropdownOptionsTransform = useCallback(
    (query: string) => {
      const mappedKey = FILTER_MAPPING[query] ?? query;
      if (!apiData) {
        return null;
      }

      let options = [];
      if (application === IntegrationTypes.AZURE) {
        let keyFound = Object.keys(valuesToFilters).find(key => (valuesToFilters as any)[key] === query);
        if (keyFound) query = keyFound;

        if (query in workItemFieldForAzureMapping) {
          query = workItemFieldForAzureMapping[query];
        }

        if (query.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
          query = query.split(AZURE_CUSTOM_FIELD_PREFIX)[1];
        }
      }

      const _data = apiData.find((i: any) => Object.keys(i)[0] === mappedKey || Object.keys(i)[0] === query);
      options = _data?.[Object.keys(_data || {})[0]];

      if (!options) {
        return null;
      }

      let valueKey: string = "key",
        labelKey: string = "key";

      if (ID_FILTER_MAPPING[query]) {
        labelKey = "additional_key";
      }

      if (AZURE_PRIORITY_MAPPING[query]) {
        options = mapOptionsForWorkItemPriorities(options, valueKey);
        labelKey = "label";
      }

      return options.map((i: any) => (
        <Option key={i[valueKey] ?? i[labelKey]} value={i[valueKey] ?? i[labelKey]}>
          {(i[labelKey] ?? "").toString()}
        </Option>
      ));
    },
    [apiData]
  );

  const mapValuesToFilter = useCallback((key: string) => {
    const exceptionalFilterKeys: { [key: string]: string } = {
      projects: "projects"
    };
    return Object.keys(exceptionalFilterKeys)?.includes(key)
      ? exceptionalFilterKeys[key]
      : (valuesToFilters as any)[key];
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const multiDropdownValueTransform = useCallback((value: string, key: string, filters: any) => {
    if (!filters) {
      return;
    }

    const mappedKey = mapValuesToFilter(key) || key;
    switch (value) {
      case FIELD_EXCLUDE: {
        return (filters["exclude"] || {})[mappedKey] || [];
      }
      default: {
        return filters[mappedKey] || [];
      }
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleLengthChange = useCallback(
    (index: number, key: string) => {
      const _customHygienes = [...customHygienes];
      const ch: any = _customHygienes[index];
      ch.filter = {
        ...(ch.filter || {}),
        [FIELD_SIZE]: {
          ...(ch.filter?.[FIELD_SIZE] || {}),
          [key]: {}
        }
      };
      _customHygienes[index] = { ...ch };
      updateCustomHygienes([..._customHygienes]);
    },
    [customHygienes]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const getLengthTypeValue = useCallback(
    (index: number, key: string) => {
      const _customHygienes = [...customHygienes];
      const ch: any = _customHygienes[index];
      return get(ch.filter, [FIELD_SIZE, key], {});
    },
    [customHygienes]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleLengthTypeChange = useCallback(
    (index: number, key: string, value: string) => {
      return (e: any) => {
        const lengthValue = e.target.value;
        const _customHygienes = [...customHygienes];
        const ch: any = _customHygienes[index];
        if (lengthValue.length > 0) {
          ch.filter = {
            ...(ch.filter || {}),
            [FIELD_SIZE]: {
              ...(ch.filter?.[FIELD_SIZE] || {}),
              [key]: {
                ...(ch.filter?.[FIELD_SIZE]?.[key] || {}),
                [value]: lengthValue
              }
            }
          };
        } else {
          unset(ch.filter, [FIELD_SIZE, key, value]);
        }
        _customHygienes[index] = { ...ch };
        updateCustomHygienes([..._customHygienes]);
      };
    },
    [customHygienes]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleFieldChange = useCallback(
    (type: any, index: number, itemIndex: number) => {
      return (value: any) => {
        handleFieldChanges(type, value, index, itemIndex);
      };
    },
    [customHygienes, apiData, supportedFilters]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const handleValueChange = useCallback(
    (type: any, index: number, itemIndex: number, key: string) => {
      return (value: any) => {
        handleFieldChanges(type, value, index, itemIndex);
        if (value === FIELD_SIZE) {
          handleLengthChange(index, key);
        }
      };
    },
    [customHygienes, apiData, supportedFilters]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const filterIsLengthType = useCallback(
    (index: number, key: string, filterValue: string) => {
      const filter = getLengthTypeValue(index, key);
      return filterValue === FIELD_SIZE && Object.keys(filter).length > 0;
    },
    [customHygienes]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  const getCustomDataSource = useCallback(
    (key: string) => {
      const filteredData = apiData.find((obj: any) => Object.keys(obj).includes(key));
      return filteredData && filteredData[key] ? filteredData[key] : [];
    },
    [apiData]
  );

  const getFieldType = useCallback(
    (fieldKey: string) => {
      const _customFields: { custom_fields: IntegrationTransformedCFTypes[] } = apiData.find(
        (item: any) => Object.keys(item)[0] === "custom_fields"
      );
      let fieldType: string = "string";
      if (_customFields && _customFields.hasOwnProperty("custom_fields")) {
        const customFieldConfig = (_customFields.custom_fields ?? []).find(cf => cf.key === fieldKey);
        if (customFieldConfig) {
          fieldType = customFieldConfig.field_type ?? fieldType;
        }
      }
      return fieldType;
    },
    [apiData]
  );

  const getSelectedFields = useCallback(
    (customHygiene: CustomHygieneType, index: number) => {
      return customHygiene?.selectedFields?.map((field: any, itemIndex: number) => {
        let options: any = [...FIELD_VALUES];
        const fieldType = getFieldType(field.key);
        let gtLtOptions: any = [
          {
            label: "Greater Than",
            value: KEY_GREATER_THAN
          },
          {
            label: "Less Than",
            value: KEY_LESS_THAN
          }
        ];
        if (
          ["story_points", "parent_story_points", "workitem_story_points"].includes(field.key) ||
          INTEGRATION_FIELD_NUMERIC_TYPES.includes(fieldType)
        ) {
          options = [...options, ...gtLtOptions];
        }
        return (
          <Row style={rowStyle}>
            <div className="dropdown-row">
              <Col span={field.value === FIELD_SIZE ? 6 : 8} style={columnStyle}>
                <Select
                  style={fullWidth}
                  mode="default"
                  placeholder="Field"
                  value={field?.label}
                  onChange={handleFieldChange("field", index, itemIndex)}>
                  {renderOptions(customHygiene)}
                </Select>
              </Col>
              <Col span={field.value === FIELD_SIZE ? 6 : 8} style={columnStyle}>
                <Select
                  style={fullWidth}
                  mode="default"
                  placeholder="Value"
                  value={field?.value}
                  onChange={handleValueChange("value", index, itemIndex, field.key)}>
                  {options.map((i: any) => (
                    <Option key={i.value} value={i.value}>
                      {i.label}
                    </Option>
                  ))}
                </Select>
              </Col>
              {field?.value === FIELD_SIZE && (
                <>
                  <Col span={6} style={columnStyle}>
                    <AntInput
                      placeholder="Greater Than"
                      value={getLengthTypeValue(index, field.key)[KEY_GREATER_THAN]}
                      hasError={!filterIsLengthType(index, field.key, field.value)}
                      onChange={handleLengthTypeChange(index, field.key, KEY_GREATER_THAN)}
                    />
                    {!filterIsLengthType(index, field.key, field.value) && (
                      <AntText style={fieldRequiredStyle} type="danger">
                        This field is required
                      </AntText>
                    )}
                  </Col>
                  <Col span={6} style={columnStyle}>
                    <AntInput
                      placeholder="Less Than"
                      hasError={!filterIsLengthType(index, field.key, field.value)}
                      value={getLengthTypeValue(index, field.key)[KEY_LESS_THAN]}
                      onChange={handleLengthTypeChange(index, field.key, KEY_LESS_THAN)}
                    />
                    {!filterIsLengthType(index, field.key, field.value) && (
                      <AntText style={fieldRequiredStyle} type="danger">
                        This field is required
                      </AntText>
                    )}
                  </Col>
                </>
              )}
              {[KEY_GREATER_THAN, KEY_LESS_THAN].includes(field?.value) && (
                <Col span={6} style={columnStyle}>
                  <AntInput
                    value={
                      customHygiene.filter && customHygiene.filter[field.key]
                        ? Object.values(customHygiene.filter[field.key])[0]
                        : ""
                    }
                    onChange={inputFieldChanged(customHygiene, field.value, index, field.key)}
                  />
                </Col>
              )}
              {[KEY_EQUALS, FIELD_EXCLUDE].includes(field?.value) && !(field.key || "").includes("custom") && (
                <Col span={8} style={columnStyle}>
                  <Select
                    mode="tags"
                    style={fullWidth}
                    placeholder="Value"
                    value={multiDropdownValueTransform(field.value, field.key, customHygiene.filter)}
                    allowClear={true}
                    tokenSeparators={tokenSeparator}
                    onChange={inputFieldChanged(customHygiene, field.value, index, field.key)}>
                    {multiDropdownOptionsTransform(field.key)}
                  </Select>
                </Col>
              )}
              {[KEY_EQUALS, FIELD_EXCLUDE].includes(field?.value) && (field.key || "").includes("custom") && (
                <Col span={8} style={columnStyle}>
                  <CustomFieldSelect
                    selectedValues={multiDropdownValueTransform(field.value, field.key, customHygiene.filter)}
                    dataSource={getCustomDataSource(field.key)}
                    onChange={inputFieldChanged(customHygiene, field.value, index, field.key)}
                  />
                </Col>
              )}
            </div>
            <div className="delete-container">
              {customHygiene && customHygiene.selectedFields && customHygiene.selectedFields.length > 1 && (
                <div className="icon-container">
                  <AntIcon
                    type="close-circle"
                    // @ts-ignore
                    style={closeStyle}
                    onClick={handleCustomHygieneItemDelete(customHygiene, index, itemIndex)}
                  />
                </div>
              )}
            </div>
          </Row>
        );
      });
    },
    [customHygienes, apiData, supportedFilters]
  ); // eslint-disable-line react-hooks/exhaustive-deps

  if (loadingFilterGetValues || props.loading) {
    return (
      <div style={spinnerStyle}>
        <Spin />
      </div>
    );
  }

  return (
    <Form.Item label={getHeader} colon={false} style={formStyle} className="integration-field-form">
      <div className={"integration-field-container"}>
        {customHygienes.length === 0 && getNoHygieneText}
        <div className="customHygienes">
          {customHygienes.map((customHygiene: CustomHygieneType, index: number) => {
            return (
              <div key={index} className="customHygiene">
                <AntTextComponent style={titleStyle}>
                  Name<AntText type="danger">*</AntText>
                </AntTextComponent>
                <div className="hygiene-row">
                  <div className="w-65 hygiene-name">
                    <AntInput
                      className="w-100"
                      value={customHygiene.name}
                      onChange={handleCustomHygieneNameChange(index)}
                      placeholder="Name"
                      hasError={!customHygiene.name}
                    />
                    {!customHygiene.name && (
                      <AntText style={fieldRequiredStyle} type="danger">
                        This field is required
                      </AntText>
                    )}
                  </div>
                  <div className="icon-container">
                    <AntIcon type="delete" onClick={handleCustomHygieneDelete(customHygiene)} />
                  </div>
                </div>
                {getSelectedFields(customHygiene, index)}
                <div>
                  <AntButton
                    onClick={handleAddFieldItem(customHygiene, index)}
                    disabled={!validHygienes(customHygiene)}>
                    Add field
                  </AntButton>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </Form.Item>
  );
};

export default React.memo(IntegrationFields);
