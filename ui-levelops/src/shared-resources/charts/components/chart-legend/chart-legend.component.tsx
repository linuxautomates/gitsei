import React, { useCallback, useMemo } from "react";
import { Button, Dropdown, Menu, Divider } from "antd";
import { AntCheckboxComponent as AntCheckbox } from "shared-resources/components/ant-checkbox/ant-checkbox.component";
import { default as AntIcon } from "shared-resources/components/ant-icon/ant-icon.component";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";
import { calcLegendPivotIndex } from "../../helper";
import "./chart-legend.style.scss";
import { toTitleCase } from "../../../../utils/stringUtils";
import widgetConstants from "../../../../dashboard/constants/widgetConstants";
import { TRANSFORM_LEGEND_DATAKEY, TRANSFORM_LEGEND_LABEL } from "dashboard/constants/applications/names";
import { get } from "lodash";
import { TREND_LINE_DIFFERENTIATER } from "shared-resources/charts/constant";
import { ChartLegendProps, FilterActionType } from "./chart-legend.types";
import { LegendMenuFilterItems } from "./LegendMenuFilterItems";
import { chartLegendSortingComparator, getUpdatedFilters } from "./chart-legend.utils";

const ChartLegendComponent: React.FC<ChartLegendProps> = (props: ChartLegendProps) => {
  const {
    payload,
    filters,
    setFilters,
    width,
    allowLabelTransform,
    labelMapping,
    readOnlyLegend,
    report = undefined,
    legendsProps = undefined,
    trendLineKey = undefined
  } = props;
  let slicedPayload = ((payload || []).slice(0, (payload || []).length) || []).sort(chartLegendSortingComparator);
  if (trendLineKey) {
    slicedPayload = slicedPayload.filter((data: any) => data?.dataKey !== trendLineKey);
  }
  const filterKeys = useMemo(() => (filters && Object.keys(filters).length > 1 ? Object.keys(filters) : []), [filters]);
  const hasFilters = useMemo(() => filters && Object.keys(filters).length > 1, [filters]);

  const legendFormatter = useCallback(
    value => {
      if (labelMapping) {
        value = labelMapping?.[value] || value;
      }

      if (allowLabelTransform === false) {
        return value;
      }
      const transformLegendLabelFn = get(widgetConstants, [report || "", TRANSFORM_LEGEND_LABEL], undefined);
      if (transformLegendLabelFn) {
        return value ? transformLegendLabelFn(value, legendsProps) : "-";
      }
      return value ? toTitleCase(value?.replace("_", " ")) : "_";
    },
    [allowLabelTransform, labelMapping]
  );

  const transformDataKey = useCallback(key => {
    const transformFn = get(widgetConstants, ["azure_resolution_time_report", TRANSFORM_LEGEND_DATAKEY], undefined);
    if (transformFn) return transformFn(key);
    return key;
  }, []);

  const pivotIndex = useMemo(() => {
    return calcLegendPivotIndex(filterKeys, legendFormatter, width);
  }, [filters, width]);

  const listFilterKeys = useMemo(
    () => (hasFilters ? (slicedPayload || []).slice(0, pivotIndex) : []),
    [payload, filters, width]
  );

  const dropdownFilterKeys = useMemo(
    () => (hasFilters ? (slicedPayload || []).slice(pivotIndex) : []),
    [payload, filters, width]
  );

  const handleFilterChange = useCallback(
    (key: string, value: boolean, action?: FilterActionType) => {
      if (action === FilterActionType.SELECT_ALL) {
        return setFilters(getUpdatedFilters(filters, true));
      } else if (action === FilterActionType.CLEAR) {
        return setFilters(getUpdatedFilters(filters, false));
      }

      if (filters && (filters as any)[key] !== value) {
        const updatedFilters = { ...filters, [key]: value };
        return setFilters(updatedFilters);
      }

      setFilters({ ...(filters || {}), [key]: value });
    },
    [filters, setFilters]
  );

  const handleResetFilters = useCallback(() => {
    let updatedFilters = {};
    Object.keys(filters).forEach(filter => {
      updatedFilters = {
        ...updatedFilters,
        [filter]: true
      };
    });
    setFilters(updatedFilters);
  }, [filters, setFilters]);

  const renderResetButton = useMemo(
    () => (
      <Button type="link" className="reset-btn" onClick={handleResetFilters}>
        Reset Legend
      </Button>
    ),
    [filters, setFilters]
  );

  const renderFiltersList = useMemo(() => {
    return (
      <>
        {listFilterKeys.map((entry: any) => {
          const { dataKey, color } = entry;
          if (dataKey.includes(TREND_LINE_DIFFERENTIATER)) {
            return (
              <span className="mr-20">
                <AntIcon type="dash" className="mr-5 ml-5" style={{ color, fontSize: "1rem" }} />
                {`${legendFormatter(transformDataKey(dataKey))} Line`}
              </span>
            );
          }
          if (readOnlyLegend) {
            return (
              <div className="indicator-item">
                <div className="indicator-color" style={{ backgroundColor: color }} />
                <AntText className="indicator-title">{dataKey}</AntText>
              </div>
            );
          }
          return (
            <AntCheckbox
              key={`filter-${dataKey}`}
              className={`legend-checkbox`}
              style={{ textTransform: allowLabelTransform === false ? "" : "capitalize", "--tick-color": color }}
              indeterminate={filters[dataKey]}
              checked={filters[dataKey]}
              onChange={(input: any) => handleFilterChange(dataKey, input.target.checked)}>
              {legendFormatter(transformDataKey(dataKey))}
            </AntCheckbox>
          );
        })}
      </>
    );
  }, [payload, filters, setFilters, width, allowLabelTransform, labelMapping, readOnlyLegend]);

  const renderFiltersMenu = () => {
    return (
      <Menu>
        <LegendMenuFilterItems
          dropdownFilterKeys={dropdownFilterKeys}
          filters={filters}
          handleFilterChange={handleFilterChange}
          legendFormatter={legendFormatter}
          allowLabelTransform={allowLabelTransform}
          key={"legend-menu-filter-items"}
          readOnlyLegend={readOnlyLegend}
        />
      </Menu>
    );
  };

  const renderFiltersDropdown = useMemo(() => {
    if (!dropdownFilterKeys.length) {
      return null;
    }

    return (
      <Dropdown
        overlay={renderFiltersMenu}
        placement="bottomCenter"
        overlayClassName="legend-filters-container-dropdown"
        trigger={["click"]}>
        <Button type="link" className="more-btn">
          <span>More</span>
          <AntIcon type="down" className="down-arrow" />
        </Button>
      </Dropdown>
    );
  }, [payload, filters, setFilters, width]);

  if (!hasFilters) return null;

  return (
    <div className="chart-legend-container">
      <div className="legend-filters-container">
        {renderFiltersList}
        {renderFiltersDropdown}
      </div>
      {!readOnlyLegend && renderResetButton}
    </div>
  );
};

export default ChartLegendComponent;
