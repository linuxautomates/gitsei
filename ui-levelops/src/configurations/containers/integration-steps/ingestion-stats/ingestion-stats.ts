import { ListResponse } from "reduxConfigs/actions/restapi/response-types/common";
import { get } from "lodash";

interface Request {
  uri: string;
  method: string;
  uuid: string;
}

export interface IngestionStatPayload {
  title: string;
  request: Request;
  dataTransformer: (data: any) => number | string | undefined | any;
  filters: { [key: string]: any };
}

const getFilter = (id: string) => ({ filter: { integration_ids: [id] }, page_size: 1 });

const getRequest = (uri: string, uuid: string, method: string = "list"): Request => ({
  uri,
  method,
  uuid
});

export const ingestionStatsMapping = (id: string): { [key: string]: IngestionStatPayload[] } => ({
  jira: [
    {
      title: "Projects",
      request: getRequest("jira_filter_values", id),
      dataTransformer: getValuesCount("project"),
      filters: {
        fields: ["project"],
        integration_ids: [id],
        filter: {
          integration_ids: [id]
        }
      }
    },
    {
      title: "Tickets",
      request: getRequest("jira_tickets", id),
      dataTransformer: getTotalCount,
      filters: getFilter(id)
    }
  ],
  github: [
    {
      title: "Repos",
      request: getRequest("scm_repos", id),
      dataTransformer: getTotalCount,
      filters: getFilter(id)
    },
    {
      title: "Commits",
      request: getRequest("github_commits_tickets", id),
      dataTransformer: getTotalCount,
      filters: getFilter(id)
    },
    {
      title: "PRs",
      request: getRequest("github_prs_tickets", id),
      dataTransformer: getTotalCount,
      filters: getFilter(id)
    }
  ],
  jenkins: [
    {
      title: "Jobs",
      request: getRequest("jenkins_pipelines_jobs_filter_values", id),
      dataTransformer: getValuesCount("job_normalized_full_name"),
      filters: {
        fields: ["job_normalized_full_name"],
        filter: {
          integration_ids: [id]
        }
      }
    },
    {
      title: "Runs",
      request: getRequest("pipeline_job_runs", id),
      dataTransformer: getTotalCount,
      filters: getFilter(id)
    }
  ],
  zendesk: [
    {
      title: "Tickets",
      request: getRequest("zendesk_tickets", id),
      dataTransformer: getTotalCount,
      filters: getFilter(id)
    },
    {
      title: "Collections",
      request: getRequest("zendesk_filter_values", id),
      dataTransformer: getValuesCount("organization"),
      filters: {
        fields: ["organization"],
        filter: {
          integration_ids: [id]
        }
      }
    }
  ],
  salesforce: [
    {
      title: "Tickets",
      request: getRequest("salesforce_tickets", id),
      dataTransformer: getTotalCount,
      filters: getFilter(id)
    }
  ],
  snyk: [
    {
      title: "Projects",
      request: getRequest("snyk_issues_values", id),
      dataTransformer: getValuesCount("project"),
      filters: {
        fields: ["project"],
        filter: {
          integration_ids: [id]
        }
      }
    },
    {
      title: "Issues",
      request: getRequest("snyk_issues_report", id),
      dataTransformer: getTotalCount,
      filters: { ...getFilter(id), across: "type" }
    }
  ],
  pagerduty: [
    {
      title: "Incidents",
      request: getRequest("pagerduty_incidents", id),
      dataTransformer: getTotalCount,
      filters: getFilter(id)
    }
  ]
});

const getTotalCount = (list: ListResponse<{}>) => {
  return list?._metadata?.total_count || "-";
};

const getValuesCount = (key: string) => {
  return (list: ListResponse<{ [key: string]: { key: string; total_tickets: number }[] }>) => {
    const valuesData = get(list, ["records", "0", key], []);
    return valuesData.length || "-";
  };
};
