import * as React from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { useEffect, useState } from "react";
import { mapIntegrationStatetoProps } from "reduxConfigs/maps/integrationMap";
import {
  addIntegrationMetadataOverrides,
  getBreadcrumbsForCreatePage,
  INTEGRATION_DETAILS,
  IntegrationAuthTypes
} from "./helpers";
import { CredentialOauth } from "configurations/components/integrations";
import { useCallback } from "react";
import {
  integrationForm,
  integrationComplete,
  integrationType,
  integrationSubType
} from "reduxConfigs/actions/integrationActions";
import queryString from "querystring";
import { IntegrationDetailsNewForm } from "./components/integration-form.component";
import { AntButton, AntText, AntTitle } from "../../../../shared-resources/components";
import { Button, Col, notification, Row } from "antd";
import { getIntegrationPage } from "constants/routePaths";
import { RestIntegrations } from "../../../../classes/RestIntegrations";
import { RestTags } from "../../../../classes/RestTags";
import { tagsCreate, integrationsCreate, apikeysCreate } from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import { RestApikey } from "../../../../classes/RestApikey";
import { removeEmptyIntegrations, removeEmptyObject } from "../../../../utils/integrationUtils";
import FileSaver from "file-saver";
import yaml from "js-yaml";
import Loader from "components/Loader/Loader";
import { IntegrationResponse } from "./components/integration-response.component";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { emitEvent } from "dataTracking/google-analytics";
import { AnalyticsCategoryType, IntegrationAnalyticsActions } from "dataTracking/analytics.constants";
import { getIntegrationUrlMap } from "constants/integrations";
import { initialFormValues } from "configurations/pages/self-onboarding/helpers/getIntegrationCreateUpdatePayload";
import { DOCS_ROOT, DOCS_PATHS } from "constants/docsPath";
import LocalStoreService from "services/localStoreService";
import { getBaseAPIUrl } from "constants/restUri";
import { SatelliteYAMLInterface } from "../constants";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { useAppStore } from "contexts/AppStoreContext";

interface IntegrationAddProps extends RouteComponentProps {}

export const FORM_DATA_EXCLUDE_KEYS = ["name", "form_data"];

