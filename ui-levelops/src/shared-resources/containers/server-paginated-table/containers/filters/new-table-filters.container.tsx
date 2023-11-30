import "./filters.style.scss";

import { Badge, Icon, Radio } from "antd";
import { RangePickerValue } from "antd/lib/date-picker/interface";
import React, { useCallback, useMemo, useState, useEffect } from "react";
import { AntRow, AntText, AntButton, AntCol } from "shared-resources/components";

import { FILTER_TYPE } from "../../../../../constants/filters";
import { getEndOfDayFromDate, getStartOfDayFromDate } from "../../../../../utils/dateUtils";
import APISelectFilter from "./api-select-filter";
import DateRangeFilter from "./date-range-filter";
import InputFilter from "./input-filter";
import SearchFilter from "./search-filter";
import SelectFilter from "./select-filter";
import TagsFilter from "./tags-filter";
import Loader from "components/Loader/Loader";
import { FilterHeader } from "./header/filter-header";

export interface NewTableFiltersContainerProps {
  filtersConfig: any[];
  onSearchEvent?: any;
  onOptionSelectEvent?: any;
  onInputChange?: any;
  onTagsChange?: any;
  onMultiOptionsChangeEvent?: any; // TODO:
  onExcludeSwitchChange?: any;
  className?: string;
  onCloseFilters: () => void;
  filterSaveButtonEnabled?: boolean;
  handleFilterSave?: () => void;
  savingFilters?: boolean;
  hideSaveBtn?: boolean;
  onBinaryChange?: (field: any, event: any) => void;
}

export const NewTableFiltersContainer: React.FC<NewTableFiltersContainerProps> = props => {
  // this state is use to trigger close when saving is done
  const [savingFilters, setSavingFilters] = useState(false);

  useEffect(() => {
    if (props.savingFilters && !savingFilters) {
      setSavingFilters(true);
    }

    if (!props.savingFilters && savingFilters) {
      setSavingFilters(false);
      props.onCloseFilters && props.onCloseFilters();
    }
  }, [props.savingFilters]);

  const onSearchHandler = useCallback(
    (field: string) => {
      return (value: any) => {
        props.onSearchEvent?.(field, value);
      };
    },
    [props.onSearchEvent]
  );

  const onInputChange = useCallback(
    (field: string) => {
      return (value: any) => {
        props.onInputChange?.(field, value);
      };
    },
    [props.onInputChange]
  );

  const onOptionSelect = useCallback(
    (field: string) => {
      return (value: any) => {
        props.onOptionSelectEvent?.(field, value);
      };
    },
    [props.onOptionSelectEvent]
  );

  const onRestOptionSelect = useCallback(
    (field: string) => {
      return (option: any) => {
        const filter = props.filtersConfig.filter((rec: any) => rec.field === field);
        if (filter.length > 0 && filter[0].returnCall) {
          filter[0].returnCall(field, option ? option : undefined);
        }
        props.onOptionSelectEvent?.(field, option ? option : undefined);
      };
    },
    [props.onOptionSelectEvent, props.filtersConfig]
  );

  const onRangeSelect = useCallback(
    (field: string) => {
      return (dates: RangePickerValue, dateStrings: [string, string]) => {
        if (dates.length > 1) {
          return props.onOptionSelectEvent?.(
            field,
            {
              $gt: dateStrings[0],
              $lt: dateStrings[1]
            },
            "date"
          );
        } else {
          return props.onOptionSelectEvent(field, undefined, "date");
        }
      };
    },
    [props.onOptionSelectEvent]
  );

  const getFilters = () => {
    return props.filtersConfig.map((filter: any, index: number) => {
      if (filter.type === FILTER_TYPE.SEARCH) {
        return <SearchFilter key={`filter-${index}`} filter={filter} onChange={onSearchHandler(filter.field)} />;
      }

      if (filter.type === FILTER_TYPE.INPUT) {
        return <InputFilter key={`filter-${index}`} filter={filter} onChange={onInputChange(filter.field)} />;
      }

      if (filter.type === FILTER_TYPE.TAGS) {
        return (
          <TagsFilter
            key={`filter-${index}`}
            filter={filter}
            onTagsChange={value => props.onTagsChange?.("epics", value)}
          />
        );
      }

      if ([FILTER_TYPE.SELECT, FILTER_TYPE.MULTI_SELECT].includes(filter.type)) {
        return (
          <SelectFilter
            key={`filter-${index}`}
            filter={filter}
            onOptionSelect={onOptionSelect(filter.field)}
            onSwitchValueChange={value => props.onExcludeSwitchChange?.(filter.field, value)}
          />
        );
      }

      if (filter.type === FILTER_TYPE.DATE_RANGE) {
        return (
          <DateRangeFilter
            datePickerClassName={"new-filters__date-picker"}
            key={`filter-${index}`}
            filter={filter}
            onChange={onRangeSelect(filter.field)}
          />
        );
      }

      if ([FILTER_TYPE.API_SELECT, FILTER_TYPE.API_MULTI_SELECT].includes(filter.type)) {
        return <APISelectFilter key={`filter-${index}`} filter={filter} onChange={onRestOptionSelect(filter.field)} />;
      }

      if (filter?.type === FILTER_TYPE.BINARY) {
        return (
          <AntCol key={index} className="gutter-row" span={filter.span ? filter.span : 4}>
            <FilterHeader label={filter?.label} />
            <Radio.Group
              id={`binary-${filter.id}`}
              onChange={(e: any) => props?.onBinaryChange?.(filter.field, e.target.value)}
              value={filter.selected === undefined ? "all" : filter.selected}>
              <Radio value={true}>Yes</Radio>
              <Radio value={false}>No</Radio>
            </Radio.Group>
          </AntCol>
        );
      }
    });
  };

  const filterCount = useMemo(() => {
    let count = 0;
    props.filtersConfig.forEach((filter: any) => {
      if (filter.selected) {
        if (
          (typeof filter.selected === "object" && Object.keys(filter.selected).length > 0) ||
          filter.selected?.toString()?.length > 0
        ) {
          count = count + 1;
        }
      }
    });
    return count;
  }, [props.filtersConfig]);
  const gutter = useMemo(() => [100, 16], []);
  const backgroundColor = useMemo(() => (filterCount ? "var(--harness-blue)" : "#bfbfbf"), [filterCount]);

  return (
    <div className={`${props.className || "new-filters"}`} data-testid={"rest-api-paginated-filters"}>
      <div className="flex align-center filter-heading">
        <AntText className="filter-heading--text">Filters</AntText>
        <Badge
          style={{ backgroundColor, marginLeft: "0.5rem" }}
          count={filterCount}
          showZero={true}
          overflowCount={10}
        />
        <div className="flex-1" />
        {props.savingFilters && <Loader />}
        {!props.savingFilters && !props.hideSaveBtn && (
          <AntButton
            disabled={!props.filterSaveButtonEnabled}
            onClick={() => props.handleFilterSave && props.handleFilterSave()}>
            Save and Close
          </AntButton>
        )}
        <Icon type="close" className="filter-heading__close-button" onClick={props.onCloseFilters} />
      </div>
      <AntRow gutter={gutter} justify={"start"} type={"flex"}>
        {getFilters()}
      </AntRow>
    </div>
  );
};

export default NewTableFiltersContainer;
