import React, { useCallback, useEffect, useMemo, useState } from "react";
import { notification, Spin } from "antd";
import { get, map, capitalize } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
// @ts-ignore
import uuidv1 from "uuid/v1";
import { RestTags } from "classes/RestTags";
import { RestIntegrations } from "classes/RestIntegrations";
import Loader from "components/Loader/Loader";
import IntegrationDetailsForm from "configurations/components/integration-steps/integration-details-form/integration-details-form-old.component";
import CustomFieldMapping from "configurations/components/integration-steps/custom-field-mapping/custom-field-mapping.component";
import { getIntegrationPage } from "constants/routePaths";
import {
  integrationConfigsCreate,
  integrationConfigsList,
  integrationsUpdate,
  tagsBulkList,
  tagsGetOrCreate,
  jenkinsIntegrationsAttach,
  JIRA_CUSTOM_FIELDS_LIST,
  AZURE_CUSTOM_FIELDS_LIST,
  ZENDESK_CUSTOM_FIELDS_LIST,
  TESTRAILS_CUSTOM_FIELDS_LIST
} from "reduxConfigs/actions/restapi";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { getIntegrationForm } from "reduxConfigs/selectors/integrationSelectors";
import { jiraIntegrationConfigListSelector } from "reduxConfigs/selectors/jira.selector";
import { parseQueryParamsIntoKeys } from "utils/queryUtils";
import { restAPILoadingState } from "utils/stateUtil";
import IntegrationFields from "../IntegrationFieldsOld";
import "./IntegrationDetails.container.scss";
import {
  azureSpreadCustomFieldsFilter,
  mapAzureCustomHygieneFilters,
  mapAzureIntegrationFilters,
  mapAzureMissingFields,
  mapJiraIntegrationFilters,
  spreadCustomFieldsFilter,
  transformAzureMissingAndFieldSizeFilters
} from "configurations/helpers/map-filters.helper";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericMethodSelector, getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import ZendeskCustomFieldMappingComponent from "configurations/components/integration-steps/custom-field-mapping/zendesk-custom-field-mapping/zendesk-custom-field-mapping.component";
import HelixCustomFieldMappingComponent from "configurations/components/integration-steps/custom-field-mapping/helix-custom-field-mapping/helix-custom-field-mapping";
import JenkinNodes from "../../../../shared-resources/components/jenkin-nodes/jenkins-nodes.component";
import {
  jenkinsAttachedIntegrationListSelector,
  jenkinsAvailableIntegrationListSelector
} from "reduxConfigs/selectors/jenkinsIntegrationSelector";
import { AntButton } from "../../../../shared-resources/components";
import AzureCustomFieldMappingComponent from "configurations/components/integration-steps/custom-field-mapping/azure-custom-field-mapping/AzureCustomFieldMappingComponent";
import { clearCachedIntegration } from "reduxConfigs/actions/cachedIntegrationActions";
import { AZURE_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { widgetsSelector } from "reduxConfigs/selectors/widgetSelector";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import TestrailsCustomFieldMappingComponent from "configurations/components/integration-steps/custom-field-mapping/testrails-custom-field-mapping/testrailsCustomFieldMappingComponent";
import { IntegrationTypes } from "constants/IntegrationTypes";

const EPIC_LINKS_HEADER_INFO =
  "Optional field, defaults to “Epic Link”, select a value if another custom field is used to link tickets with epics";

const STORY_POINT_HEADER_INFO =
  "Optional field, defaults to “Story Points”, select a value if ticket story points are tracked through a non-standard custom field";

// Added this to avoid integration config list API call in saga as current component has already called it.
export const IntegrationConfigComplete = "Complete_Integration_Config";

interface IntegrationDetailsOldContainerProps extends RouteComponentProps {}

const IntegrationDetailsOldContainer: React.FC<IntegrationDetailsOldContainerProps> = props => {
  const { history, location } = props;

  let id: string | undefined = undefined;
  const JIRA: string = "jira";
  const ZENDESK: string = "zendesk";
  const HELIX = "helix";
  const JENKINS = "jenkins";
  const AZURE_DEVOPS = "azure_devops";
  const TESTRAILS = "testrails";
  const uuid = useMemo(() => uuidv1(), []);

  const { id: integrationId } = parseQueryParamsIntoKeys(location.search, ["id"]);
  if (integrationId) {
    id = integrationId[0];
  }
  const [custom_field_mapping_loading, setCustomFieldMappingLoading] = useState(false);
  const [custom_field_list, setCustomFieldList] = useState<any[]>([]);
  const [salesforce_field_list, setSalesforceFieldList] = useState<any[]>([]);
  const [epics_field_list, setEpicsFieldList] = useState<any[]>([]);
  const [story_points_field_list, setStoryPointsFieldList] = useState<any[]>([]);
  const [repo_paths_field_list, setRepoPathsFieldList] = useState<any[]>([]);
  const [custom_hygienes, setCustomHygienes] = useState<any[]>([]);
  const [show_dropdown, setShowDropdown] = useState(false);
  const [header, setHeader] = useState(false);
  const [integration_form, setIntegrationForm] = useState<any>({});
  const [tag_ids, setTagIds] = useState<any[]>([]);
  const [create_tags_loading, setCreateTagsLoading] = useState(false);
  const [edit_loading, setEditLoading] = useState(false);
  const [show_salesforce_dropdown, setShowSalesforceDropdown] = useState(false);
  const [show_epic_dropdown, setShowEpicDropdown] = useState(false);
  const [show_story_dropdown, setShowStoryDropdown] = useState(false);
  const [save_integration, setSaveIntegration] = useState(false);
  const [tags_loading, setTagsLoading] = useState(false);
  const [jenkinsIntegrationsLoading, setJenkinsIntegrationsLoading] = useState(false);
  const [jenkinIntegrations, setJenkinIntegrations] = useState<any>({});

  const dispatch = useDispatch();

  const integrationGetState = useParamSelector(getGenericMethodSelector, { uri: "integrations", method: "get" });
  const integrationUpdateState = useParamSelector(getGenericMethodSelector, { uri: "integrations", method: "update" });
  const integrationConfigListState = useSelector(jiraIntegrationConfigListSelector);
  const tagsBulkState = useParamSelector(getGenericMethodSelector, { uri: "tags", method: "bulk" });
  const tagsGetOrCreateState = useParamSelector(getGenericMethodSelector, { uri: "tags", method: "getOrCreate" });
  const integrationForm = useSelector(getIntegrationForm);
  const jenkinsIntegrationState = useParamSelector(getGenericUUIDSelector, {
    uri: "jenkins_integrations",
    method: "list",
    uuid: "0"
  });
  const jiraFieldsSelector = useParamSelector(getGenericMethodSelector, {
    uri: JIRA_CUSTOM_FIELDS_LIST,
    method: "list"
  });
  const azureFieldsSelector = useParamSelector(getGenericMethodSelector, {
    uri: AZURE_CUSTOM_FIELDS_LIST,
    method: "list"
  });
  const zendeskFieldsSelector = useParamSelector(getGenericMethodSelector, {
    uri: ZENDESK_CUSTOM_FIELDS_LIST,
    method: "list"
  });
  const testrailsFieldsSelector = useParamSelector(getGenericMethodSelector, {
    uri: TESTRAILS_CUSTOM_FIELDS_LIST,
    method: "list"
  });
  const selectedDashboardWidgetsState = useSelector(widgetsSelector);
  const selecteddashboard = useSelector(selectedDashboard);
  const availableIntegrations = useSelector(jenkinsAvailableIntegrationListSelector);
  const attachedIntegrations = useSelector(jenkinsAttachedIntegrationListSelector);

  useEffect(() => {
    if (custom_field_mapping_loading) {
      const { loading, error } = get(integrationConfigListState, ["data"], { loading: true, error: false });
      if (!loading && !error) {
        let customFields = [];
        let salesforceFields = [];
        let customHygienes = [];
        let epicFields = [];
        let storyPointsField = [];
        let repoPathField = [];
        const data = get(integrationConfigListState, ["data"], {});
        const apiData = data.records[0];
        if (apiData && apiData.config && apiData.config.agg_custom_fields && apiData.config.agg_custom_fields.length) {
          customFields = apiData.config.agg_custom_fields
            .filter((obj: { type: string }) => obj.type !== "any")
            .map((obj: any, index: number) => {
              return {
                key: obj.key,
                label: obj.name,
                delimiter: obj.delimiter,
                id: index.toString()
              };
            });
        }

        if (apiData && apiData.config && apiData.config.salesforce_fields && apiData.config.salesforce_fields.length) {
          salesforceFields = apiData.config.salesforce_fields
            .filter((obj: { type: string }) => obj.type !== "any")
            .map((obj: any, index: number) => {
              return {
                key: obj.key,
                label: obj.name,
                id: index.toString()
              };
            });
        }

        if (apiData && apiData.config && apiData.config.epic_field && apiData.config.epic_field.length) {
          epicFields = apiData.config.epic_field
            .filter((obj: { type: string }) => obj.type !== "any")
            .map((obj: any, index: number) => {
              return {
                key: obj.key,
                label: obj.name,
                id: index.toString()
              };
            });
        }

        if (
          apiData &&
          apiData.config &&
          apiData.config.story_points_field &&
          apiData.config.story_points_field.length
        ) {
          storyPointsField = apiData.config.story_points_field
            .filter((obj: { type: string }) => obj.type !== "any")
            .map((obj: any, index: number) => {
              return {
                key: obj.key,
                label: obj.name,
                id: index.toString()
              };
            });
        }

        if (apiData && apiData.custom_hygienes && apiData.custom_hygienes.length) {
          customHygienes = apiData.custom_hygienes
            .filter((obj: any) => obj.type !== "any")
            .map((obj: any) => {
              const missingFields = obj.missing_fields;
              let filter = obj.filter;
              if (obj.filter && obj.filter.field_size) {
                let fieldSize = Object.keys(obj.filter.field_size).reduce((acc: any, val: any) => {
                  if (val.length > 0) {
                    return { ...acc, [val]: obj.filter.field_size[val] };
                  }
                }, {});
                filter = { ...filter, field_size: fieldSize };
              }
              return {
                name: obj.name,
                id: obj.id,
                missing_fields: missingFields,
                filter: filter
              };
            });
        }

        if (apiData && apiData.repository_config && apiData.repository_config.length) {
          repoPathField = apiData.repository_config.map((obj: any, index: number) => {
            return {
              repo_id: obj.repo_id,
              path_prefix: obj.path_prefix,
              id: uuidv1()
            };
          });
        }

        setCustomFieldList(customFields);
        setSalesforceFieldList(salesforceFields);
        setEpicsFieldList(epicFields);
        setStoryPointsFieldList(storyPointsField);
        setCustomHygienes(customHygienes);
        setRepoPathsFieldList(repoPathField);
        setCustomFieldMappingLoading(false);
      }
    }
  }, [integrationConfigListState]);

  useEffect(() => {
    if (id) {
      // considering parent has done the API call
      const data = get(integrationGetState, [id, "data"], {});
      const application = get(data, ["application"], undefined);
      const tags = [...(data?.tags || [])];
      if (tags.length > 0) {
        const filters = {
          filter: {
            tag_ids: data.tags
          }
        };
        dispatch(tagsBulkList(filters));
      }

      data.tags = [];
      if (id) {
        if ([JIRA, ZENDESK, HELIX, AZURE_DEVOPS, TESTRAILS].includes(application)) {
          dispatch(integrationConfigsList({ filter: { integration_ids: [id] } }, IntegrationConfigComplete));
          setCustomFieldMappingLoading(true);
        } else if (application === JENKINS) {
          // dispatch(jenkinsList(id));
          setJenkinsIntegrationsLoading(true);
        }
      } else {
        setHeader(true);
        setIntegrationForm(data);
        setTagsLoading(tags.length > 0);
        return;
      }
      setHeader(true);
      setIntegrationForm(data);
      setTagsLoading(tags.length > 0);
    }
  }, []);

  useEffect(() => {
    if (tags_loading) {
      const { loading, error } = restAPILoadingState(tagsBulkState, "0");
      if (!loading && !error) {
        const data = get(tagsBulkState, ["0", "data", "records"], []);
        const form = integration_form;
        dispatch(restapiClear("tags", "bulk", "0"));
        setTagsLoading(false);
        setIntegrationForm({ ...form, tags: data.map((i: any) => ({ key: i.id, label: i.name })) });
      }
    }
  }, [tagsBulkState]);

  useEffect(() => {
    if (create_tags_loading) {
      const { loading, error } = restAPILoadingState(tagsGetOrCreateState, "0");
      if (!loading && !error) {
        const newtags = get(tagsGetOrCreateState, ["0", "data"], {});
        let newIntegration = new RestIntegrations();
        let form = { ...integrationForm };
        newIntegration.name = form.name;
        newIntegration.description = form.description;
        newIntegration.formData = {
          application_type: form.application,
          id: form.id,
          metadata: form.metadata,
          satellite: form.satellite,
          tags: newtags.map((t: any) => t.id).concat(tag_ids)
        };
        if ([JIRA, ZENDESK, HELIX, AZURE_DEVOPS].includes(form.application)) {
          newIntegration.formData = {
            ...(newIntegration.formData || {}),
            custom_hygienes:
              form.application === AZURE_DEVOPS
                ? getTransformedAzureCustomHygienes()
                : custom_hygienes.map(({ selectedFields, ...rest }) => rest),
            custom_fields: custom_field_list,
            salesforce_fields: salesforce_field_list,
            epic_fields: epics_field_list,
            story_points_field: story_points_field_list,
            repository_config: repo_paths_field_list
          };
        }
        dispatch(integrationsUpdate(id as string, newIntegration));
        setCreateTagsLoading(false);
        setTagIds([]);
        setEditLoading(true);
      }
    }
  }, [tagsGetOrCreateState]);

  const tranformedCustomFieldRecords = useMemo(
    () =>
      custom_field_list
        .filter(f => !String(f?.key)?.includes(AZURE_CUSTOM_FIELD_PREFIX))
        .map(f => ({
          field_key: `${AZURE_CUSTOM_FIELD_PREFIX}${f?.key}`,
          metadata: {
            transformed: AZURE_CUSTOM_FIELD_PREFIX
          }
        })),
    [custom_field_list]
  );

  const getTransformedAzureCustomHygienes = () => {
    return custom_hygienes.map(({ selectedFields, ...rest }) => ({
      ...rest,
      missing_fields: mapAzureMissingFields(rest?.missing_fields, tranformedCustomFieldRecords),
      filter: mapAzureCustomHygieneFilters(get(rest, ["filter"], {}), tranformedCustomFieldRecords)
    }));
  };
  const clearStoredCustomFields = (fieldsSelectorData: any, uri: string, method: string = "list") => {
    if (isJira || isZendesk || isAzure) {
      const cachedIntegrationKeys = Object?.keys(fieldsSelectorData || {});
      if (cachedIntegrationKeys?.length) {
        const keysToRemove = cachedIntegrationKeys.filter(val => val?.includes(id as string));
        keysToRemove?.forEach(key => {
          dispatch(restapiClear(uri, method, key));
        });
      }
    }
  };

  useEffect(() => {
    if (save_integration) {
      let valid = !!integration_form;
      valid =
        valid &&
        custom_hygienes.every(
          (hygiene: any) =>
            hygiene.name &&
            hygiene.selectedFields &&
            hygiene.selectedFields.length > 0 &&
            hygiene.selectedFields.every(
              (field: any) => !(field.value.toString().length > 0 && field.label.length === 0)
            )
        ) &&
        integration_form.name.length > 0;
      const lengthFilters = custom_hygienes.filter(
        (hygiene: any) => hygiene?.selectedFields?.filter((field: any) => field.value === "field_size").length > 0
      );
      if (lengthFilters.length) {
        valid =
          valid &&
          lengthFilters.every(
            (hygiene: any) =>
              hygiene?.filter?.field_size &&
              Object.keys(hygiene?.filter?.field_size || {}).length > 0 &&
              Object.keys(hygiene?.filter?.field_size || {}).every(
                (fil: any) => Object.keys(hygiene?.filter?.field_size?.[fil]).length > 0
              )
          );
      }
      if (!valid) {
        notification.error({
          message: "Few Fields are required, please fill them"
        });
        setSaveIntegration(false);
        return;
      }

      let newIntegration = new RestIntegrations();
      let form = { ...integration_form };
      newIntegration.name = form.name;
      newIntegration.description = form.description;
      newIntegration.formData = {
        application_type: form.application,
        id: form.id,
        metadata: form.metadata,
        satellite: form.satellite,
        tags: form.tags ? form.tags.map((tag: any) => tag.key) : []
      };
      const { newTags, existingTags } = RestTags.getNewAndExistingTags(form.tags);
      if ([JIRA, ZENDESK, HELIX, AZURE_DEVOPS, TESTRAILS].includes(integration_form.application)) {
        const filteredRepoPathList = repo_paths_field_list
          .filter(item => item.repo_id && item.path_prefix)
          .map(item => ({
            repo_id: item.repo_id,
            path_prefix: item.path_prefix
          }));
        newIntegration.formData = {
          ...(newIntegration.formData || {}),
          custom_hygienes:
            integration_form.application === AZURE_DEVOPS
              ? getTransformedAzureCustomHygienes()
              : custom_hygienes.map(({ selectedFields, ...rest }) => rest),
          custom_fields: custom_field_list,
          salesforce_fields: salesforce_field_list,
          epic_fields: epics_field_list,
          story_points_field: story_points_field_list,
          repository_config: filteredRepoPathList
        };
        let integrationConfigsData: any = {};
        if ([ZENDESK].includes(integration_form.application)) {
          integrationConfigsData = {
            integration_id: integration_form.id,
            config: {
              agg_custom_fields:
                custom_field_list &&
                custom_field_list.map((f: any) => {
                  return f.delimiter
                    ? {
                        key: f.key,
                        name: f.label,
                        delimiter: f.delimiter
                      }
                    : {
                        key: f.key,
                        name: f.label
                      };
                })
            }
          };
        } else if (integration_form.application === HELIX) {
          integrationConfigsData = {
            integration_id: integration_form.id,
            config: {},
            repository_config: filteredRepoPathList
          };
        } else if (integration_form.application === AZURE_DEVOPS) {
          integrationConfigsData = {
            integration_id: integration_form.id,
            custom_hygienes:
              custom_hygienes &&
              custom_hygienes
                .filter((customHygiene: any) => customHygiene.selectedFields?.length !== 0)
                .map(({ id, name, selectedFields, filter }: any, index: number) => {
                  const missingFields: any = {};
                  selectedFields
                    ?.filter(
                      (i: any) => !["$gt", "$lt", "$eq", "field_size", "field_exclude"].includes(i.value) && !!i.key
                    )
                    .map((i: any) => (missingFields[i.key] = !!i.value));
                  return {
                    id: id || index.toString(),
                    name,
                    missing_fields: mapAzureMissingFields(missingFields, tranformedCustomFieldRecords),
                    filter: mapAzureIntegrationFilters(filter, tranformedCustomFieldRecords)
                  };
                }),
            config: {
              agg_custom_fields:
                custom_field_list &&
                custom_field_list.map((f: any) => {
                  return f.delimiter
                    ? {
                        key: f.key,
                        name: f.label,
                        delimiter: f.delimiter
                      }
                    : {
                        key: f.key,
                        name: f.label
                      };
                })
            }
          };
        } else if (integration_form.application === TESTRAILS) {
          integrationConfigsData = {
            integration_id: integration_form.id,
            config: {
              agg_custom_fields:
                custom_field_list &&
                custom_field_list.map((f: any) => {
                  return f.delimiter
                    ? {
                        key: f.key,
                        name: f.label,
                        delimiter: f.delimiter
                      }
                    : {
                        key: f.key,
                        name: f.label
                      };
                })
            }
          };
        } else {
          integrationConfigsData = {
            integration_id: integration_form.id,
            custom_hygienes:
              custom_hygienes &&
              custom_hygienes
                .filter((customHygiene: any) => customHygiene.selectedFields?.length !== 0)
                .map(({ id, name, selectedFields, filter }: any, index: number) => {
                  const missingFields: any = {};
                  selectedFields
                    ?.filter(
                      (i: any) => !["$gt", "$lt", "$eq", "field_size", "field_exclude"].includes(i.value) && !!i.key
                    )
                    .map((i: any) => (missingFields[i.key] = !!i.value));
                  return {
                    id: id || index.toString(),
                    name,
                    missing_fields: missingFields,
                    filter: mapJiraIntegrationFilters(filter)
                  };
                }),
            config: {
              agg_custom_fields:
                custom_field_list &&
                custom_field_list.map((f: any) => {
                  return f.delimiter
                    ? {
                        key: f.key,
                        name: f.label,
                        delimiter: f.delimiter
                      }
                    : {
                        key: f.key,
                        name: f.label
                      };
                }),
              salesforce_fields:
                salesforce_field_list &&
                salesforce_field_list.map((f: any) => {
                  return {
                    key: f.key,
                    name: f.label
                  };
                }),
              epic_field:
                epics_field_list &&
                epics_field_list.map((f: any) => {
                  return {
                    key: f.key,
                    name: f.label
                  };
                }),
              story_points_field:
                story_points_field_list &&
                story_points_field_list.map((f: any) => {
                  return {
                    key: f.key,
                    name: f.label
                  };
                })
            }
          };
        }

        dispatch(integrationConfigsCreate(integrationConfigsData));
      }

      if (newTags.length > 0) {
        const tagsToCreate = newTags.map(t => t.key);
        const tag_ids = existingTags.map(t => t.key);
        dispatch(tagsGetOrCreate(tagsToCreate));
        setCreateTagsLoading(true);
        setTagIds(tag_ids);
        setSaveIntegration(false);
      } else {
        dispatch(integrationsUpdate(integration_form.id, newIntegration));
        setEditLoading(true);
        setSaveIntegration(false);
      }
    }
  }, [save_integration, custom_field_list, custom_hygienes, integration_form, tranformedCustomFieldRecords]);

  useEffect(() => {
    if (integration_form.application === IntegrationTypes.JENKINS) {
      dispatch(
        jenkinsIntegrationsAttach(
          {
            integration_id: id,
            ...jenkinIntegrations
          },
          id
        )
      );
    }
  }, [jenkinIntegrations]);

  const removeAllWidgetsData = () => {
    dispatch(restapiClear("dashboards", "get", selecteddashboard?.id));
    const widgets = Object.values(selectedDashboardWidgetsState || {});
    if (widgets?.length) {
      widgets?.forEach((widget: any) => {
        const uri = get(widgetConstants, [widget?.type, "uri"], "");
        dispatch(restapiClear(uri, "list", "-1"));
      });
    }
  };

  useEffect(() => {
    if (edit_loading) {
      const stringId = id;
      if (stringId) {
        dispatch(restapiClear("integrations", "update", stringId));
        clearStoredCustomFields(jiraFieldsSelector, JIRA_CUSTOM_FIELDS_LIST);
        clearStoredCustomFields(azureFieldsSelector, AZURE_CUSTOM_FIELDS_LIST);
        clearStoredCustomFields(zendeskFieldsSelector, ZENDESK_CUSTOM_FIELDS_LIST);
        clearStoredCustomFields(testrailsFieldsSelector, TESTRAILS_CUSTOM_FIELDS_LIST);
        const integration_ids = get(selecteddashboard, ["query", "integration_ids"], []);
        if (integration_ids?.includes(stringId)) {
          removeAllWidgetsData();
        }
        dispatch(clearCachedIntegration(stringId));
        history.push(`${getIntegrationPage()}?tab=your_integrations`);
      }
      setEditLoading(false);
    }
  }, [integrationUpdateState]);

  const onSave = useCallback(() => {
    setSaveIntegration(true);
  }, []);

  const onCancel = useCallback(() => {
    history.push(`${getIntegrationPage()}?tab=your_integrations`);
  }, [history]);

  useEffect(() => {
    if (jenkinsIntegrationsLoading) {
      const _loading = get(jenkinsIntegrationState, "loading", true);
      const _error = get(jenkinsIntegrationState, "error", true);
      if (!_loading && !_error) {
        setJenkinsIntegrationsLoading(false);
      }
    }
  }, [jenkinsIntegrationState]);

  const updateField = useCallback(
    (key: string, value: any) => {
      setIntegrationForm((prev: any) => ({
        ...prev,
        [key]: value
      }));
    },
    [integration_form]
  );

  const onFieldRemove = useCallback(
    (type: string) => {
      return (id: any) => {
        switch (type) {
          case "custom_field_list":
            setCustomFieldList(custom_field_list.filter((field: { id: any }) => field.id !== id));
            break;
          case "salesforce_field_list":
            setSalesforceFieldList(salesforce_field_list.filter((field: { id: any }) => field.id !== id));
            break;
          case "epics_field_list":
            setEpicsFieldList(epics_field_list.filter((field: { id: any }) => field.id !== id));
            break;
          case "story_points_field_list":
            setStoryPointsFieldList(story_points_field_list.filter((field: { id: any }) => field.id !== id));
            break;
        }
      };
    },
    [custom_field_list, salesforce_field_list, epics_field_list, story_points_field_list]
  );

  const handleDelimiterChange = useCallback(
    (key: any) => {
      return (delimiter: any) => {
        let customFields = [...custom_field_list];
        customFields.forEach((field: any) => {
          if (field?.key === key) {
            field.delimiter = delimiter !== "none" ? delimiter : undefined;
          }
        });
        setCustomFieldList(customFields);
      };
    },
    [custom_field_list]
  );

  const handleAddField = useCallback((type: string) => {
    switch (type) {
      case "custom_field_list":
        setShowDropdown(true);
        break;
      case "salesforce_field_list":
        setShowSalesforceDropdown(true);
        break;
      case "epics_field_list":
        setShowEpicDropdown(true);
        break;
      case "story_points_field_list":
        setShowStoryDropdown(true);
        break;
    }
  }, []);

  const handleSelectBlur = useCallback((type: string) => {
    switch (type) {
      case "custom_field_list":
        setShowDropdown(false);
        break;
      case "salesforce_field_list":
        setShowSalesforceDropdown(false);
        break;
      case "epics_field_list":
        setShowEpicDropdown(false);
        break;
      case "story_points_field_list":
        setShowStoryDropdown(false);
    }
  }, []);

  const onSelectFieldChange = useCallback(
    (type: string, data: any[]) => {
      const mappedFields = data.map((d, index) => {
        const stateObj = custom_field_list.find((s: any) => s?.key === d?.key) || {};
        return {
          ...d,
          id: index.toString(),
          ...stateObj
        };
      });

      switch (type) {
        case "custom_field_list":
          setCustomFieldList(mappedFields);
          break;
        case "salesforce_field_list":
          setSalesforceFieldList(mappedFields);
          break;
        case "epics_field_list":
          setEpicsFieldList(mappedFields);
          break;
        case "story_points_field_list":
          setStoryPointsFieldList(mappedFields);
          break;
      }
    },
    [custom_field_list]
  );

  const getDropdown = useCallback(
    (type: string) => {
      switch (type) {
        case "custom_field_list":
          return show_dropdown;
        case "salesforce_field_list":
          return show_salesforce_dropdown;
        case "epics_field_list":
          return show_epic_dropdown;
        case "story_points_field_list":
          return show_story_dropdown;
      }
    },
    [show_dropdown, show_salesforce_dropdown, show_epic_dropdown, show_story_dropdown]
  );

  const customMappings = useMemo(() => {
    return [
      {
        uuid: uuid,
        type: "custom_field_list",
        header: "custom field Mappings",
        data: custom_field_list,
        noMapping: "custom",
        paddingTop: 0,
        loadAllData: true
      },
      {
        uuid: uuid,
        type: "salesforce_field_list",
        header: "salesforce field Mappings",
        data: salesforce_field_list,
        noMapping: "salesforce",
        paddingTop: 0
      },
      {
        uuid: uuid,
        type: "epics_field_list",
        header: "Epic Field",
        data: epics_field_list,
        noMapping: "epic",
        paddingTop: "1rem",
        header_info: EPIC_LINKS_HEADER_INFO,
        singleSelectFields: true
      },
      {
        uuid: uuid,
        type: "story_points_field_list",
        header: "Story Point Field",
        data: story_points_field_list,
        noMapping: "story_point",
        paddingTop: "1rem",
        header_info: STORY_POINT_HEADER_INFO,
        singleSelectFields: true
      }
    ];
  }, [custom_field_list, salesforce_field_list, epics_field_list, story_points_field_list]);

  const isJira = useMemo(() => {
    return get(integration_form, ["application"], "") === JIRA;
  }, [integration_form]);

  const isZendesk = useMemo(() => {
    return get(integration_form, ["application"], "") === ZENDESK;
  }, [integration_form]);

  const isAzure = useMemo(() => {
    return get(integration_form, ["application"], "") === AZURE_DEVOPS;
  }, [integration_form]);

  const isHelix = useMemo(() => get(integration_form, ["application"], "") === HELIX, [integration_form]);

  const isJenkins = useMemo(() => {
    return get(integration_form, ["application"], "") === JENKINS;
  }, [integration_form]);

  const isTestrails = useMemo(() => {
    return get(integration_form, ["application"], "") === TESTRAILS;
  }, [integration_form]);

  const spinnerStyle = useMemo(() => ({ display: "flex", justifyContent: "center", alignItems: "center" }), []);

  const spinner = useMemo(() => {
    return (
      <div style={spinnerStyle}>
        <Spin />
      </div>
    );
  }, []);

  const handleCustomHygienesChange = useCallback((customHygienes: any) => {
    setCustomHygienes(customHygienes);
  }, []);

  const handleHelixCustomFieldMappingChange = useCallback((repoPathList: any) => {
    setRepoPathsFieldList(repoPathList);
  }, []);

  const integration_id = useMemo(() => integration_form.id, [integration_form]);

  const mappedCustomHygienes = useMemo(() => {
    return map(custom_hygienes, hygiene => ({
      ...hygiene,
      missing_fields: isAzure
        ? transformAzureMissingAndFieldSizeFilters(hygiene?.missing_fields, tranformedCustomFieldRecords)
        : hygiene?.missing_fields ?? {},
      filter: isAzure
        ? azureSpreadCustomFieldsFilter(hygiene.filter || {}, tranformedCustomFieldRecords)
        : spreadCustomFieldsFilter(hygiene.filter || {})
    }));
  }, [custom_hygienes, isAzure, tranformedCustomFieldRecords]);

  const renderAddCustomHygienes = useMemo(() => {
    return (
      <div className="add-custom-hygienes">
        <IntegrationFields
          isAzure={isAzure}
          integrationId={integration_id}
          onCustomHygienesChange={handleCustomHygienesChange}
          custom_hygienes={mappedCustomHygienes}
          custom_field_mapping_loading={custom_field_mapping_loading}
        />
      </div>
    );
  }, [custom_field_mapping_loading, mappedCustomHygienes, integration_form]);

  const renderForm = useMemo(
    () => (
      <div className="integration-row">
        <IntegrationDetailsForm
          integration_form={integration_form}
          tags_loading={tags_loading}
          updateField={updateField}
        />
      </div>
    ),
    [integration_form, tags_loading, updateField]
  );

  const renderCustomMapping = useMemo(
    () => (
      <div className="integration-mappings">
        {customMappings.map((mapping: any, index: number) => {
          return (
            <CustomFieldMapping
              key={index}
              custom_field_list={custom_field_list}
              custom_field_mapping={mapping}
              loading={custom_field_mapping_loading}
              onAddClick={handleAddField}
              onSelectBlur={handleSelectBlur}
              onSelectFieldChange={onSelectFieldChange}
              showDropdown={getDropdown(mapping.type)}
              integration_id={integration_id}
              onDelimiterChange={handleDelimiterChange}
              onFieldRemove={onFieldRemove}
            />
          );
        })}
      </div>
    ),
    [
      integration_form,
      customMappings,
      custom_field_mapping_loading,
      custom_field_list,
      salesforce_field_list,
      epics_field_list,
      story_points_field_list,
      show_dropdown,
      show_salesforce_dropdown,
      show_epic_dropdown,
      show_story_dropdown
    ]
  );

  const renderZendeskCustomMapping = useMemo(() => {
    return [customMappings[0]].map((mapping: any, index: number) => (
      <div className="integration-mappings">
        <ZendeskCustomFieldMappingComponent
          key={`zendesk_${index}`}
          custom_field_mapping={mapping}
          loading={custom_field_mapping_loading}
          onAddClick={handleAddField}
          onSelectBlur={handleSelectBlur}
          onSelectFieldChange={onSelectFieldChange}
          showDropdown={getDropdown(mapping.type)}
          integration_id={integration_id}
          onDelimiterChange={handleDelimiterChange}
          onFieldRemove={onFieldRemove}
        />
      </div>
    ));
  }, [integration_form, custom_field_mapping_loading, custom_field_list, show_dropdown, customMappings]);

  const renderAzureDevopsCustomMapping = useMemo(() => {
    return [customMappings[0]].map((mapping: any, index: number) => (
      <div className="integration-mappings">
        <AzureCustomFieldMappingComponent
          key={`azure_${index}`}
          custom_field_mapping={mapping}
          loading={custom_field_mapping_loading}
          onAddClick={handleAddField}
          onSelectBlur={handleSelectBlur}
          onSelectFieldChange={onSelectFieldChange}
          showDropdown={getDropdown(mapping.type)}
          more_filters={{ integration_ids: [integration_id] }}
          onDelimiterChange={handleDelimiterChange}
          onFieldRemove={onFieldRemove}
        />
      </div>
    ));
  }, [integration_form, custom_field_mapping_loading, custom_field_list, show_dropdown, customMappings]);

  const renderRepoPathMapping = useMemo(() => {
    return (
      <div className="integration-mappings">
        <HelixCustomFieldMappingComponent
          mapping={repo_paths_field_list}
          loading={custom_field_mapping_loading}
          onMappingChange={handleHelixCustomFieldMappingChange}
        />
      </div>
    );
  }, [integration_form, custom_field_mapping_loading, repo_paths_field_list]);

  const renderJenkinsIntegrations = useMemo(() => {
    return (
      <>
        <JenkinNodes
          integrationId={id as string}
          onIntegrationChange={setJenkinIntegrations}
          integrationName={integration_form.name}
        />
      </>
    );
  }, [availableIntegrations, attachedIntegrations, jenkinsIntegrationsLoading]);

  const renderTestrailsDevopsCustomMapping = useMemo(() => {
    return [customMappings[0]].map((mapping: any, index: number) => (
      <div className="integration-mappings">
        <TestrailsCustomFieldMappingComponent
          key={`azure_${index}`}
          custom_field_mapping={mapping}
          loading={custom_field_mapping_loading}
          onAddClick={handleAddField}
          onSelectBlur={handleSelectBlur}
          onSelectFieldChange={onSelectFieldChange}
          showDropdown={getDropdown(mapping.type)}
          more_filters={{ integration_ids: [integration_id] }}
          onDelimiterChange={handleDelimiterChange}
          onFieldRemove={onFieldRemove}
        />
      </div>
    ));
  }, [integration_form, custom_field_mapping_loading, custom_field_list, show_dropdown, customMappings]);

  if (edit_loading) {
    return <Loader />;
  }

  return (
    <div className="integration-details-container">
      <div className="flex justify-space-between mb-5 mt-10 ml-15 mr-15">
        <div className="integration-details-container--title">
          Edit {capitalize(integration_form.application)} Configuration
        </div>
        <div className="flex">
          <AntButton className="mr-10" type="secondary" onClick={onCancel}>
            Cancel
          </AntButton>
          <AntButton type="primary" onClick={onSave}>
            Save
          </AntButton>
        </div>
      </div>
      {renderForm}
      {isAzure && (
        <div className="integration-row">
          {renderAzureDevopsCustomMapping}
          {renderAddCustomHygienes}
        </div>
      )}
      {isZendesk && <div className="integration-row">{renderZendeskCustomMapping}</div>}
      {isHelix && <div className="integration-row">{renderRepoPathMapping}</div>}
      {isJenkins && <div className="integration-row jenkins-integrations">{renderJenkinsIntegrations}</div>}
      {isJira && (
        <div className="integration-row">
          {renderCustomMapping}
          {renderAddCustomHygienes}
        </div>
      )}
      {isTestrails && <div className="integration-row">{renderTestrailsDevopsCustomMapping}</div>}
    </div>
  );
};

export default IntegrationDetailsOldContainer;
