import React from "react";
import { useGetSupportedFiltersAndApplication } from "../../../../custom-hooks/useGetSupportedFiltersAndApplication";
import { useSupportedFilters } from "../../../../custom-hooks/useSupportedFilters";
import { Spin } from "antd";
import { DashboardGraphFilters } from "../../../graph-filters/components";

interface GithubJiraFiltersProps {
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeChange?: (key: string, value: boolean) => void;
  onTimeRangeTypeChange?: (key: string, value: any) => void;
  onTimeFilterValueChange?: (value: any, type?: any, rangeType?: string) => void;
  metaData?: any;
  filters: any;
  integrationIds: Array<any>;
}

const GithubJiraFilters: React.FC<GithubJiraFiltersProps> = (props: GithubJiraFiltersProps) => {
  const [githubJiraFilesApplication, githubJiraFilesSupportedFilters] =
    useGetSupportedFiltersAndApplication("scm_jira_files_report");
  const { loading: githubJiraFilesApiLoading, apiData: githubJiraFilesApiData } = useSupportedFilters(
    githubJiraFilesSupportedFilters,
    props.integrationIds,
    githubJiraFilesApplication
  );
  return (
    <div style={{ width: "100%", height: "100%" }}>
      {githubJiraFilesApiLoading && (
        <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100%" }}>
          <Spin />
        </div>
      )}
      {!githubJiraFilesApiLoading && githubJiraFilesApiData && (
        <DashboardGraphFilters
          application={"githubjira"}
          data={githubJiraFilesApiData}
          filters={props.filters}
          reportType={"scm_jira_files_report"}
          onFilterValueChange={props.onFilterValueChange}
          applicationUse={true}
          onExcludeChange={props.onExcludeChange}
          metaData={props.metaData}
          onPartialChange={() => {}}
          integrationIds={props.integrationIds}
        />
      )}
    </div>
  );
};

export default GithubJiraFilters;
