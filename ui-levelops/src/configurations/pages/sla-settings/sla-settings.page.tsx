import React, { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { MappingsFilter } from "./components";
import { ValuesCollapse } from "./components/values.component";
import { Collapse, Empty, Spin } from "antd";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import { RouteComponentProps } from "react-router-dom";
import { FiltersType } from "dashboard/dashboard-types/common-types";
import { fetchSlaData } from "reduxConfigs/actions/restapi/slaModuleAction";
import { slaParamSelector } from "reduxConfigs/selectors/slaModule.Selector";
import { toTitleCase } from "utils/stringUtils";
import { slaTableConfig } from "./components/tableConfig";
import { getPrioritiesUri, typeKey } from "./components/helper";
import { useParamSelector } from "reduxConfigs/selectors/selector";

interface SLASettingsPageProps extends RouteComponentProps {}
const { Panel } = Collapse;
export const SLASettingsPage: React.FC<SLASettingsPageProps> = (props: SLASettingsPageProps) => {
  const dispatch = useDispatch();
  const [filters, setFilters] = useState<FiltersType>({});
  const slaValuesDataState = useParamSelector(slaParamSelector, {
    uri: "sla_module_data",
    method: "list",
    id: "sla_data"
  });
  const [values, setValues] = useState<any>({});
  const [loading, setLoading] = useState<boolean>(false);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("sla_module_data", "list", "sla_data"));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!filters.product_id) {
      return;
    }
    setLoading(true);
    dispatch(fetchSlaData("sla_data", filters || []));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters]);

  useEffect(() => {
    if (filters?.product_id) {
      const loading = get(slaValuesDataState, ["loading"], true);
      const error = get(slaValuesDataState, ["error"], true);
      if (!loading && !error) {
        const data = get(slaValuesDataState, ["data"], []);
        setValues(data);
        setLoading(false);
      }
    } else if (Object.keys(values).length !== 0) {
      setValues({});
      setLoading(false);
    }
  }, [slaValuesDataState]);

  const onFilterChange = (filters: FiltersType) => {
    setFilters(filters);
  };

  const columns = (application: string, type_key: string) => {
    const projectOptions = values?.[application]?.find(
      (val: any) => Object.keys(val).length > 0 && Object.keys(val)[0] === "project"
    ) || {
      project: []
    };

    const typeOptions = values?.[application]?.find(
      (val: any) => Object.keys(val).length > 0 && Object.keys(val)[0] === type_key
    ) || {
      [type_key]: []
    };
    return slaTableConfig(application, projectOptions.project, typeOptions?.[type_key]);
  };

  return (
    <>
      <MappingsFilter onChange={onFilterChange} />
      {filters.product_id === undefined && !loading && (
        <Empty
          description={
            "Choose project and integrations to configure SLAs. These SLAs can be used as filters in your insights"
          }
        />
      )}
      {!loading && (
        <Collapse accordion={false} style={{ paddingBottom: "0px" }} defaultActiveKey={Object.keys(values)}>
          {Object.keys(values).map(application => {
            return (
              <Panel header={toTitleCase(application) as any} style={{ margin: "0px" }} key={application}>
                <ValuesCollapse
                  values={{ [application]: values[application] }}
                  filters={filters}
                  application={application}
                  priorityUri={getPrioritiesUri(application)}
                  columns={columns(application, typeKey(application))}
                />
              </Panel>
            );
          })}
        </Collapse>
      )}
      {loading && <Spin className="centered" />}
    </>
  );
};

export default React.memo(SLASettingsPage);
