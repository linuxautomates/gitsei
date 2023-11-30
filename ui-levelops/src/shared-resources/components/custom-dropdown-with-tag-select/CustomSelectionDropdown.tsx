import React, { useCallback, useMemo, useState } from "react";
import { Dropdown, Icon, Menu } from "antd";
import { debounce, filter } from "lodash";
import { SearchInput } from "dashboard/pages/dashboard-drill-down-preview/components/SearchInput.component";
import { AntButton } from "..";
import { useHistory, useParams } from "react-router-dom";
import { WebRoutes } from "routes/WebRoutes";
import { ProjectPathProps } from "classes/routeInterface";

interface CustomSelectionDropdownProps {
  selectionRecords: any[];
  handleSelectChange: (value: string) => void;
  onSearchChange?: (value: string) => void;
  loading?: boolean;
}

const CustomSelectionDropdown: React.FC<CustomSelectionDropdownProps> = ({
  selectionRecords,
  handleSelectChange,
  loading,
  onSearchChange
}) => {
  const [searchValue, setSeachValue] = useState<string>("");
  const history = useHistory();
  const projectParams = useParams<ProjectPathProps>();
  const filteredData = useMemo(() => {
    return filter(selectionRecords, record => (record?.name || "").toLowerCase().includes(searchValue.toLowerCase()));
  }, [selectionRecords, searchValue]);

  const handleSearching = useCallback(
    debounce((value: string) => {
      if (onSearchChange) {
        onSearchChange(value);
      } else {
        setSeachValue(value);
      }
    }, 250),
    [onSearchChange]
  );

  const getOptions = useMemo(() => {
    return filteredData.map(record => (
      <div className="dropdown-option">
        <div key={record?.ou_id} onClick={e => handleSelectChange(record?.id)} style={{ width: "90%" }}>
          <div className="name">{record?.name}</div>
        </div>
        <div className="navigation-options">
          <Dropdown
            overlay={
              <Menu>
                <Menu.Item
                  onClick={(e: any) =>
                    history.push(WebRoutes.dashboard.devProductivityDashboard(projectParams, record?.id, record?.ou_id))
                  }>
                  Collection Overview
                </Menu.Item>
                <Menu.Item
                  onClick={(e: any) =>
                    history.push(
                      WebRoutes.organization_page.edit(projectParams, record?.id, record?.version, record?.version)
                    )
                  }>
                  Edit Collection
                </Menu.Item>
              </Menu>
            }>
            <Icon type="setting" />
          </Dropdown>
        </div>
      </div>
    ));
  }, [filteredData, handleSelectChange, searchValue]);

  return (
    <div className="custom-selection-dropdown">
      <SearchInput onChange={handleSearching} loading={loading} />
      <div className="selection-action-buttons">
        <AntButton
          type="primary"
          onClick={(e: any) => history.push(WebRoutes.organization_page.create_org_unit(projectParams))}>
          Add New Collection
        </AntButton>
        <AntButton type="primary" onClick={(e: any) => history.push(WebRoutes.organization_page.root(projectParams))}>
          Manage Collections
        </AntButton>
      </div>
      <div className="selection-list-container">{getOptions}</div>
    </div>
  );
};

export default CustomSelectionDropdown;
