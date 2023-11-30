import { Form } from "antd";
import { APIFilters } from "dashboard/graph-filters/components";
import { map } from "lodash";
import React, { useMemo, useState } from "react";
import { EditableTag } from "shared-resources/components";
import "./CategoryFiltes.style.scss";

interface CategoriesRuleProps {
  filters: any;
  filtersData: any[];
  customData: any[];
  partialFilterError: any;
  handlePartialValueChange: (key: string, value: any) => void;
  handleSwitchValueChange: (key: string, value: boolean) => void;
  handleFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  handleLastSprintChange?: (value: boolean, filterKey: string) => void;
}
const CategoriesRuleComponent: React.FC<CategoriesRuleProps> = ({
  filtersData,
  filters,
  customData,
  partialFilterError,
  handlePartialValueChange,
  handleSwitchValueChange,
  handleFilterValueChange,
  handleLastSprintChange
}) => {
  const [activePopKey, setActivePopKey] = useState<string | undefined>();

  const transformCustomData = useMemo(() => {
    return map(customData, (item: any) => {
      const dataKey = `${item.key}@${item.name}`;
      return {
        [dataKey]: item.values || []
      };
    });
  }, [customData]);

  return (
    <>
      <p className="category-filters-title">Jira Filters</p>
      <div className="category-filters">
        <APIFilters
          data={filtersData}
          filters={filters}
          supportExcludeFilters={true}
          supportPartialStringFilters={true}
          handlePartialValueChange={handlePartialValueChange}
          handleFilterValueChange={handleFilterValueChange}
          handleSwitchValueChange={handleSwitchValueChange}
          partialFilterError={partialFilterError}
          reportType={""} // will remove afterwards
          activePopkey={activePopKey}
          handleActivePopkey={key => setActivePopKey(key)}
          handleLastSprintChange={handleLastSprintChange}
        />
        {customData && (
          <APIFilters
            data={transformCustomData}
            filters={filters}
            supportExcludeFilters={true}
            supportPartialStringFilters={true}
            handlePartialValueChange={handlePartialValueChange}
            handleFilterValueChange={handleFilterValueChange}
            handleSwitchValueChange={handleSwitchValueChange}
            partialFilterError={partialFilterError}
            reportType={""} // will remove afterwards
            isCustom={true}
            activePopkey={activePopKey}
            handleActivePopkey={key => setActivePopKey(key)}
            handleLastSprintChange={handleLastSprintChange}
          />
        )}
        <Form.Item key={"epics"} label={"Epics"}>
          <EditableTag
            tagLabel={"Add Epic"}
            tags={filters["epics"]}
            onTagsChange={(value: string[]) => handleFilterValueChange(value, "epic")}
          />
        </Form.Item>
      </div>
    </>
  );
};

export default CategoriesRuleComponent;
