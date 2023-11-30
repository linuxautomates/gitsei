import React, { useCallback, useState } from "react";
import { Popconfirm, Select } from "antd";

import "./WidgetLibraryFilterBtn.scss";
import { AntButton, AntCheckbox, AntSwitch, AntText } from "../../../../shared-resources/components";
import { getAllApplications, getAllCategoriesOptions } from "../reportHelper";
import { useDispatch } from "react-redux";
import { updateWidgetLibraryList } from "reduxConfigs/actions/widgetLibraryActions";
import { ProjectPathProps } from "classes/routeInterface";
import { useParams } from "react-router-dom";

const { Option } = Select;

interface WidgetLibraryFiltersProps {
  hideCategoryFilter?: boolean;
}

const WidgetLibraryFilterBtn: React.FC<WidgetLibraryFiltersProps> = ({ hideCategoryFilter }) => {
  const dispatch = useDispatch();
  const [applications, setApplications] = useState<string[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [showSupportedReportsOnly, setShowSupportedReportsOnly] = useState(false);
  const projectParams = useParams<ProjectPathProps>();

  const handleFilterClick = useCallback(
    () =>
      dispatch(
        updateWidgetLibraryList({
          applications,
          categories,
          supported_only: showSupportedReportsOnly
        })
      ),
    [applications, categories, showSupportedReportsOnly]
  );

  const handleShowSupportedFilterChange = useCallback((event: any) => {
    setShowSupportedReportsOnly(event.target.checked);
  }, []);

  function renderTitle() {
    return (
      <div className="widget-library-filters">
        <AntText className="widget-library-filters--title" strong>
          Filters
        </AntText>
        <AntText className="widget-library-filters--type-heading">Applications</AntText>
        <Select
          mode="multiple"
          showArrow
          onChange={setApplications}
          value={applications}
          placeholder="Select Applications">
          {getAllApplications().map((item: any) => (
            <Option key={item.key} value={item.key}>
              {item.label}
            </Option>
          ))}
        </Select>
        {!hideCategoryFilter && (
          <>
            <AntText className="widget-library-filters--type-heading">Categories</AntText>
            <Select
              showArrow
              mode="multiple"
              onChange={setCategories}
              value={categories}
              placeholder="Select Categories">
              {getAllCategoriesOptions(projectParams).map((item: any) => (
                <Option key={item.key} value={item.key}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </>
        )}
        <div className="mt-10 exclude-btn">
          <AntCheckbox className="ml-3" onChange={handleShowSupportedFilterChange} checked={showSupportedReportsOnly}>
            <AntText className="title">Exclude unavailable widgets</AntText>
          </AntCheckbox>
        </div>
      </div>
    );
  }

  return (
    <Popconfirm
      icon={null}
      placement="rightTop"
      title={renderTitle()}
      onConfirm={handleFilterClick}
      okText="Apply"
      cancelText="Cancel">
      <AntButton className="filter-action-btn" icon="filter" />
    </Popconfirm>
  );
};

export default WidgetLibraryFilterBtn;
