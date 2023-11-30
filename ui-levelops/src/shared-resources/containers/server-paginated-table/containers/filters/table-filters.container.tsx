import "./filters.style.scss";

import { Icon } from "antd";
import { RangePickerValue } from "antd/lib/date-picker/interface";
import React, { useCallback, useMemo } from "react";
import { AntRow } from "shared-resources/components";

import { FILTER_TYPE } from "../../../../../constants/filters";
import APISelectFilter from "./api-select-filter";
import CascadeFilter from "./cascade-filter";
import DateRangeFilter from "./date-range-filter";
import InputFilter from "./input-filter";
import SearchFilter from "./search-filter";
import SelectFilter from "./select-filter";
import TagsFilter from "./tags-filter";

export interface TableFiltersContainerProps {
  filtersConfig: any[];
  onSearchEvent?: any;
  onOptionSelectEvent?: any;
  onInputChange?: any;
  onTagsChange?: any;
  onMultiOptionsChangeEvent?: any; // TODO:
  onExcludeSwitchChange?: any;
  className?: string;
  onCloseFilters: () => void;
}

export const TableFiltersContainer: React.FC<TableFiltersContainerProps> = props => {
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

  const onRestCascadeOptionSelect = useCallback(
    (field: string) => {
      return (option: any) => {
        const filter = props.filtersConfig.filter(rec => rec.field === field);
        let tags = filter[0].selected || [];
        if (option === undefined) {
          //tags = []
        } else {
          tags.push(option);
        }
        props.onOptionSelectEvent?.(field, tags);
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

  const handleRemoveTag = useCallback(
    (field: string, selected: [], index: number) => {
      return (e: any) => {
        e.preventDefault();
        let tags = selected || [];
        tags.splice(index, 1);
        props.onOptionSelectEvent?.(field, tags);
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
        return <DateRangeFilter key={`filter-${index}`} filter={filter} onChange={onRangeSelect(filter.field)} />;
      }

      if ([FILTER_TYPE.API_SELECT, FILTER_TYPE.API_MULTI_SELECT].includes(filter.type)) {
        return <APISelectFilter key={`filter-${index}`} filter={filter} onChange={onRestOptionSelect(filter.field)} />;
      }

      if ([FILTER_TYPE.CASCADE].includes(filter.type)) {
        return (
          <CascadeFilter
            key={`filter-${index}`}
            filter={filter}
            onChange={onRestCascadeOptionSelect(filter.field)}
            onClose={handleRemoveTag(filter.field, filter.selected, index)}
          />
        );
      }
    });
  };

  const gutter = useMemo(() => [16, 16], []);
  return (
    <div className={`${props.className || "filters"}`} data-testid={"rest-api-paginated-filters"}>
      <Icon type="close" style={{ right: "1rem", position: "absolute", zIndex: 1 }} onClick={props.onCloseFilters} />
      <AntRow gutter={gutter} justify={"start"} type={"flex"}>
        {getFilters()}
      </AntRow>
    </div>
  );
};

export default TableFiltersContainer;
