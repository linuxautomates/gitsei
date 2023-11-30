import { getIntegrationUrlMap } from "constants/integrations";
import { buildApiUrl } from "constants/restUri";
import { capitalize, get, intersection, sortBy, uniq, uniqBy } from "lodash";
import CompactReport from "../../../model/report/CompactReport";
import Report from "../../../model/report/Report";
import { getAllReports } from "../../../utils/reportListUtils";
import { ReportTheme, reportThemes } from "./report.constant";
import { IS_FRONTEND_REPORT } from "../../constants/filter-key.mapping";
import { ProjectPathProps } from "classes/routeInterface";

export interface CompactCategoryReports {
  [category: string]: CompactReport[];
}

export const ReportSortOptions: { [key: string]: string } = {
  atoz: "Sort A -> Z",
  ztoa: "Sort Z -> A"
};

export const HiddenReportMappings = {
  jira_salesforce_report: "jira_zendesk_report",
  salesforce_bounce_report: "zendesk_bounce_report",
  salesforce_hops_report: "zendesk_hops_report",
  salesforce_resolution_time_report: "zendesk_resolution_time_report",
  salesforce_tickets_report: "zendesk_tickets_report",
  salesforce_top_customers_report: "zendesk_top_customers_report",
  salesforce_hygiene_report: "zendesk_hygiene_report",
  jira_salesforce_files_report: "jira_zendesk_files_report",
  jira_salesforce_escalation_time_report: "jira_zendesk_escalation_time_report",
  salesforce_time_across_stages: "zendesk_time_across_stages",
  salesforce_c2f_trends: "zendesk_c2f_trends"
};

export const getAllApplications = () => {
  const integrationMap = getIntegrationUrlMap();
  const applicationKeys = Object.keys(integrationMap);

  return applicationKeys.map((key: string) => {
    const application = (integrationMap as any)[key].application;
    return { key: application, label: capitalize(application) };
  });
};

export const getAllCategoriesOptions = (params: ProjectPathProps) => {
  return reportThemes(params, "").map(report => ({ key: report.key, label: report.label }));
};

export const getAllCompactReports = (): CompactReport[] => {
  const allReports = getAllReports();

  return allReports.map((report: Report) => {
    return {
      key: report.report_type,
      report_type: report.report_type,
      supported_widget_types: report.supported_widget_types || ["graph"], // setting default type as graph
      name: report.name || "",
      applications: [report.application] || [],
      categories: report.category ? [report.category] : ["miscellaneous"],
      description: report?.description,
      [IS_FRONTEND_REPORT]: report?.[IS_FRONTEND_REPORT] || false
    };
  });
};
const getImageUrl = (report: ReportTheme) => {
  let url = report?.["image-url"] || "";
  if (!!url) {
    url = (url.indexOf("//") || url.indexOf("://")) > 0 ? url : buildApiUrl(url);
  }
  return url;
};

export const mapWidgetLibraryApiList = (list: ReportTheme[]) => {
  const modifiedList = uniqBy(
    list.map(report => ({
      ...report,
      id: get(HiddenReportMappings, report.id, report.id)
    })),
    "id"
  );

  const l = modifiedList.map((report: ReportTheme) => ({
    key: report.id,
    report_type: report.id,
    name: "",
    applications: [],
    categories: report["report-categories"],
    content: report.content,
    imageUrl: getImageUrl(report) || "",
    supported_widget_types: [],
    description: report.description || ""
  }));
  return l;
};

const _filterCompactReportsByCategory = (list: CompactReport[], applications: string[], categories: string[]) => {
  return list
    .filter((report: CompactReport) => {
      let addReport = true;
      if (applications.length) {
        addReport = intersection(applications, report.applications).length > 0;
      }

      if (categories.length) {
        addReport = addReport && intersection(categories, report.categories).length > 0;
      }
      return addReport;
    })
    .map((report: CompactReport) => (categories.length ? { ...report, categories } : report));
};

