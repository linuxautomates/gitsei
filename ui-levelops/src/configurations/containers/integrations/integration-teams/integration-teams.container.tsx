import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Card } from "shared-resources/components";
import { getAllTeamsSelector } from "reduxConfigs/selectors/teamsSelectors";
import "./integration-teams.style.scss";
import { integrationsGet, teamsList } from "reduxConfigs/actions/restapi";
import { get } from "lodash";

interface IntegrationTeamsContainerProps {
  className: string;
  integration: any;
}
export const IntegrationTeamsContainer: React.FC<IntegrationTeamsContainerProps> = (
  props: IntegrationTeamsContainerProps
) => {
  const className = props.className !== undefined ? props.className : "integration-teams";

  const dispatch = useDispatch();

  const integrationMetadataState = useSelector(state => get(state, ["restapiReducer", "integrations", "get"], {}));

  const teamsState = useSelector(state => getAllTeamsSelector(state));

  useEffect(() => {
    if (props.integration.id) {
      dispatch(integrationsGet(props.integration.id));
    }

    const params = {
      filter: { partial: {} },
      page: 0,
      page_size: 100,
      sort: []
    };
    dispatch(teamsList(params));
  }, []);

  return (
    <Card>
      <div className={`${className} flex`}>
        <div className={`${className}__metadata`}>
          {integrationMetadataState[props.integration.id] &&
            integrationMetadataState[props.integration.id].data &&
            Object.keys(integrationMetadataState[props.integration.id].data.metadata).map(meta => {
              return (
                <div className="flex direction-column">
                  <div>{meta}</div>
                  <div className="flex direction-column">
                    {integrationMetadataState[props.integration.id].data.metadata[meta].map((item: any) => (
                      <div>{item}</div>
                    ))}
                  </div>
                </div>
              );
            })}
        </div>
        <div className={`${className}__info`}>Teams</div>
        {teamsState.map((team: any) => (
          <div className={`${className}__team`} key={`team-${team.id}`}>
            <div className={`${className}__state`}></div>
            <div className={`${className}__label`}>{team.name}</div>
          </div>
        ))}
      </div>
    </Card>
  );
};

export default React.memo(IntegrationTeamsContainer);
