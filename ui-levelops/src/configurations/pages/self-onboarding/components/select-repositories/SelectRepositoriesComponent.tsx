import { Checkbox, Collapse, notification, Spin } from "antd";
import { get, map, uniq } from "lodash";
import React, { useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { integrationCreateSelector } from "reduxConfigs/selectors/integrationSelector";
import { AntSwitch } from "shared-resources/components";
import queryString from "query-string";
import {
  INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP,
  SELECT_REPOSITORIES_COLUMNS_CONFIG,
  SelfOnboardingFormFields
} from "../../constants";
import { getIntegrationCreatePayload } from "../../helpers/getIntegrationCreateUpdatePayload";
import "./selectRepositoriesStyles.scss";
import { RestIntegrations } from "classes/RestIntegrations";
import { useLocation } from "react-router-dom";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import moment from "moment";
import {
  allowedSCMReposCountForUser,
  isSelfOnboardingUser
} from "reduxConfigs/selectors/session_current_user.selector";
import ScmReposPaginatedTable from "../scm-repos-paginated-table/ScmReposPaginatedTable";
import { SCMReposDataType } from "../../types/integration-step-components-types";
import SCMIntegrationErrorComponent from "./SCMIntegrationErrorComponent";
import SCMIntegrationNameComponent from "./SCMIntegrationNameComponent";
import { integrationsCreate } from "reduxConfigs/actions/restapi";

interface SelectRepositoriesProps {
  selfOnboardingForm: any;
  integration: string;
  getFromSelfOnboardingForm: (key: string) => any;
  updateSelfOnboardingForm: (key: string, value: any) => void;
}

const { Panel } = Collapse;

const SelectRepositoriesComponent: React.FC<SelectRepositoriesProps> = (props: SelectRepositoriesProps) => {
  const { integration, selfOnboardingForm, getFromSelfOnboardingForm, updateSelfOnboardingForm } = props;

  const [integrationCreateLoading, setIntegrationCreateLoading] = useState<boolean>(false);
  const [selectedRepos, setSelectedRepos] = useState<Array<SCMReposDataType>>(
    (selfOnboardingForm.repos || [])?.filter((item: string) => !!item) || []
  );
  const [error, setError] = useState<boolean>(false);
  const [integrationError, setIntegrationError] = useState<any>(undefined);
  const [showSelectedRepos, setShowSelectedRepos] = useState<boolean>(false);
  const integrationCreateState = useSelector(integrationCreateSelector);

  const allowedSCMReposCountForUserState: number = useSelector(allowedSCMReposCountForUser);
  const isTrialUser = useSelector(isSelfOnboardingUser);

  const location = useLocation();
  const dispatch = useDispatch();
  const { code, state, id: integration_id } = queryString.parse(location.search);

  useEffect(() => {
    if (!integration_id) {
      const extraPayload: any = { name: `${integration} ${moment.utc().unix()}`, start_ingestion: false }; // providing a dummy name while creating an integration
      const payload: RestIntegrations = getIntegrationCreatePayload({
        selfOnboardingForm,
        code,
        state,
        extraPayload,
        application: integration,
        isUpdate: false
      });
      setIntegrationCreateLoading(true);
      dispatch(integrationsCreate(payload));
    } else {
      updateSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID, integration_id);
    }
  }, []);

  useEffect(() => {
    if (integrationCreateLoading) {
      if (!get(integrationCreateState, ["loading"], true)) {
        if (get(integrationCreateState, ["error"], true)) {
          setIntegrationError(get(integrationCreateState, ["data", "preflight_check"]));
        } else {
          const integrationId = get(integrationCreateState, ["data", "id"], "");
          if (integrationId) {
            dispatch(genericRestAPISet({}, "integrations", "create", "-1"));
            updateSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID, integrationId);
          }
        }
        setIntegrationCreateLoading(false);
      }
    }
  }, [integrationCreateState, integration]);

  const handleSelectedRepos = (selectedRepoKeys: any, selectedRows: Array<SCMReposDataType> = []) => {
    const newSelectedRepos = uniq([...selectedRepoKeys]);
    if (allowedSCMReposCountForUserState > 0 && selectedRepoKeys?.length > allowedSCMReposCountForUserState) {
      setError(true);
    }

    if (allowedSCMReposCountForUserState > 0 && selectedRepoKeys?.length <= allowedSCMReposCountForUserState && error) {
      setError(false);
    }
    setSelectedRepos(newSelectedRepos);
    updateSelfOnboardingForm(SelfOnboardingFormFields.REPOS, newSelectedRepos);
  };

  const rowSelection = useMemo(() => {
    return {
      selectedRowKeys: (getFromSelfOnboardingForm(SelfOnboardingFormFields.REPOS) ?? []).filter((v: string) => !!v),
      onChange: handleSelectedRepos,
      getCheckboxProps: (rec: any) => ({
        disabled: !!getFromSelfOnboardingForm(SelfOnboardingFormFields.INGEST_ALL_REPOS)
      })
    };
  }, [getFromSelfOnboardingForm, handleSelectedRepos]);

  const renderIngestAllRepoSwitch = useMemo(() => {
    return (
      <div>
        <AntSwitch
          disabled={isTrialUser}
          checked={!!getFromSelfOnboardingForm(SelfOnboardingFormFields.INGEST_ALL_REPOS)}
          onChange={(v: boolean) => updateSelfOnboardingForm(SelfOnboardingFormFields.INGEST_ALL_REPOS, v)}
        />
        <span className="ingest-all-text">Ingest All Repos</span>
      </div>
    );
  }, [getFromSelfOnboardingForm, updateSelfOnboardingForm, isTrialUser]);

  const getRowKey = (record: any, index: number) => {
    return record?.name ?? index;
  };

  const getMoreFilters = useMemo(() => {
    if (showSelectedRepos) {
      return {
        integration_id: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID),
        repos: selectedRepos
      };
    }
    return {
      integration_id: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID)
    };
  }, [showSelectedRepos, getFromSelfOnboardingForm]);

  if (integrationCreateLoading) {
    return (
      <div className="flex align-center justify-center">
        <Spin />
      </div>
    );
  }

  if (integrationError) {
    return <SCMIntegrationErrorComponent error={integrationError} />;
  }

  return (
    <div className="select-repositories-container">
      <SCMIntegrationNameComponent
        setNameStatus={value => updateSelfOnboardingForm(SelfOnboardingFormFields.VALID_NAME, value)}
        name={getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_NAME)}
        onChange={value => updateSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_NAME, value)}
      />
      <div>
        <p className="select-repo-title">Select Repository</p>
        <p>
          The most recently updated repositories has been auto-selected.
          <br />
          Tip: choose repositories with deployments, releases, pull requests, hotfixes and any other activity you want
          to track.
        </p>
        {error && <p style={{ color: "red" }}>Please select upto {allowedSCMReposCountForUserState} repositories.</p>}
      </div>
      {getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_ID) && (
        <ScmReposPaginatedTable
          uri="self_onboarding_repos"
          method="list"
          pageName={"repos_select"}
          generalSearchField={"repo"}
          hasTitleSearch={true}
          searchClassName="select-repo-search"
          searchURI="self_onboarding_repos_search"
          searchPlaceholder="Start typing to search"
          className="select-repositories-container__table"
          rowSelection={rowSelection}
          rowKey={getRowKey}
          pageSize={10}
          selectedRepos={selectedRepos}
          moreFilters={getMoreFilters}
          customExtraContent={renderIngestAllRepoSwitch}
          columns={SELECT_REPOSITORIES_COLUMNS_CONFIG}
          setShowSelectedRepos={setShowSelectedRepos}
          showSelectedRepos={showSelectedRepos}
        />
      )}
      {Object.keys(INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP).includes(integration) ? (
        <Collapse bordered={false} className="select-repo-collapse">
          <Panel key={"additional_options"} header={"ADDITIONAL OPTIONS"} className="select-repo-panel">
            {map(Object.keys(INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP[integration]), (key: string) => {
              if (key === "fetch_commit_files" && !getFromSelfOnboardingForm("fetch_commits")) return null;
              return (
                <Checkbox
                  style={{ marginLeft: key === "fetch_commit_files" ? "2rem" : "0.5rem" }}
                  key={key}
                  checked={getFromSelfOnboardingForm(key)}
                  onChange={e => updateSelfOnboardingForm(key, e.target.checked)}>
                  {get(INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP[integration], [key], key)}
                </Checkbox>
              );
            })}
          </Panel>
        </Collapse>
      ) : null}
    </div>
  );
};

export default SelectRepositoriesComponent;