const _filterCompactReportsByQuery = (list: CompactReport[], searchQuery?: string) => {
  const filteredList = list.filter(report => report.name.toLowerCase().includes((searchQuery || "").toLowerCase()));
  return _getReportsByCategory(filteredList);
};

const _getReportsByCategory = (list: CompactReport[]): CompactCategoryReports => {
  const _allCategories = uniq(list.reduce((acc: string[], next) => [...acc, ...next.categories], []));
  return _allCategories.reduce(
    (acc: CompactCategoryReports, category: string) => ({
      ...acc,
      [category]: list.filter(report => report.categories.includes(category))
    }),
    {}
  );
};

const _sortReports = (
  list: CompactCategoryReports,
  order: "popular" | "recent" | "atoz" | "ztoa"
): CompactCategoryReports[] => {
  const keys = Object.keys(list).map(key => key.toLowerCase());
  const sortedKey = [...keys].sort();
  switch (order) {
    case "atoz":
      return sortedKey.map(key => {
        const sortedData = sortBy(list[key], ["name"]);
        return {
          [key]: sortedData
        };
      });
    case "ztoa":
      const reverseSortedKey = [...keys].reverse();
      return reverseSortedKey.map(key => {
        const sortedData = sortBy(list[key], ["name"]).reverse();
        return {
          [key]: sortedData
        };
      });
    default:
      return [];
  }
};

export const getFilteredCompactListByCategory = (
  list: CompactReport[],
  filters?: { applications: string[]; categories: string[]; search_query: string },
  sortOrder?: "atoz" | "ztoa"
) => {
  let filteredList: any = _filterCompactReportsByCategory(list, filters?.applications || [], filters?.categories || []);

  filteredList = _filterCompactReportsByQuery(filteredList, filters?.search_query);

  filteredList = _sortReports(filteredList, sortOrder || "atoz");

  return filteredList;
};

export const getFilteredCompactList = (
  list: CompactReport[],
  filters?: { applications: string[]; categories: string[]; search_query: string },
  sortOrder?: "atoz" | "ztoa"
) => {
  // filtering for application and category
  let filteredList = list.filter(report => {
    let addReport = true;
    if (filters && filters.applications.length > 0) {
      addReport = intersection(report.applications, filters.applications).length > 0;
    }

    if (filters && filters.categories.length > 0) {
      addReport = addReport && intersection(report.categories, filters.categories).length > 0;
    }

    return addReport;
  });

  // filtering for search_query
  if (filters && filters.search_query) {
    filteredList = filteredList.filter(report =>
      report.name.toLowerCase().includes(filters.search_query.toLowerCase())
    );
  }

  // sorting by name
  if (sortOrder) {
    switch (sortOrder) {
      case "atoz":
        filteredList = sortBy(filteredList, ["name"]);
        break;
      case "ztoa":
        filteredList = sortBy(filteredList, ["name"]).reverse();
        break;
    }
  }

  return filteredList;
};

export const listOverWriteHelper = (stateList: CompactReport[], actionList: CompactReport[]) => {
  //@ts-ignore
  const newList: CompactReport[] = actionList.map((item: CompactReport) => {
    const findItem = stateList.find((actionItem: CompactReport) => actionItem.key === item.key);
    const _categories = item?.categories.map(category => category?.toLowerCase()?.replace(/ /g, "_")) || [];
    if (findItem) {
      return {
        ...findItem,
        categories: _categories,
        content: item?.content || findItem?.content,
        imageUrl: item?.imageUrl || findItem?.imageUrl,
        description: item?.description || findItem?.description
      };
    }
  });
  return newList.filter(item => !!item);
};

export const removeFEMiscReportPresentInAPIData = (feMiscReports: any, allBEReports: any) => {
  return feMiscReports
    .map((report: any) => {
      const _report = allBEReports.find((_r: any) => _r.id === report.key);
      if (!_report) {
        return report;
      }
      return undefined;
    })
    .filter((v: any) => !!v);
};
