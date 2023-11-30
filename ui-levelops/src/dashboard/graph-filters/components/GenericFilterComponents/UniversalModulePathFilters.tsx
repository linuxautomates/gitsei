import React, { useCallback, useEffect, useMemo, useState } from "react";
import { AutoComplete, Form } from "antd";
import { get } from "lodash";
import { ApiDropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { useGenericApi } from "custom-hooks";
import { stringSortingComparator } from "../sort.helper";
import { isFile } from "shared-resources/charts/grid-view-chart/util";
import { FileReportRootURIs } from "dashboard/constants/helper";
import { AntIcon, AntInput, NewCustomFormItemLabel } from "shared-resources/components";

interface UniversalModulePathFiltersFilterProps {
  filterProps: LevelOpsFilter;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
}
const { Option } = AutoComplete;
const UniversalModulePathFilters: React.FC<UniversalModulePathFiltersFilterProps> = ({
  filterProps,
  onFilterValueChange
}) => {
  const { filterMetaData, beKey, label, id, allFilters, hideFilter, metadata } = filterProps;
  const { uri, integration_ids, method } = filterMetaData as ApiDropDownData;

  const [searchQuery, setSearchQuery] = useState<string>("");
  const [options, setOptions] = useState<any[]>([]);
  const [changeDirectory, setChangeDirectory] = useState(false);
  const [focus, setFocus] = useState(false);
  const modulePathValue = get(allFilters ?? {}, [beKey], "");
  const defaultPath = !!modulePathValue ? `${modulePathValue}/` : "";

  const apiCallConfig: Array<any> = useGenericApi(
    {
      id,
      uri,
      method: (method as any) ?? "list",
      data: {
        filter: {
          integration_ids,
          [beKey]: modulePathValue
        }
      }
    },
    [modulePathValue]
  );

  const apiLoading: boolean = apiCallConfig[0];
  const apiData = apiCallConfig[1];

  useEffect(() => {
    if (!apiLoading && (apiData ?? []).length) {
      const options = (apiData ?? []).map((item: any) => {
        return {
          key: item.key,
          repoId: item.repo_id
        };
      });
      setOptions(options);
    }
  }, [apiData, apiLoading]);

  useEffect(() => {
    if (changeDirectory) {
      const searchText = searchQuery?.split("/").pop();
      setSearchQuery(`${defaultPath}${searchText}`);
      setChangeDirectory(false);
    } else {
      setSearchQuery(defaultPath);
    }
  }, [modulePathValue, changeDirectory, searchQuery, defaultPath]);

  const filteredOptions = useMemo(() => {
    if (apiLoading) {
      return [];
    }
    return options
      .filter(option => {
        const searchText = searchQuery.startsWith(defaultPath) ? searchQuery.slice(defaultPath.length) : searchQuery;
        return option?.key.includes(searchText);
      })
      .sort(stringSortingComparator("repoId"))
      .map((item, index) => {
        return (
          <Option
            key={`${id}-${index}`}
            value={!!modulePathValue ? `${modulePathValue}/${item.key}` : item.key}
            disabled={isFile(item.key)}>
            {`${item.repoId}/${defaultPath}${item.key}`}
          </Option>
        );
      });
  }, [uri, apiLoading, options, searchQuery, modulePathValue]);

  const handleSelect = useCallback(
    (value, option) => {
      let data: any = {
        module: value
      };
      if (
        [FileReportRootURIs.SCM_FILES_REPORT, FileReportRootURIs.SCM_JIRA_FILES_REPORT].includes(
          uri as FileReportRootURIs
        )
      ) {
        data = {
          ...data,
          repoId: option.key
        };
      }
      !isFile(value) && onFilterValueChange(data, beKey);
    },
    [onFilterValueChange]
  );

  const handleSearch = useCallback(
    search => {
      if (search.startsWith(defaultPath)) {
        const paths = search.split("/");
        const searchString = paths.pop();
        const currentPathWithRoot = paths.join("/");
        if (searchString) {
          setSearchQuery(search);
        }
        if (`${currentPathWithRoot}/` !== defaultPath) {
          setChangeDirectory(true);
          setSearchQuery(searchString);
          const currentDirPath = paths;
          onFilterValueChange(
            {
              module: currentDirPath.length ? currentDirPath.join("/") : "",
              repoId: ""
            },
            beKey
          );
        } else {
          setSearchQuery(search);
        }
      } else {
        setChangeDirectory(true);
        setSearchQuery(search);
        modulePathValue &&
          onFilterValueChange({
            module: "",
            repoId: ""
          });
      }
    },
    [modulePathValue, searchQuery, onFilterValueChange]
  );

  const handleBlur = () => {
    setSearchQuery(defaultPath);
    setFocus(false);
  };

  const handleFocus = () => {
    setFocus(true);
  };

  const inputSuffix = useMemo(() => {
    return <AntIcon className="search-icon" type={apiLoading ? "loading" : "search"} />;
  }, [apiLoading, focus]);

  const inputField = useMemo(() => {
    return <AntInput className="search-field" placeholder="Module Paths" suffix={inputSuffix} />;
  }, [apiLoading, focus]);

  const isFilterHidden = useMemo(() => {
    if (typeof hideFilter === "boolean") return hideFilter;
    if (hideFilter instanceof Function) return hideFilter({ filters: allFilters, metadata });
    return false;
  }, [hideFilter, allFilters, metadata]);

  if (isFilterHidden) return null;

  return (
    <Form.Item
      key={id}
      label={<NewCustomFormItemLabel label={label} />}
      extra={"Type / after folder name to traverse inside the selected directory"}>
      <div className="module-path-filter">
        <AutoComplete
          value={searchQuery}
          dropdownMatchSelectWidth
          notFoundContent={apiLoading ? "Loading Data..." : "No Data"}
          dataSource={filteredOptions}
          onSelect={handleSelect}
          onSearch={handleSearch}
          onFocus={handleFocus}
          onBlur={handleBlur}>
          {inputField}
        </AutoComplete>
      </div>
    </Form.Item>
  );
};

export default UniversalModulePathFilters;
