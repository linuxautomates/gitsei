import { Collapse, Drawer, Spin } from "antd";
import React, { CSSProperties, useMemo } from "react";
import ReactJson from "react-json-view";
import { AntBadge, AntCard, AntCol, AntRow, AntTable, AntTag, AntText } from "shared-resources/components";
import { tableCell } from "utils/tableUtils";
import { getTableColumns } from "../../helpers/table-config";
import "./plugin-results-diff.scss";

interface PluginDiffProps {
  diff?: any;
  visible?: boolean;
  onClose?: () => void;
  rows?: any[];
}

const { Panel } = Collapse;

const PluginResultsDiffComponent: React.FC<PluginDiffProps> = (props: PluginDiffProps) => {
  const { onClose, visible, diff, rows } = props;
  const renderJson = useMemo(
    () =>
      (value: any, name = "before") => {
        let valueType = typeof value;
        if (value === null) {
          valueType = "undefined";
        }

        switch (valueType) {
          case "object":
            return <ReactJson src={value} name={name} sortKeys={true} />;
          case "undefined":
            return "";
          case "boolean":
            return value ? <AntTag color={"green"}>true</AntTag> : <AntTag color={"red"}>false</AntTag>;
          default:
            return <AntText code>{value}</AntText>;
        }
      },
    []
  );
  const columns = getTableColumns(renderJson);

  const drawerStyle = useMemo(() => ({ position: "absolute" }), []);

  const getTitleValue = useMemo(
    () => (title: string, value: string) => {
      return (
        <div className="policy-diff-col__item flex align-center">
          <label className="policy-diff-col__title">{title}</label>
          <span className="policy-diff-col__value">{value}</span>
        </div>
      );
    },
    []
  );

  const getObject = useMemo(
    () => (path: string, operation: string, add: string) => {
      return {
        operation: operation,
        path: add,
        before: diff[path]?.data_changes[add]?.before,
        after: diff[path]?.data_changes[add]?.after
      };
    },
    [diff]
  );

  return (
    <Drawer
      height={"100%"}
      title={"Diff Plugin Results"}
      placement={"bottom"}
      closable={true}
      onClose={onClose}
      visible={visible}
      getContainer={false}
      style={drawerStyle as CSSProperties}
      destroyOnClose={true}>
      {!diff && <Spin />}
      {diff && (
        <>
          <AntCard title="Diff Plugin Headers" className="ant-card__diff-plugin-headers">
            <AntRow gutter={[0, 16]} type={"flex"} justify={"space-between"}>
              {(rows || []).map((row: any) => (
                <AntCol className="policy-diff-col" span={12}>
                  {Object.keys(row).map(r => {
                    if (["plugin_name", "created_at_epoch", "successful", "labels", "tags", "workspaces"].includes(r)) {
                      if (r === "created_at_epoch" || r === "successful" || r === "tags") {
                        return getTitleValue(r.replace("_", " "), tableCell(r, row[r]));
                      }
                      if (r === "labels") {
                        let value = row[r];
                        let tags: any = [];
                        if (!value) {
                          return tags;
                        }
                        Object.keys(value).forEach(label => {
                          (value[label] || []).forEach((key: any) => {
                            tags.push(<AntTag>{`${label} ${key}`}</AntTag>);
                          });
                        });
                        return getTitleValue(r, tags);
                      }
                      if (r === "workspaces") {
                        return getTitleValue("Project", row[r].join(","));
                      }
                    }
                  })}
                </AntCol>
              ))}
            </AntRow>
          </AntCard>
          <Collapse defaultActiveKey={[""]}>
            {Object.keys(diff).map((path: string, index: number) => {
              const added = diff[path]?.added?.length;
              const removed = diff[path]?.removed?.length;
              const changed = diff[path]?.changed?.length;
              let tableData = [];
              const addData = diff[path]?.added?.map((add: any) => getObject(path, "added", add));
              tableData.push(...addData);

              const removeData = diff[path].removed.map((add: any) => getObject(path, "removed", add));
              tableData.push(...removeData);

              const changeData = diff[path]?.changed?.map((add: any) => getObject(path, "changed", add));
              tableData.push(...changeData);

              return (
                <Panel
                  key={index}
                  header={
                    <AntRow type={"flex"} justify={"space-between"}>
                      <AntCol>
                        <AntText strong>{path}</AntText>
                      </AntCol>
                      <AntCol>
                        <AntRow type={"flex"} gutter={[15, 0]}>
                          {added > 0 && (
                            <AntCol>
                              <AntBadge count={added}>
                                <AntTag color={"green"}>Added</AntTag>
                              </AntBadge>
                            </AntCol>
                          )}
                          {removed > 0 && (
                            <AntCol>
                              <AntBadge count={removed}>
                                <AntTag color={"red"}>Removed</AntTag>
                              </AntBadge>
                            </AntCol>
                          )}
                          {changed > 0 && (
                            <AntCol>
                              <AntBadge count={changed}>
                                <AntTag color={"blue"}>Changed</AntTag>
                              </AntBadge>
                            </AntCol>
                          )}
                        </AntRow>
                      </AntCol>
                    </AntRow>
                  }>
                  <AntTable columns={columns} dataSource={tableData} pagination={false} />
                </Panel>
              );
            })}
          </Collapse>
        </>
      )}
    </Drawer>
  );
};

export default PluginResultsDiffComponent;
