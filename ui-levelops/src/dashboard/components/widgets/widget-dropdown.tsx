import React from "react";
import { useDispatch } from "react-redux";
import { remoteWidgetUpdate, _widgetUpdateCall } from "reduxConfigs/actions/restapi";
import { AntSelect } from "shared-resources/components";
import "./widget-dropdown-select.style.scss";
interface WidgetSelectOptionsProps {
  selectedValue: string;
  widgetId: string;
  selectOptions: string[];
  mode?: string;
  prefixLabel?: string;
  showArrow?: boolean;
  dataKey: string;
}
const WidgetSelectComponent: React.FC<WidgetSelectOptionsProps> = ({
  selectedValue,
  widgetId,
  mode,
  selectOptions,
  prefixLabel,
  showArrow,
  dataKey
}) => {
  const dispatch = useDispatch();

  const onChange = (value: string) => {
    dispatch(
      remoteWidgetUpdate(widgetId, {
        metadata: {
          [dataKey]: value
        }
      })
    );
  };

  return (
    <>
      {prefixLabel}
      <AntSelect
        options={selectOptions}
        className="widget-filter-select"
        showArrow={showArrow}
        value={selectedValue}
        mode={mode || "single"}
        onChange={onChange}
      />
    </>
  );
};

export default WidgetSelectComponent;
