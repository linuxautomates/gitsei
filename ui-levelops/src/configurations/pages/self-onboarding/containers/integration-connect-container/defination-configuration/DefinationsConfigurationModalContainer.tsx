import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AntButton, AntIcon, AntModal, AntText } from "shared-resources/components";
import { Divider, Icon, notification, Spin } from "antd";
import queryString from "query-string";
import { useHistory, useLocation, useParams } from "react-router-dom";
import DefinitionConfigurations from "configurations/pages/lead-time-profiles/components/doraMetricsDefinitions/DefinitionConfigurations";
import { useDispatch } from "react-redux";
import {
  velocityConfigsGet,
  velocityConfigsList,
  velocityConfigsUpdate
} from "reduxConfigs/actions/restapi/velocityConfigs.actions";
import {
  defaultRestVelocityConfigSelector,
  velocityConfigsRestGetSelector,
  velocityConfigsUpdateSelector,
  VELOCITY_CONFIG_LIST_ID
} from "reduxConfigs/selectors/velocityConfigs.selector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { RestVelocityConfigs } from "classes/RestVelocityConfigs";
import { DORA_CONFIG_METRICS } from "configurations/pages/lead-time-profiles/helpers/constants";
import "./definitionConfigurationModal.styles.scss";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { VELOCITY_CONFIGS } from "constants/restUri";
import { get } from "lodash";
import { getDefaultDashboardPath } from "utils/dashboardUtils";
import { USER_ID } from "constants/localStorageKeys";
import { ProjectPathProps } from "classes/routeInterface";

interface DefinationsConfigurationModalProps {}

const DefinationsConfigurationModalContainer: React.FC<DefinationsConfigurationModalProps> = (
  props: DefinationsConfigurationModalProps
) => {
  const location = useLocation();
  const dispatch = useDispatch();
  const history = useHistory();
  const params = useParams();
  const projectParams = useParams<ProjectPathProps>();

  const { integration_id } = queryString.parse(location.search);
  const curUserMetricEditingStateId = `${localStorage.getItem(USER_ID)}@${integration_id}`;
  const curUserMetricEditingState = sessionStorage.getItem(curUserMetricEditingStateId) === "true";
  const [updating, setUpdating] = useState(false);
  const [showDoraModal, setShowDoraModal] = useState<boolean>(!!integration_id);
  const [selectedMetric, setSelectedMetric] = useState<number>(1);

  const defaultRestVelocityConfig: RestVelocityConfigs = useParamSelector(defaultRestVelocityConfigSelector, {
    id: VELOCITY_CONFIG_LIST_ID
  });

  const restConfig: RestVelocityConfigs = useParamSelector(velocityConfigsRestGetSelector, {
    config_id: defaultRestVelocityConfig?.id
  });

  const restConfigUpdateState = useParamSelector(velocityConfigsUpdateSelector, {
    id: defaultRestVelocityConfig?.id
  });

  useEffect(() => {
    if (updating) {
      const loading = get(restConfigUpdateState, ["loading"], true);
      const error = get(restConfigUpdateState, ["error"], true);
      if (!loading) {
        if (!error) {
          notification.success({ message: "Workflow Profile Updated Successfully" });
          sessionStorage.setItem(curUserMetricEditingStateId, "true");
          history.replace(getDefaultDashboardPath(projectParams, (params as any)?.id));
          setShowDoraModal(false);
        }
        setUpdating(false);
      }
    }
  }, [restConfigUpdateState]);

  useEffect(() => {
    if (integration_id) {
      dispatch(velocityConfigsList({ integration_id: integration_id }, VELOCITY_CONFIG_LIST_ID));
    }
  }, []);

  useEffect(() => {
    if (defaultRestVelocityConfig && !restConfig?.id) {
      dispatch(velocityConfigsGet(defaultRestVelocityConfig?.id ?? ""));
    }
  }, [defaultRestVelocityConfig, restConfig]);

  const handleOnCancel = () => {
    setShowDoraModal(false);
  };

  const handleNext = () => {
    if (selectedMetric < 4) {
      setSelectedMetric(p => p + 1);
    } else {
      setUpdating(true);
      dispatch(velocityConfigsUpdate(restConfig?.id ?? defaultRestVelocityConfig?.id ?? "", restConfig));
    }
  };

  const handleBack = () => {
    if (selectedMetric > 1) {
      setSelectedMetric(p => p - 1);
    }
  };

  const handleFieldChange = useCallback(
    (key: string, value: any) => {
      (restConfig as any)[key] = value;
      dispatch(genericRestAPISet(restConfig?.json, VELOCITY_CONFIGS, "get", restConfig?.id));
    },
    [defaultRestVelocityConfig]
  );

  const renderTitle = useMemo(() => {
    return (
      <div className="post-integration-modal-title">
        <h3 className="flex justify-space-between align-center">
          Success! your app has been successfully integrated.
          <span className="ml-10">
            <Icon type="close" onClick={handleOnCancel} />
          </span>
        </h3>
        <p>You will receive an email when your insight is ready.</p>
        <Divider />
      </div>
    );
  }, [handleOnCancel]);

  const getNextButtonText = () => {
    if (selectedMetric === 4) return "Finish";
    return "Next";
  };
  const renderFooter = useMemo(() => {
    return (
      <div className="footer">
        <AntButton onClick={handleBack}>Back</AntButton>
        <AntButton type="primary" onClick={handleNext} disabled={!defaultRestVelocityConfig?.id}>
          {getNextButtonText()}
        </AntButton>
      </div>
    );
  }, [selectedMetric, defaultRestVelocityConfig]);

  if (curUserMetricEditingState) {
    history.replace(getDefaultDashboardPath(projectParams, (params as any)?.id));
    return null;
  }

  return (
    <AntModal
      className="defination-configuration-modal"
      visible={showDoraModal}
      title={renderTitle}
      centered={true}
      footer={renderFooter}
      closable={false}>
      {defaultRestVelocityConfig?.id ? (
        <div>
          <div className="defination-configuration-modal_tips">
            <h3>One more step to improve DORA metrics accuracy</h3>
            <span className="defination-configuration-modal_tips">
              How do you define Releases, Deployments, Hotfixes and Defects?
            </span>
            <br />
            <span className="defination-configuration-modal_tips">
              Review the defaults and change to match your worflow.
            </span>
            <br />
            <span className="defination-configuration-modal_tips">
              You can always update them in Settings/Workflow Profiles.
            </span>
          </div>
          <div className="flex align-baseline justify-end">
            <div>
              {DORA_CONFIG_METRICS.slice(1).map((item: { label: string; value: string }) => (
                <div
                  key={item.value}
                  className={`pt-5 ${DORA_CONFIG_METRICS[selectedMetric] === item ? "selected-config" : ""}`}>
                  <AntText className="pr-5">{item.label}</AntText>
                  <AntIcon type="edit" style={{ cursor: "pointer" }} />
                </div>
              ))}
            </div>
            <DefinitionConfigurations
              config={restConfig}
              onChange={handleFieldChange}
              metric={DORA_CONFIG_METRICS[selectedMetric]}
            />
          </div>
        </div>
      ) : (
        <div className="flex justify-center align-center">
          <Spin />
        </div>
      )}
    </AntModal>
  );
};

export default DefinationsConfigurationModalContainer;
