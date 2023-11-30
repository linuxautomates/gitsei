import { Button, Checkbox, Form, Popover } from "antd";
import {
  widgetDataSortingOptions,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { get, isArray, isEqual } from "lodash";
import React, { useEffect, useMemo } from "react";
import { GithubPicker } from "react-color";
import { colorPalletteShades } from "shared-resources/charts";
import {
  AntText,
  CustomSelect,
  TableRowActions,
  AntFormItem,
  AntSelect,
  FilterLabel,
  AntInput,
  AntButton,
  AntSwitch
} from "shared-resources/components";
import "./Style.scss";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { LEVELOPS_TABLE_MULTI_STACK_ERROR } from "dashboard/constants/applications/levelops-constant";

export type YAxisType = {
  label: string;
  value: string;
  key: string;
  format: string;
  show_trend_line: boolean;
  display_color: string;
};

interface ConfigTableWidgetAxisProps {
  setYAxis: (key: string, value: any, yIndex: number) => void;
  setValues: (key: string, value: any) => void;
  addYAxis: () => void;
  xAxisValue: any;
  yAxisOptions: Array<any>;
  xAxisOptions: Array<any>;
  yAxis: Array<YAxisType>;
  groupBy: boolean;
  setGroupBy: (value: boolean) => void;
  setStackBy: (value: string[]) => void;
  stackBy: string[] | undefined;
  onFilterValueChange?: (key: string, value: any, isMetaData?: boolean) => void;
  widgetMetadata?: Record<string, any>;
  widgetType?: string;
}

const graphOptions = (): Array<any> => {
  return [
    { label: "Bar", key: "bar" },
    { label: "Area", key: "area" },
    { label: "Line", key: "line" }
  ];
};

const ConfigTableWidgetAxis: React.FC<ConfigTableWidgetAxisProps> = props => {
  const { widgetType } = props;
  const stackByFilterMode: string = getWidgetConstant(widgetType, ["stackByFilterMode"], "multiple");
  const isAllowMultipleStackSelection = getWidgetConstant(widgetType, ["isAllowMultipleStackSelection"], true);
  const errorInMultiStackSelection =
    !isAllowMultipleStackSelection && isArray(props.stackBy) && props.stackBy.length > 1;

  const checkForYAxis = (value: any) => {
    const newYaxis = props.yAxis.map((yA: any) => {
      if (yA.key === value) {
        return {
          key: "",
          value: "",
          label: ""
        };
      }
      return yA;
    });
    props.setValues("yAxis", newYaxis);
  };

  /** filtering stackby values if not present in the columns */
  useEffect(() => {
    if (props.yAxisOptions?.length) {
      const stackOptions = (props.yAxisOptions ?? []).map(option => option?.key);
      const newStackByOptions = (props.stackBy ?? []).filter(key => stackOptions.includes(key));
      if (!isEqual(newStackByOptions, props.stackBy)) {
        props.setStackBy(newStackByOptions);
      }
    }
  }, [props.yAxisOptions, props.stackBy]);

  const handleXAxisChange = (value: any) => {
    checkForYAxis(value);
    props.setValues("xAxis", value);
  };

  const onRemoveHandler = (key: string) => {
    const newYaxis = props.yAxis.filter(axis => axis.key !== key);
    props.setValues("yAxis", newYaxis);
  };

  const buildActionOptions = (key: string) => {
    const actions = [
      {
        type: "delete",
        id: key,
        onClickEvent: onRemoveHandler
      }
    ];
    return <TableRowActions actions={actions} />;
  };

  const getModifiedYAxisOptions = (item: YAxisType) => {
    const currentYAxisOption = (props.xAxisOptions || []).filter(options => options.key === item.key);
    let modifiedOptions = [...(props.yAxisOptions || [])];
    if ((currentYAxisOption || []).length) {
      modifiedOptions = [...modifiedOptions, currentYAxisOption[0]];
    }
    return modifiedOptions;
  };

  const mappedGraphTypeOptions = useMemo(() => {
    if (props.yAxis.length > 1) return graphOptions();
    return [...graphOptions(), { label: "Donut", key: "donut" }];
  }, [props.yAxis]);

  const hasDonutChartSelected = useMemo(() => {
    return !!props.yAxis.find((axis: any) => axis.value === "donut");
  }, [props.yAxis]);

  const tooltipLabelHelp = useMemo(
    () => (
      <div>
        Tooltip Label <span style={{ color: "red" }}>*</span>
      </div>
    ),
    []
  );
  return (
    <div className="axis-container">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Form.Item label="X-Axis" style={{ marginTop: "1.5rem" }}>
          <Checkbox
            disabled={hasDonutChartSelected}
            checked={props.groupBy}
            onChange={event => props.setGroupBy(event.target.checked)}>
            <AntText style={{ fontWeight: 600, fontSize: "0.8rem" }}>GROUP BY</AntText>
          </Checkbox>
          <CustomSelect
            valueKey="key"
            labelKey="label"
            labelCase="none"
            mode="default"
            createOption={false}
            onChange={handleXAxisChange}
            value={props.xAxisValue}
            options={props.xAxisOptions}
          />
          <AntFormItem label={"Sort X-Axis"}>
            <AntSelect
              options={widgetDataSortingOptions[widgetDataSortingOptionsNodeType.NON_TIME_BASED]}
              defaultValue={widgetDataSortingOptions[widgetDataSortingOptionsNodeType.NON_TIME_BASED][0]}
              onChange={(e: any) => props.onFilterValueChange && props.onFilterValueChange("sort_xaxis", e, true)}
              value={props?.widgetMetadata?.sort_xaxis}
            />
          </AntFormItem>
        </Form.Item>
      </div>
      {props.groupBy && (
        <div>
          <AntFormItem label="Stack By">
            <AntSelect
              showArrow
              mode={stackByFilterMode}
              allowClear
              options={(props.yAxisOptions || []).map(item => ({ label: item.label, value: item.key }))}
              value={props.stackBy}
              {...(errorInMultiStackSelection && {
                style: {
                  borderBottom: "1px solid #ff0000"
                }
              })}
              onChange={(value: string | string[]) => {
                if (typeof value === "string") {
                  props.setStackBy([value]);
                } else {
                  props.setStackBy(value);
                }
              }}
            />
            {errorInMultiStackSelection && (
              <p style={{ color: "#ff0000", fontSize: "12px", lineHeight: 1.5 }}>{LEVELOPS_TABLE_MULTI_STACK_ERROR}</p>
            )}
          </AntFormItem>
        </div>
      )}
      {!props.groupBy && (
        <>
          <div className="axis-container__y">
            {props.yAxis.map((item: YAxisType, index: number) => {
              const formItemLabel = index ? `Y-Axis ${index + 1}:` : "Y-Axis:";
              const newYAxisOptions = getModifiedYAxisOptions(item);
              return (
                <div className="axis-container__y-item" key={`${item.key}-${index}`}>
                  <div className="axis-container__y-item-left">
                    <div className="axis-container__y-item-label">
                      <FilterLabel label={formItemLabel} />
                    </div>
                    <div className="flex w-100">
                      <AntFormItem help="Data Source Column">
                        <CustomSelect
                          valueKey="key"
                          labelKey="label"
                          labelCase="none"
                          mode="default"
                          createOption={false}
                          onChange={value => props.setYAxis("key", value, index)}
                          value={item.key}
                          options={newYAxisOptions}
                        />
                      </AntFormItem>
                      <AntFormItem className="flex-1" help="Graph Type" required>
                        <CustomSelect
                          valueKey="key"
                          labelKey="label"
                          labelCase="title_case"
                          mode="default"
                          createOption={false}
                          onChange={value => props.setYAxis("value", value, index)}
                          value={item.value}
                          options={mappedGraphTypeOptions}
                          disabled={!item.key}
                        />
                      </AntFormItem>
                    </div>
                    <div className="axis-container_y-item-bottom flex w-100">
                      <AntFormItem className="flex-1 mr-10" help={tooltipLabelHelp}>
                        <AntInput
                          value={item.label}
                          onChange={(e: any) => props.setYAxis("label", e.target.value, index)}
                        />
                      </AntFormItem>
                      <AntFormItem className="flex-1 mr-10" help="Format">
                        <AntSelect
                          options={[
                            { label: "Plain Text", value: "plain_text" },
                            { label: "Integer", value: "integer" },
                            { label: "Decimal", value: "decimal" }
                          ]}
                          //@ts-ignore
                          value={item.format || "plain_text"}
                          onSelect={(value: any) => props.setYAxis?.("format", value, index)}
                        />
                      </AntFormItem>
                    </div>
                    {item.value === "bar" && (
                      <>
                        <div className="axis-container_y-item-bottom flex w-100">
                          <AntFormItem className="flex-1 mr-10" help="Display Color">
                            <Popover
                              title={null}
                              content={
                                <GithubPicker
                                  width={"187px"}
                                  color={item.display_color}
                                  triangle={"hide"}
                                  onChangeComplete={color => props.setYAxis("display_color", color.hex, index)}
                                  colors={colorPalletteShades}
                                />
                              }>
                              <AntInput
                                value={item.display_color}
                                className="display_color_input"
                                prefix={
                                  <div
                                    style={{
                                      height: 20,
                                      width: 20,
                                      borderRadius: "100%",
                                      backgroundColor: item.display_color,
                                      marginRight: 10
                                    }}
                                  />
                                }
                              />
                            </Popover>
                          </AntFormItem>
                        </div>
                        <div className="w-100 mt-20">
                          <AntSwitch
                            onChange={(value: boolean) => props.setYAxis?.("show_trend_line", value, index)}
                            checked={item.show_trend_line ?? false}
                          />
                          <span className="ml-10">Show Trend Line</span>
                        </div>
                      </>
                    )}
                  </div>
                  <div className="axis-container__y-item-right">{buildActionOptions(item.key)}</div>
                </div>
              );
            })}
          </div>
          <Button
            onClick={props.addYAxis}
            icon={"plus"}
            style={{ width: "fit-content", marginTop: "1rem" }}
            disabled={!props.xAxisValue || hasDonutChartSelected}>
            Add Y-Axis
          </Button>
          <div className="w-100 mt-20">
            <AntSwitch
              onChange={(value: boolean) => props.setValues?.("show_baseline", value)}
              checked={get(props.widgetMetadata, ["show_baseline"], false)}
            />
            <span className="ml-10">Show BaseLine</span>
          </div>
        </>
      )}

      {props.groupBy && (
        <div className="w-100 mt-20">
          <AntSwitch
            onChange={(value: boolean) => props.setValues?.("show_baseline", value)}
            checked={get(props.widgetMetadata, ["show_baseline"], false)}
          />
          <span className="ml-10">Show BaseLine</span>
        </div>
      )}
      {props.groupBy && (
        <div className="w-100 mt-20">
          <AntSwitch
            onChange={(value: boolean) => props.setValues?.("show_trend_line", value)}
            checked={get(props.widgetMetadata, ["show_trend_line"], false)}
          />
          <span className="ml-10">Show Trending</span>
        </div>
      )}
    </div>
  );
};

export default ConfigTableWidgetAxis;
