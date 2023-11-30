import React, { useMemo, useState } from "react";
import { Dropdown, Menu } from "antd";
import { default as AntIcon } from "shared-resources/components/ant-icon/ant-icon.component";
import { AntTableComponent as AntTable } from "shared-resources/components/ant-table/ant-table.component";
import { AntTagComponent as AntTag } from "shared-resources/components/ant-tag/ant-tag.component";
import { AntTooltipComponent as AntTooltip } from "shared-resources/components/ant-tooltip/ant-tooltip.component";
import "./orgUnitCategoryDropdown.styles.scss";
import { CATEGORY_SELECTION_PAGE_SIZE, CATEGORY_VALUE_LENGTH_THRESHOLD } from "./constant";
import { ColumnProps } from "antd/lib/table";
import OrgUnitCategoryRowComponent from "./OrgUnitCategoryRowComponent";
import { OUCategoryOptionsType } from "configurations/configuration-types/OUTypes";

interface OrgUnitCategoryDropdownProps {
  onChange: (value: string[]) => void;
  categoryOptions: Array<OUCategoryOptionsType>;
  selectedCategories: Array<string>;
  categoryLoading: boolean;
}

const OrgUnitCategoryDropdownComponent: React.FC<OrgUnitCategoryDropdownProps> = (
  props: OrgUnitCategoryDropdownProps
) => {
  const { categoryLoading, categoryOptions, selectedCategories, onChange } = props;
  const [page, setPage] = useState<number>(1);
  const [dropdownVisible, setDropdownVisible] = useState<boolean>(false);

  const handleCategorySelection = (selectedCategoryValues: string[]) => {
    onChange(selectedCategoryValues);
  };

  const handleTagDelete = (categoryId: string) => {
    const newCategories = (selectedCategories ?? []).filter(id => id !== categoryId);
    onChange(newCategories);
  };

  const categorySelection = useMemo(
    () => ({
      selectedRowKeys: selectedCategories,
      onChange: handleCategorySelection
    }),
    [selectedCategories, onChange]
  );

  const getFinalCategoryOptions = useMemo(() => {
    let high = CATEGORY_SELECTION_PAGE_SIZE * page;
    let low = high - CATEGORY_SELECTION_PAGE_SIZE + 1;
    return categoryOptions?.slice(low - 1, high);
  }, [page, categoryOptions]);

  const onPageChangeHandler = (page: number) => {
    setPage(page);
  };

  const handleMenuClick = () => {
    setDropdownVisible(true);
  };

  const handleDropdownVisibleChange = (curVisibility: boolean) => {
    setDropdownVisible(curVisibility);
  };

  const getCategoryColumn: Array<ColumnProps<any>> = useMemo(
    () => [
      {
        title: "Select All",
        key: "category",
        dataIndex: "label",
        render: (i: string, rec: OUCategoryOptionsType) => <OrgUnitCategoryRowComponent categoryRecord={rec} />
      } as ColumnProps<any>
    ],
    []
  );

  const menu = useMemo(
    () => (
      <Menu onClick={handleMenuClick}>
        <Menu.Item>
          <AntTable
            hasCustomPagination={!categoryLoading}
            rowSelection={categorySelection}
            dataSource={getFinalCategoryOptions}
            onPageChange={onPageChangeHandler}
            pageSize={CATEGORY_SELECTION_PAGE_SIZE}
            page={page}
            totalRecords={(categoryOptions ?? []).length}
            columns={getCategoryColumn}
            rowKey="value"
            size="small"
            className="category-selection-table"
            paginationSize={"small"}
            hideTotal={true}
            showPageSizeOptions={false}
          />
        </Menu.Item>
      </Menu>
    ),
    [
      categorySelection,
      categoryOptions,
      getFinalCategoryOptions,
      categoryLoading,
      page,
      dropdownVisible,
      getCategoryColumn
    ]
  );

  const getSelectedValuesAsTags = useMemo(() => {
    if ((selectedCategories ?? []).length === (categoryOptions ?? []).length) {
      return "All Selected";
    }
    return (selectedCategories ?? []).map((categoryId: string) => {
      const categoryOption: OUCategoryOptionsType | undefined = categoryOptions.find(
        option => option?.value === categoryId
      );
      if (categoryOption) {
        let tagDisplayValue = categoryOption.label;
        const isLongTag = tagDisplayValue.length > CATEGORY_VALUE_LENGTH_THRESHOLD;
        const tagElem = (
          <AntTag
            key={categoryOption.value}
            style={{ backgroundColor: "rgb(198, 211, 251)" }}
            closable
            onClose={() => handleTagDelete(categoryOption.value)}>
            {isLongTag ? `${tagDisplayValue.slice(0, CATEGORY_VALUE_LENGTH_THRESHOLD)}...` : tagDisplayValue}
          </AntTag>
        );

        return isLongTag ? (
          <AntTooltip title={tagDisplayValue} key={categoryOption.value}>
            {tagElem}
          </AntTooltip>
        ) : (
          tagElem
        );
      }
      return "";
    });
  }, [selectedCategories, categoryOptions, onChange]);

  return (
    <div className="ou-category-dropdown-container">
      <Dropdown
        className="ou-category-dropdown"
        overlay={menu}
        trigger={["click"]}
        placement="bottomCenter"
        overlayClassName="ou-dropdown-overlay"
        visible={dropdownVisible}
        onVisibleChange={handleDropdownVisibleChange}>
        <div className="pb-10 pt-10">
          <div>{getSelectedValuesAsTags}</div>
          <AntIcon type={categoryLoading ? "loading" : "down"} className="mr-10" />
        </div>
      </Dropdown>
    </div>
  );
};

export default OrgUnitCategoryDropdownComponent;
