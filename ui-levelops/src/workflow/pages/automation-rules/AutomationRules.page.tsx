import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { get } from "lodash";
import { PROPELS_ROUTES, getBaseUrl } from "constants/routePaths";
import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { AutomationRulesList } from "workflow/containers";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";

interface AutomationRulesPageProps extends RouteComponentProps {}

const AutomationRulesPage: React.FC<AutomationRulesPageProps> = props => {
  const dispatch = useDispatch();
  const { location, history, match } = props;

  const pageSettingsState = useSelector(pageSettings);

  useEffect(() => {
    dispatch(
      setPageSettings(location.pathname, {
        title: "Automation Rules",
        action_buttons: {
          back: {
            key: "back",
            type: "secondary",
            label: "Back",
            hasClicked: false
          },
          add_rule: {
            key: "add_rule",
            type: "primary",
            label: "Add Rule",
            hasClicked: false
            // icon: "save",
          }
        }
      })
    );

    return () => {
      dispatch(clearPageSettings(location.pathname));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const backClick = get(pageSettingsState, [location.pathname, "action_buttons", "back", "hasClicked"], false);
    if (backClick) {
      const route = `${getBaseUrl()}${PROPELS_ROUTES._ROOT}`;
      history.push(route);
    }

    const addRuleClick = get(pageSettingsState, [location.pathname, "action_buttons", "add_rule", "hasClicked"], false);
    if (addRuleClick) {
      dispatch(setPageButtonAction(location.pathname, "add_automation_rule", { hasClicked: false }));
      const route = `${getBaseUrl()}${PROPELS_ROUTES.AUTOMATION_RULES}/create`;
      history.push(route);
    }
  }, [pageSettingsState]); // eslint-disable-line react-hooks/exhaustive-deps

  return <AutomationRulesList history={history} location={location} match={match} />;
};

export default AutomationRulesPage;