export const IntegrationAddPageNew: React.FC<IntegrationAddProps> = props => {
  const dispatch = useDispatch();
  const integrationMap: any = getIntegrationUrlMap();

  const integrationFormState = useSelector(state => mapIntegrationStatetoProps(state));
  const integrationCreateState = useSelector(state => get(state, ["restapiReducer", "integrations", "create"], {}));
  const tagsCreateState = useSelector(state => get(state, ["restapiReducer", "tags", "create"], {}));
  const apiKeysCreateState = useSelector(state => get(state, ["restapiReducer", "apikeys", "create"], {}));

  const params = queryString.parse(props.location.search.slice(1));

  let shouldTriggerOath = false;
  if (
    integrationFormState?.integration_type === "bitbucket" &&
    integrationFormState?.integration_sub_type === IntegrationAuthTypes.OAUTH
  ) {
    shouldTriggerOath = true;
  }

  const [header, setHeader] = useState(false);
  const [isDetailFormValid, setIsDetailFormValid] = useState(false);
  const [triggerOauth, setTrigerOauth] = useState(shouldTriggerOath);
  const [createTags, setCreateTags] = useState([]);
  const [createTagsLoading, setCreateTagsLoading] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [created, setCreated] = useState(false);
  const [error, setError] = useState(false);
  const [apiKeyLoading, setApiKeyLoading] = useState(false);
  const [integrationId, setIntegrationId] = useState<undefined | string>(undefined);
  const [fieldInfo, setFieldInfo] = useState<any>({});
  const [paramsState, setParamsState] = useState<any>(params.state);
  const [fieldTouched, setFieldTouched] = useState<any>({});

  const { accountInfo } = useAppStore();

  const getCreateIntegrationData = () => {
    let newIntegration = new RestIntegrations();
    let form = { ...integrationFormState.integration_form };
    newIntegration.name = (form.name || "").trim();
    newIntegration.description = form.description;
    delete form.name;
    delete form.description;
    delete form.size;
    delete form.__altered;
    newIntegration.formData = {
      ...form,
      tags: form.tags ? form.tags.map((tag: any) => tag.key) : [],
      satellite: integrationFormState.integration_sub_type === IntegrationAuthTypes.PRIVATE_API_KEY
    };
    const integrationType = integrationFormState.integration_type;
    // @ts-ignore
    newIntegration.application = integrationMap[integrationType]?.application || integrationType;
    newIntegration.method =
      integrationFormState.integration_sub_type === IntegrationAuthTypes.OAUTH ? "oauth" : "apikey";
    if (!form.hasOwnProperty("url")) {
      newIntegration.url = "https://gitlab.com";
    }

    // Override url for gitlab cloud
    if (integrationType === IntegrationTypes.GITLAB_CLOUD) {
      newIntegration.url = "https://gitlab.com";
    }

    if (
      newIntegration.application === IntegrationTypes.BITBUCKET &&
      [IntegrationAuthTypes.PRIVATE_API_KEY, IntegrationAuthTypes.PUBLIC_API_KEY].includes(
        integrationFormState.integration_sub_type
      )
    ) {
      newIntegration.application = IntegrationTypes.BITBUCKET_SERVER;
    }

    if (integrationMap[integrationType]?.hasOwnProperty("mapIntegrationForm")) {
      newIntegration.formData = integrationMap[integrationType].mapIntegrationForm(newIntegration.formData);
    }

    // adding any additional metadata
    (newIntegration.formData as any).metadata = addIntegrationMetadataOverrides(
      newIntegration.application,
      (newIntegration.formData as any).metadata
    );

    return newIntegration;
  };

  const getDataforOauth = () => {
    return { ...getCreateIntegrationData().json(), tags: integrationFormState.integration_form.tags || [] };
  };

  const createIntegration = (integration?: any, disablePreflightCheck = false) => {
    const newIntegration = integration && integration.application ? integration : getCreateIntegrationData();

    if (disablePreflightCheck) {
      newIntegration.formData.skip_preflight_check = true;
    }

    const createTags = newIntegration.formData.tags.filter((tag: any) => tag.toString().includes("create:"));
    if (createTags.length > 0) {
      setCreateTagsLoading(true);
      setCreateTags(createTags);
      createTags.forEach((tag: any) => {
        let newTag = new RestTags();
        newTag.name = tag.replace("create:", "");
        dispatch(tagsCreate(newTag, tag));
      });
      return;
    }
    setCreateLoading(true);
    dispatch(integrationsCreate(newIntegration));
  };

  const updateField = useCallback(
    (key: string, value: any) => {
      dispatch(
        integrationForm({
          ...integrationFormState.integration_form,
          [key]: value
        })
      );
    },
    [integrationFormState.integration_form]
  );

  useEffect(() => {
    if (params.state && !createLoading) {
      const callbackStateId = params.state;
      // @ts-ignore
      const items = callbackStateId ? JSON.parse(sessionStorage.getItem(callbackStateId)) : {};
      if (items && Object.keys(items).length) {
        dispatch(integrationType(items.name));
        dispatch(integrationSubType(items.form_data.integration_sub_type));
        const keysToKeep = Object.keys(items).filter(key => !FORM_DATA_EXCLUDE_KEYS.includes(key));
        const fieldsToUpdate: any = {};
        keysToKeep.forEach(key => (fieldsToUpdate[key] = items[key]));
        if (keysToKeep.includes("error")) {
          dispatch(integrationComplete());
          // @ts-ignore
          sessionStorage.removeItem(callbackStateId);
        } else {
          const form = items.form_data;
          form.state = items.state;
          form.code = items.code;
          dispatch(integrationType(form.application));
          dispatch(integrationSubType(IntegrationAuthTypes.OAUTH));
          dispatch(
            integrationForm({
              ...fieldsToUpdate,
              name: form.name,
              description: form.description,
              metadata: form.metadata,
              tags: form.tags
            })
          );
          // @ts-ignore
          sessionStorage.removeItem(callbackStateId);

          let newIntegration = new RestIntegrations();
          newIntegration.name = (form.name || "").trim();
          newIntegration.description = form.description;

          newIntegration.formData = {
            ...form,
            ...fieldsToUpdate,
            tags: form.tags ? form.tags.map((tag: any) => tag.key) : [],
            satellite: form.satellite
          };
          newIntegration.application = form.application;
          newIntegration.method = form.method;
          if (form.application !== IntegrationTypes.BITBUCKET) {
            setCreateLoading(true);
            createIntegration(newIntegration);
          }
        }
      }
    }
  }, [params.state, params.code]);

  useEffect(() => {
    return () => {
      dispatch(integrationComplete());
      dispatch(restapiClear("integrations", "create", "0"));
    };
  }, []);

  useEffect(() => {
    if (integrationFormState?.integration_type && !header) {
      const settings = {
        title: "Integrations",
        bread_crumbs: getBreadcrumbsForCreatePage(integrationFormState?.integration_type)
      };
      dispatch(setPageSettings(props.location.pathname, settings));
      setHeader(true);
    }
  }, [integrationFormState]);

  useEffect(() => {
    const type = integrationFormState?.integration_type;
    if (
      type &&
      [IntegrationTypes.BITBUCKET, IntegrationTypes.BITBUCKET_SERVER, IntegrationTypes.GITLAB].includes(type)
    ) {
      const initialMetadataValues = initialFormValues(
        type.includes(IntegrationTypes.BITBUCKET) ? IntegrationTypes.BITBUCKET : IntegrationTypes.GITLAB
      );
      const metadata = integrationFormState?.integration_form?.metadata;
      updateField("metadata", { ...metadata, ...initialMetadataValues });
    }
  }, []);

  useEffect(() => {
    if (createTagsLoading) {
      const loading = createTags.some(tag => get(tagsCreateState, [tag, "loading"], true));
      const error = createTags.some(tag => get(tagsCreateState, [tag, "error"], true));
      if (!loading && !error) {
        let tagIds: any[] = [];
        let newTags: any[] = [];
        createTags.forEach((tag: string) => {
          tagIds.push(tagsCreateState[tag].data.id);
          newTags.push({ label: tag.replace("create:", ""), key: tagsCreateState[tag].data.id });
        });
        const newIntegration = getCreateIntegrationData();
        let form = { ...integrationFormState.integration_form };
        const tags = [
          ...form.tags.filter((tag: any) => !tag.key.includes("create:")).map((tag: any) => tag.key),
          ...tagIds
        ];
        newIntegration.formData = {
          ...form,
          tags: tags
        };
        const allTags = [...form.tags.filter((tag: any) => !tag.key.includes("create:")), ...newTags];
        dispatch(integrationForm({ tags: allTags }));
        dispatch(integrationsCreate(newIntegration));
        setCreateTagsLoading(false);
        setCreateLoading(true);
      }
    }
  }, [tagsCreateState]);

  useEffect(() => {
    if (createLoading) {
      if (!get(integrationCreateState, ["0", "loading"], true)) {
        if (get(integrationCreateState, ["0", "error"], true)) {
          setError(true);
        } else {
          // GA EVENT
          emitEvent(
            AnalyticsCategoryType.INTEGRATION,
            IntegrationAnalyticsActions.INTEGRATION_APP_SUCCESS,
            integrationFormState.integration_type
          );
          const id = get(integrationCreateState, ["data", "id"], 0);
          const storage = new LocalStoreService();
          storage.setNewIntegrationAdded(id);
          setIntegrationId(integrationCreateState["0"].data.id);
          if (integrationFormState.integration_sub_type === IntegrationAuthTypes.PRIVATE_API_KEY) {
            const apikey = new RestApikey({
              name: `${integrationFormState.integration_form.name} apikey`,
              role: "INGESTION"
            });
            dispatch(apikeysCreate(apikey));
            setApiKeyLoading(true);
          }
        }
        setCreated(true);
        setCreateLoading(false);
      }
    }
  }, [integrationCreateState]);

  useEffect(() => {
    if (apiKeyLoading) {
      if (!get(apiKeysCreateState, ["0", "loading"], true)) {
        setApiKeyLoading(false);
        if (get(apiKeysCreateState, ["0", "error"], true)) {
          notification.error({
            message: `Failed to download API Keys`,
            description: `Unable to fetch API keys for ${integrationFormState.integration_form.name} and generate satellite.yml file`
          });
        } else {
          const createApikey = apiKeysCreateState["0"].data.key;
          const { integration_form } = integrationFormState;
          let _application = integrationFormState.integration_type || "";

          let integrationConfig: any = {
            id: integrationId || "",
            application: _application,
            url: integration_form.url || "",
            username: integration_form.username || "",
            api_key: integration_form.apikey || "",
            metadata: integration_form.metadata || {}
          };

          if (_application === IntegrationTypes.BITBUCKET) {
            integrationConfig = {
              ...integrationConfig,
              application: IntegrationTypes.BITBUCKET_SERVER,
              satellite: true
            };
          }

          if (integrationMap[_application]?.hasOwnProperty("mapIntegrationForm")) {
            integrationConfig = integrationMap[_application].mapIntegrationForm(integrationConfig);
          }

          let config: SatelliteYAMLInterface = {
            satellite: {
              tenant: window.isStandaloneApp
                ? localStorage.getItem("levelops_user_org") || ""
                : (accountInfo?.identifier || "").toLowerCase(),
              api_key: createApikey,
              url: getBaseAPIUrl()
            },
            integrations: [removeEmptyObject(removeEmptyIntegrations(integrationConfig))]
          };

          if ((config.integrations as any)[0]) {
            (config.integrations as any)[0].metadata = addIntegrationMetadataOverrides(
              _application,
              (config.integrations as any)[0].metadata
            );
          }

          const ymlString = yaml.dump(config, { lineWidth: -1 });
          let file = new File([ymlString], `satellite.yml`, { type: "text/plain;charset=utf-8" });
          FileSaver.saveAs(file);
        }
      }
    }
  }, [apiKeysCreateState]);

  const onSubmit = () => {
    switch (integrationFormState.integration_sub_type) {
      case IntegrationAuthTypes.OAUTH:
        return !params.state ? setTrigerOauth(true) : createIntegration();
      case IntegrationAuthTypes.API_KEY:
      case IntegrationAuthTypes.PRIVATE_API_KEY:
      case IntegrationAuthTypes.PUBLIC_API_KEY:
        return createIntegration();
      default:
    }
  };

  const getHeader = () => {
    const integrationDetails = get(INTEGRATION_DETAILS, [integrationFormState.integration_type], []);
    const headerInfo = integrationDetails.find((item: any) => item.type === integrationFormState.integration_sub_type);

    if (headerInfo) {
      return (
        <div className="flex direction-column">
          <AntText style={{ fontSize: "18px", color: "black" }}>{headerInfo.title}</AntText>
          <AntText style={{ color: "#979797", marginBottom: "2rem" }}>{headerInfo.subTitle}</AntText>
        </div>
      );
    }

    return null;
  };

  if (created) {
    return (
      <IntegrationResponse
        createLoading={createLoading}
        apiKeyLoading={apiKeyLoading}
        integrationType={integrationFormState.integration_type}
        integrationCreateState={integrationCreateState}
        integrationFormName={integrationFormState.integration_form.name}
        history={props.history}
        setCreated={setCreated}
        setError={setError}
        error={error}
        setParamsState={setParamsState}
        createIntegration={createIntegration}
      />
    );
  }

  if (createLoading) {
    return <Loader />;
  }

  if (!integrationFormState.integration_type && !paramsState) {
    props.history.push(getIntegrationPage());
  }

  if (integrationFormState.integration_sub_type === IntegrationAuthTypes.OAUTH && !paramsState && triggerOauth) {
    return (
      <CredentialOauth
        type={integrationFormState.integration_type}
        formData={getDataforOauth()}
        className="oauth-container"
      />
    );
  }

  if (integrationFormState.integration_sub_type === IntegrationAuthTypes.PRIVATE_API_KEY && !paramsState) {
    return (
      <div style={{ height: "100%" }} className="flex direction-column">
        {getHeader()}
        <Row gutter={[40, 20]} style={{ maxWidth: "70vw", minWidth: "70vw", alignSelf: "center" }}>
          <Col span={12}>
            <IntegrationDetailsNewForm
              integration_form={integrationFormState.integration_form}
              type={integrationFormState.integration_type}
              sub_type={integrationFormState.integration_sub_type}
              onValidation={setIsDetailFormValid}
              updateField={updateField}
              width="100%"
              fieldInfo={fieldInfo}
              setFieldInfo={setFieldInfo}
              fieldTouched={fieldTouched}
              setFieldTouched={setFieldTouched}
            />
          </Col>
          <Col span={12}>
            <div className="flex direction-column">
              <AntTitle level={4} style={{ fontSize: "14px" }}>
                SATELLITE INTEGRATION
              </AntTitle>
              <AntText style={{ textAlign: "justify", margin: "0.5rem 0 1.5rem 0" }}>
                Private on-premise integrations are configured on a SEI Satellite. Download the Satellite configuration
                file for this integration and follow the below link to update your SEI Satellite
              </AntText>
              <AntButton icon="download" type="primary" disabled={!isDetailFormValid} onClick={onSubmit}>
                Submit and download the configuration file
              </AntButton>
              <AntButton
                type="link"
                style={{ marginTop: "1rem", alignSelf: "flex-start", paddingLeft: 0 }}
                href={DOCS_ROOT + DOCS_PATHS.PROPELO_INGESTION_SATELLITE}
                target="_blank"
                rel="noopener noreferrer">
                How to install a satellite
              </AntButton>
            </div>
          </Col>
        </Row>
        <div style={{ marginBottom: "1rem" }} className="flex justify-end">
          <AntButton onClick={() => props.history.push(getIntegrationPage())} style={{ margin: "1rem 11.5vw 0 0" }}>
            Cancel
          </AntButton>
        </div>
      </div>
    );
  }

  if ((!triggerOauth && !paramsState) || (shouldTriggerOath && paramsState)) {
    return (
      <div style={{ height: "100%" }} className="flex direction-column">
        {getHeader()}
        <IntegrationDetailsNewForm
          integration_form={integrationFormState.integration_form}
          type={integrationFormState.integration_type}
          sub_type={integrationFormState.integration_sub_type}
          onValidation={setIsDetailFormValid}
          updateField={updateField}
          fieldInfo={fieldInfo}
          setFieldInfo={setFieldInfo}
          fieldTouched={fieldTouched}
          setFieldTouched={setFieldTouched}
        />
        <div style={{ marginBottom: "1rem" }} className="flex justify-end">
          <AntButton onClick={() => props.history.push(getIntegrationPage())}>Cancel</AntButton>
          <AntButton
            type="primary"
            disabled={!isDetailFormValid && !params.state}
            onClick={onSubmit}
            style={{ marginLeft: "1rem" }}>
            Submit
          </AntButton>
        </div>
      </div>
    );
  }

  return null;
};
