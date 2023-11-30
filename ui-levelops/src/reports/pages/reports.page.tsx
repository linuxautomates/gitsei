import { Spin, Tabs } from "antd";
import { AssessmentsList } from "assessments/pages";
import { PluginResultsList } from "configurations/pages";
import { getReportsPage } from "constants/routePaths";
import { mapFiltersToIds, mapNonApiFilters } from "dashboard/helpers/helper";
import { get } from "lodash";
import queryString from "query-string";
import React, { useEffect, useMemo, useRef, useState, useContext } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { formClear } from "reduxConfigs/actions/formActions";
import { getIdsMap } from "reduxConfigs/actions/restapi/genericIdsMap.actions";
import { getformState } from "reduxConfigs/selectors/formSelector";
import { PropelReportsList, ProductAggsList } from "reports/components";
import { USERROLES } from "routes/helper/constants";
import LocalStoreService from "services/localStoreService";
import { getNameWithUUID } from "utils/commonUtils";
import { WebRoutes } from "../../routes/WebRoutes";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";

const { TabPane } = Tabs;

const ReportsPage: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const ls = new LocalStoreService();

  const formName = useRef<string | undefined>();
  const filtersFromLocation = useRef<any>({});

  const [activeKey, setActiveKey] = useState<string>("assessments");
  const [moreFilters, setMoreFilters] = useState<any>({});
  const [filtersLoading, setFiltersLoading] = useState<boolean>(false);
  const formState = useSelector((state: any) => getformState(state, formName.current));
  const nonApiFilters = mapNonApiFilters(filtersFromLocation.current);
  const dispatch = useDispatch();
  const isTrialUser = useSelector(isSelfOnboardingUser);

  useEffect(() => {
    const values = queryString.parse(props.location.search);
    const filters = values?.filters ? JSON.parse(values.filters as string) : {};
    filtersFromLocation.current = filters;
    if (Object.keys(filters).length && !formName.current?.length) {
      const ids = mapFiltersToIds(filters);
      const name = getNameWithUUID("report_filters_map");
      if (Object.keys(ids).length) {
        dispatch(getIdsMap(name, ids));
      }
      formName.current = name;
      setFiltersLoading(Object.keys(ids).length > 0);
    }
  }, []);

  useEffect(() => {
    if (isTrialUser) {
      //@ts-ignore
      props.history.push({ pathname: WebRoutes.dashboard.details(props.match.params, "") });
    }
  }, [isTrialUser]);

  useEffect(() => {
    return () => {
      if (formName.current?.length) {
        dispatch(formClear(formName.current));
      }
    };
  }, []);

  useEffect(() => {
    const values = queryString.parse(props.location.search);
    const tab = values?.tab ? values?.tab : "assessments";
    if (activeKey !== tab) {
      setActiveKey(tab as string);
    }
  });

  useEffect(() => {
    if (filtersLoading && formName.current?.length) {
      const ids = mapFiltersToIds(filtersFromLocation.current);
      if (Object.keys(ids).length === Object.keys(formState).length) {
        let moreFilters = {};
        Object.keys(ids).forEach(key => {
          const data = get(formState, [key], []);
          let filterKey = key;
          let mappedData = data.map((res: any) => ({ key: res?.id, label: res?.name }));

          if (key === "product_ids") {
            mappedData = data[0]?.id || "";
            filterKey = "product_id";
          }
          if (key === "user_ids") {
            filterKey = "assignee_user_ids";
            mappedData = data.map((d: any) => d.id);
          }
          moreFilters = {
            ...moreFilters,
            [filterKey]: mappedData
          };
        });
        setFiltersLoading(false);
        setMoreFilters(moreFilters);
      }
    }
  }, [formState]);

  const limited = useMemo(() => {
    const rbac = ls.getUserRbac();
    return rbac?.toLowerCase() === USERROLES.LIMITED_USER;
  }, []);

  if (filtersLoading) {
    return <Spin className="centered" />;
  }

  return (
    <>
      <Tabs
        activeKey={activeKey}
        animated={false}
        onChange={key => props.history.push(`${getReportsPage()}?tab=${key}`)}
        size={"small"}>
        <TabPane key={"assessments"} tab={"Assessments"}>
          {activeKey === "assessments" && (
            <AssessmentsList moreFilters={{ ...(moreFilters || {}), ...(nonApiFilters || {}) }} />
          )}
        </TabPane>
        {!limited ? (
          <TabPane key={"plugins"} tab={"Plugins"}>
            {activeKey === "plugins" && <PluginResultsList {...props} />}
          </TabPane>
        ) : null}

        {!limited ? (
          <TabPane key={"integrations"} tab={"Integrations"}>
            {activeKey === "integrations" && <ProductAggsList />}
          </TabPane>
        ) : null}
        {!limited ? (
          <TabPane key={"propel"} tab={"Propel Reports"}>
            {activeKey === "propel" && <PropelReportsList />}
          </TabPane>
        ) : null}
      </Tabs>
    </>
  );
};

export default ReportsPage;
