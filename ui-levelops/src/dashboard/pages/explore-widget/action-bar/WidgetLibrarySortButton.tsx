import React, { useCallback } from "react";
import { SvgIcon } from "../../../../shared-resources/components";
import { Dropdown, Menu } from "antd";

import "./WidgetLibrarySortButton.scss";
import { ReportSortOptions } from "../reportHelper";
import { useDispatch } from "react-redux";
import { updateWidgetLibrarySort } from "reduxConfigs/actions/widgetLibraryActions";

interface WidgetLibrarySortButtonProps {}

const WidgetLibrarySortButton: React.FC<WidgetLibrarySortButtonProps> = ({}) => {
  const dispatch = useDispatch();

  const sortActions = Object.keys(ReportSortOptions).map((key: string) => ({ key, title: ReportSortOptions[key] }));

  const handleSortClick = useCallback((sort: string) => {
    dispatch(updateWidgetLibrarySort(sort));
  }, []);

  function renderMenu() {
    return (
      <Menu>
        {sortActions.map((action, index: number) => {
          return (
            <Menu.Item key={action.key} onClick={() => handleSortClick(action.key)}>
              {action.title}
            </Menu.Item>
          );
        })}
      </Menu>
    );
  }

  return (
    <Dropdown overlay={renderMenu()} placement="bottomRight">
      <div className="action-btn-container">
        <SvgIcon icon="filterList" />
      </div>
    </Dropdown>
  );
};

export default WidgetLibrarySortButton;
