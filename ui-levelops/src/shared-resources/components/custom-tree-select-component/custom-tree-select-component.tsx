import React, { useCallback, useMemo } from "react";
import { convertArrayToTree } from "../custom-tree-select/helper";
import "./custom-tree-select-component.style.scss";
import { Collapse, Checkbox } from "antd";
import { cloneDeep, get, uniq } from "lodash";
import { AntIcon } from "..";
import { structureProvidedData } from "./helper";
const { Panel } = Collapse;

interface CustomTreeSelectComponentProps {
  data: any;
  selected: string[];
  skipStructuring?: boolean;
  onCheckboxValueChange: Function;
  returnAllValues?: boolean;
  createParentChildRelationInData?: boolean;
}

const CustomTreeSelectComponent: React.FC<CustomTreeSelectComponentProps> = props => {
  const structuredData = useMemo(() => {
    if (props?.skipStructuring) {
      return props.data;
    }
    let structData = cloneDeep(props.data);
    if (props?.createParentChildRelationInData) {
      structData = structureProvidedData(props.data || []);
    }
    return convertArrayToTree(structData || []);
  }, [props.data]);

  const selectedValues = useMemo(() => {
    let selectedFromProps = cloneDeep(props.selected);
    // @ts-ignore
    if (selectedFromProps?.filter(_s => _s?.hasOwnProperty("child"))?.length > 0) {
      selectedFromProps = selectedFromProps?.map(_s => get(_s, "child"));
    }
    return selectedFromProps;
  }, [props.selected]);

  const onCheckboxValueChange = (e: any) => {
    if (props?.returnAllValues) {
      let values = cloneDeep(props.selected);
      if (e?.checked) {
        const newIncludes = props.data
          .filter((_datum: any) => _datum.value === e?.value)
          .map((item: any) => item?.value);
        values = uniq([...(values || []), ...newIncludes]);
      } else {
        values = (values || []).filter(v => v !== e?.value);
      }
      return props.onCheckboxValueChange(values);
    }
    props.onCheckboxValueChange(e);
  };

  const getHeader = useCallback(
    (datum: Record<string, any>, addMarginBeforeHeading = false) => {
      const classes = ["accordion-heading-wrap"];
      if (addMarginBeforeHeading) {
        classes.push("shift-heading");
      }
      return (
        <div className={classes.join(" ")}>
          <Checkbox
            checked={selectedValues.includes(datum?.value)}
            onClick={e => e?.stopPropagation()}
            onChange={e => {
              e?.stopPropagation();
              onCheckboxValueChange({ value: datum?.value, checked: e?.target?.checked });
            }}
          />
          <span
            className="accordion-heading"
            onClick={e => {
              e?.preventDefault();
              e?.stopPropagation();
              onCheckboxValueChange({ value: datum?.value, checked: !selectedValues.includes(datum?.title) });
            }}>
            {datum?.title}
          </span>
        </div>
      );
    },
    [props.selected, selectedValues]
  );

  return (
    <>
      {structuredData.map((_datum: any) => {
        if (_datum?.children?.length > 0) {
          return (
            <Collapse
              key={_datum?.key}
              defaultActiveKey={["1"]}
              className="custom-tree-select-component"
              expandIcon={({ isActive }) => <AntIcon type="caret-right" rotate={isActive ? 90 : 0} />}>
              <Panel header={getHeader(_datum)} key="1">
                <CustomTreeSelectComponent
                  data={_datum?.children}
                  selected={selectedValues}
                  skipStructuring={true}
                  onCheckboxValueChange={onCheckboxValueChange}
                />
              </Panel>
            </Collapse>
          );
        }
        return getHeader(_datum, true);
      })}
    </>
  );
};

export default CustomTreeSelectComponent;

CustomTreeSelectComponent.defaultProps = {
  skipStructuring: false
};
