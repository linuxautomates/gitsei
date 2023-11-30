import { Button, Icon, Popover } from "antd";
import Loader from "components/Loader/Loader";
import { SearchInput } from "dashboard/pages/dashboard-drill-down-preview/components/SearchInput.component";
import { cloneDeep, get, map } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AntButton, AntText, AntTooltip } from "shared-resources/components";
import "./restTableFiltersContainer.styles.scss";

interface RestTableAddFilterContainerProps {
  filtersConfig: Array<LevelOpsFilter>;
  filterSaveButtonEnabled?: boolean;
  savingFilters?: boolean;
  hideSaveBtn?: boolean;
  allFilters: any;
  onOptionSelectEvent: (field: string, value: any, type?: any) => void;
  handleFilterSave?: () => void;
  onCloseFilters: () => void;
}

const RestTableAddFilterContainer: React.FC<RestTableAddFilterContainerProps> = (
  props: RestTableAddFilterContainerProps
) => {
  const {
    savingFilters,
    filterSaveButtonEnabled,
    hideSaveBtn,
    filtersConfig,
    allFilters,
    onOptionSelectEvent,
    handleFilterSave,
    onCloseFilters
  } = props;
  const [showFiltersPopOver, setShowFiltersPopOver] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [dropdownOptions, setDropdownOptions] = useState(
    map(filtersConfig, config => ({ selected: false, label: config.label, value: config.beKey }))
  );
  const [orderedKeys, setOrderedKeys] = useState<Array<string>>([]);
  const [savingAndCloseFilters, setSavingAndCloseFilters] = useState(false);

  useEffect(() => {
    if (savingFilters && !savingAndCloseFilters) {
      setSavingAndCloseFilters(true);
    }

    if (!savingFilters && savingAndCloseFilters) {
      setSavingAndCloseFilters(false);
      onCloseFilters?.();
    }
  }, [savingFilters]);

  useEffect(() => {
    const updatedDropdownOptions = cloneDeep(dropdownOptions);
    const newOrderedKeys: Array<string> = [];
    updatedDropdownOptions.forEach(option => {
      if (get(allFilters, [option.value])) {
        option.selected = true;
        newOrderedKeys.push(option.value);
      }
    });
    setDropdownOptions(updatedDropdownOptions);
    setOrderedKeys(newOrderedKeys);
  }, []);

  const filterOptionSelected = (key: string) => {
    const index = dropdownOptions.findIndex(filter => filter.value === key);
    if (index !== -1) {
      const updatedDropdownOptions = cloneDeep(dropdownOptions);
      updatedDropdownOptions[index].selected = true;
      const newOrderKeys = [...orderedKeys, key];
      setOrderedKeys(newOrderKeys);
      setDropdownOptions(updatedDropdownOptions);
      setShowFiltersPopOver(false);
      setSearchQuery("");
    }
  };

  const handleVisibleChange = useCallback(visible => {
    setShowFiltersPopOver(visible);
    setSearchQuery("");
  }, []);

  const handleRemoveFilter = (key: string) => {
    const index = dropdownOptions.findIndex(filter => filter.value === key);
    if (index !== -1) {
      const updatedDropdownOptions = cloneDeep(dropdownOptions);
      updatedDropdownOptions[index].selected = false;
      const newOrderKeys = orderedKeys.filter(_key => _key !== key);
      setOrderedKeys(newOrderKeys);
      setDropdownOptions(updatedDropdownOptions);
      setShowFiltersPopOver(false);
      setSearchQuery("");
      onOptionSelectEvent(key, []);
    }
  };

  const handleFilterValueChange = (value: any, key: string, type = "option") => {
    onOptionSelectEvent(key, value, type);
  };

  const dropdownOptionList = useMemo(
    () =>
      dropdownOptions.filter(
        filter => !filter.selected && filter.label.toLowerCase().includes(searchQuery.toLowerCase())
      ),
    [dropdownOptions, searchQuery]
  );

  const menu = () => {
    return (
      <div className="rest-add-filter-container_dropdown">
        <SearchInput value={searchQuery} onChange={(query: string) => setSearchQuery(query)} />
        <div className="dropdown-list">
          {dropdownOptionList.map(filter => (
            <div key={filter.value} onClick={() => filterOptionSelected(filter.value)} className={"dropdown-list_name"}>
              <AntText className="dropdown-list_select">{filter.label}</AntText>
            </div>
          ))}
        </div>
      </div>
    );
  };

  const renderFilters = () => {
    return orderedKeys.map(item => {
      const filterConfig = filtersConfig.find((filterItem: LevelOpsFilter) => filterItem.beKey === item);
      if (filterConfig) {
        return React.createElement(filterConfig.apiContainer ?? filterConfig.renderComponent, {
          filterProps: {
            ...filterConfig,
            allFilters
          },
          filter: filterConfig,
          onFilterValueChange: handleFilterValueChange,
          handlePartialValueChange: () => {},
          handleRemoveFilter
        });
      }
      return null;
    });
  };

  return (
    <div className={"rest-add-filter-container"}>
      <div className="rest-add-filter-container_heading">
        {savingFilters && <Loader />}
        {!savingFilters && !hideSaveBtn && (
          <AntButton disabled={!filterSaveButtonEnabled} onClick={handleFilterSave}>
            Save and Close
          </AntButton>
        )}
        <Icon type="close" className="close-button" onClick={onCloseFilters} />
      </div>
      <div>
        <Popover
          className="filters-popover"
          placement={"bottomLeft"}
          content={menu()}
          trigger="click"
          visible={showFiltersPopOver}
          onVisibleChange={handleVisibleChange}>
          <AntTooltip title={(dropdownOptionList || []).length == 0 ? "No Filter to add." : null}>
            <Button
              disabled={(dropdownOptionList || []).length == 0}
              className={"mb-10 add-filter-btn"}
              onClick={() => setShowFiltersPopOver(!showFiltersPopOver)}>
              Add Filter <Icon type="down" />
            </Button>
          </AntTooltip>
        </Popover>
      </div>
      <div className="rest-add-filter-container_filters">{renderFilters()}</div>
    </div>
  );
};

export default RestTableAddFilterContainer;
