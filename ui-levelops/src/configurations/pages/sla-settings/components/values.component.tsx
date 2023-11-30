import React, { useEffect, useState } from "react";
import { Row, Col, Collapse, Button, InputNumber } from "antd";
import { AntText } from "../../../../shared-resources/components";
import { connect, useDispatch, useSelector } from "react-redux";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { ServerPaginatedTable } from "../../../../shared-resources/containers";
import { getLoading } from "../../../../utils/loadingUtils";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { get } from "lodash";
import { azurePrioritiesUpdate, jiraPrioritiesUpdate } from "reduxConfigs/actions/restapi";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { slaParamSelector } from "reduxConfigs/selectors/slaModule.Selector";
import { IntegrationTypes } from "constants/IntegrationTypes";

const { Panel } = Collapse;

export const tagColorArray = ["red", "blue", "green", "orange", "purple", "cyan"];
interface ValuesCollapseProps {
  values: any;
  filters: any;
  application: string;
  priorityUri: string;
  columns: any;
}

export const ValuesCollapse: React.FC<ValuesCollapseProps> = (props: ValuesCollapseProps) => {
  const { application, columns, priorityUri } = props;
  const values = props.values || {};
  const filters = props.filters || {};
  const [sla, setSla] = useState<any>({});
  const [slaFilters, setSlaFilters] = useState<any>({});
  const [putSLA, setPutSLA] = useState<any>(undefined);
  const [reload, setReload] = useState<any>({});
  const dispatch = useDispatch();
  const prioritiesState = useParamSelector(slaParamSelector, { uri: priorityUri, method: "update", id: putSLA });

  const priorities: any = Object.keys(values).reduce(
    (acc, value) => {
      if (application === IntegrationTypes.AZURE) {
        return (
          {
            ...acc,
            workitem_priority: values[value].find(
              (val: any) => Object.keys(val).length > 0 && Object.keys(val)[0] === "priority"
            )?.priority
          } || {
            workitem_priority: []
          }
        );
      }
      return (
        {
          ...acc,
          priority: values[value].find((val: any) => Object.keys(val).length > 0 && Object.keys(val)[0] === "priority")
            ?.priority
        } || {
          priority: []
        }
      );
    },
    { priority: [], workitem_priority: [] }
  );

  useEffect(() => {
    // everytime the values change do this
    // find the values for priority and use that to render the collapse
    if (putSLA) {
      // check if loaded, then reset putSLA
      const loading = get(prioritiesState, ["loading"], true);
      if (!loading) {
        const newReload = {
          ...reload,
          [putSLA]: reload[putSLA] ? reload[putSLA] + 1 : 1
        };
        setReload(newReload);
        setPutSLA(undefined);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [prioritiesState]);

  const updateSLA = (priority: any, SLAType: any) => {
    return (value: any) => {
      const updatedSLA = {
        ...sla,
        [priority]: {
          ...((sla[priority] as any) || {}),
          [SLAType]: value * (60 * 60 * 24)
        }
      };
      setSla(updatedSLA);
    };
  };

  const saveSLA = (priority: any, slaType: any) => {
    return () => {
      // make the put call here
      const filter = {
        ...((slaFilters[priority] as any) || {}),
        ...filters,
        priorities: [priority]
      };
      let update = sla[priority] || { resp_sla: 60 * 60 * 24, solve_sla: 60 * 60 * 24 };
      if (!update.resp_sla) {
        update.resp_sla = 60 * 60 * 24;
      }
      if (!update.solve_sla) {
        update.solve_sla = 60 * 60 * 24;
      }
      // bulk update SLA here
      if (slaType === "priority") {
        dispatch(
          jiraPrioritiesUpdate(priority, {
            filter: filter,
            update: update
          })
        );
      } else {
        dispatch(
          azurePrioritiesUpdate(priority, {
            filter: filter,
            update: update
          })
        );
      }

      setPutSLA(priority);
    };
  };

  const updateSLAFilters = (priority: any) => {
    return (filter: any) => {
      let slaf = {
        ...slaFilters,
        [priority]: filter
      };
      setSlaFilters(slaf);
    };
  };

  return (
    <Row>
      <Collapse accordion={true}>
        {priorities &&
          (Object.keys(priorities) || []).map(
            priority =>
              priorities[priority]?.length &&
              priorities[priority].map((p: any) => {
                // TODO: calculate the filters here
                return (
                  //@ts-ignore
                  <Panel
                    header={get(staticPriorties, p.key, p.key)}
                    style={{ margin: "0px" }}
                    key={`${priority}_${p.key}`}>
                    <Row
                      type={"flex"}
                      justify={"end"}
                      style={{ marginBottom: "5px" }}
                      gutter={[10, 10]}
                      align={"middle"}>
                      <Col span={11}>
                        <AntText type={"secondary"}>Update SLAs in bulk for the filtered group below</AntText>
                      </Col>
                      <Col span={6}>
                        <Row>
                          <Col span={12}>
                            <h4>Response SLA (days)</h4>
                          </Col>
                          <Col span={12}>
                            <InputNumber defaultValue={1} onChange={updateSLA(p.key, "resp_sla")} />
                          </Col>
                        </Row>
                      </Col>
                      <Col span={6}>
                        <Row>
                          <Col span={12}>
                            <h4>Resolution SLA (days)</h4>
                          </Col>
                          <Col span={12}>
                            <InputNumber defaultValue={1} onChange={updateSLA(p.key, "solve_sla")} />
                          </Col>
                        </Row>
                      </Col>
                      <Col span={1}>
                        <Button
                          size={"large"}
                          onClick={saveSLA(p.key, priority)}
                          icon={"save"}
                          type={"link"}
                          disabled={putSLA !== undefined}
                        />
                      </Col>
                    </Row>
                    <ServerPaginatedTable
                      uri={props.priorityUri}
                      uuid={p.key}
                      columns={columns}
                      reload={reload[p.key] || 0}
                      hasSearch={false}
                      moreFilters={{
                        ...filters,
                        priorities: [p.key]
                      }}
                      // TODO make this function use p.key for saving the filters
                      onFiltersChange={updateSLAFilters(p.key)}
                    />
                  </Panel>
                );
              })
          )}
      </Collapse>
    </Row>
  );
};

export default ValuesCollapse;
